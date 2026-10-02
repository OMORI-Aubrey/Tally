package com.jaeyun.tally.data.usagestats

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.net.toUri
import com.jaeyun.tally.domain.model.Category

/** 자동 분류의 근거. 우선순위가 높은 순서다 */
enum class AutoCategoryReason {
    /** 본 앱 → 허용 */
    SELF,

    /** 기본 홈 앱 → 허용. 앱 전환 때 홈을 거치는 짧은 구간을 이탈로 세지 않는다 (§5.1) */
    LAUNCHER,

    /** 공부 앱(화상 강의·인강·사전·노트 등) → 허용. 알려진 패키지 목록으로 판별한다 ([STUDY_APPS]) */
    STUDY_APP,

    /** 웹 브라우저 → 딴짓. 강의 시청과 구분할 수 없다는 한계가 있다 (§5.1, §15) */
    BROWSER,

    /** 전화 앱·통화 화면 → 딴짓 (§5.1) */
    PHONE,

    /** `ApplicationInfo.category`가 SOCIAL·VIDEO·GAME → 딴짓. 유튜브처럼 미리 깔린 시스템 앱도 여기서 걸린다 */
    DISTRACT_CATEGORY,

    /** 시스템 앱(설정·계산기·키보드·권한 창 등)과 정보를 읽을 수 없는 앱 → 허용 */
    SYSTEM,

    /** 그 밖에 사용자가 설치한 앱 → 딴짓 기본값. 사용자가 이탈 목록·앱 분류 화면에서 바꾼다 (#16, #18) */
    INSTALLED,
}

/** 분류에 필요한 앱 정보. [AppClassifier]가 `PackageManager`에서 모은다 */
data class AppFacts(
    val isSelf: Boolean,
    val isDefaultLauncher: Boolean,
    val isStudyApp: Boolean,
    val isBrowser: Boolean,
    val isPhone: Boolean,
    /** `ApplicationInfo.category`. 모르면 `CATEGORY_UNDEFINED` */
    val appCategory: Int,
    /** 옛 게임 표시(`FLAG_IS_GAME`) */
    val legacyGameFlag: Boolean,
    /**
     * 시스템 앱이거나 정보를 읽을 수 없는 앱. 런처 아이콘이 있는 앱은 매니페스트 `<queries>`로 모두 보이므로,
     * 보이지 않는 앱은 권한 창·공유 창 같은 시스템 부품이다
     */
    val isSystemApp: Boolean,
)

/**
 * 자동 분류 규칙 (PLAN.md §4 "기본 딴짓"). 본 앱·기본 홈·공부 앱·시스템 앱만 허용하고, 사용자가 설치한 앱은 딴짓으로
 * 시작한다. 브라우저·전화·SNS·영상·게임은 시스템 앱이어도 딴짓이라 시스템 판정보다 먼저 본다.
 * 사용자가 고친 분류(`ClassificationSource.USER`)는 덮어쓰지 않는다(#12).
 */
fun autoCategory(facts: AppFacts): Pair<Category, AutoCategoryReason> = when {
    facts.isSelf -> Category.ALLOWED to AutoCategoryReason.SELF
    facts.isDefaultLauncher -> Category.ALLOWED to AutoCategoryReason.LAUNCHER
    facts.isStudyApp -> Category.ALLOWED to AutoCategoryReason.STUDY_APP
    facts.isBrowser -> Category.DISTRACT to AutoCategoryReason.BROWSER
    facts.isPhone -> Category.DISTRACT to AutoCategoryReason.PHONE
    facts.legacyGameFlag || facts.appCategory in DISTRACT_APP_CATEGORIES ->
        Category.DISTRACT to AutoCategoryReason.DISTRACT_CATEGORY
    facts.isSystemApp -> Category.ALLOWED to AutoCategoryReason.SYSTEM
    else -> Category.DISTRACT to AutoCategoryReason.INSTALLED
}

private val DISTRACT_APP_CATEGORIES = setOf(
    ApplicationInfo.CATEGORY_SOCIAL,
    ApplicationInfo.CATEGORY_VIDEO,
    ApplicationInfo.CATEGORY_GAME,
)

data class AutoClassification(
    val packageName: String,
    /** 앱을 찾지 못하면 패키지 이름 */
    val appLabel: String,
    val category: Category,
    val reason: AutoCategoryReason,
)

/**
 * 세션 중 감지된 앱을 자동 분류하고 앱 이름을 찾는다 (§5.1). Android 11+ 패키지 가시성 때문에 매니페스트 `<queries>`에
 * 런처 앱·HOME·브라우저 인텐트를 선언해 두었다. 보이지 않는 앱은 시스템 부품으로 보고 허용한다.
 *
 * `PackageManager` 조회라 IO 디스패처에서 부른다.
 */
class AppClassifier(private val context: Context) {

    private val packageManager = context.packageManager

    /** 홈·브라우저·전화 목록은 한 번만 조회한다 */
    fun classifyAll(packageNames: Collection<String>): List<AutoClassification> {
        val launcher = defaultLauncher()
        val browsers = browsers()
        val phones = phoneApps()
        return packageNames.distinct().map { packageName ->
            val info = applicationInfo(packageName)
            val facts = AppFacts(
                isSelf = packageName == context.packageName,
                isDefaultLauncher = packageName == launcher,
                isStudyApp = packageName in STUDY_APPS,
                isBrowser = packageName in browsers,
                isPhone = packageName in phones || isInCallScreen(packageName),
                appCategory = info?.category ?: ApplicationInfo.CATEGORY_UNDEFINED,
                legacyGameFlag = info?.let(::hasLegacyGameFlag) ?: false,
                isSystemApp = info == null || (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            )
            val (category, reason) = autoCategory(facts)
            AutoClassification(
                packageName = packageName,
                appLabel = info?.let { packageManager.getApplicationLabel(it).toString() } ?: packageName,
                category = category,
                reason = reason,
            )
        }
    }

    /**
     * 사용자가 고른 기본 홈 앱. 홈 인텐트에 응답하는 앱 전부가 아니다 — 설정 앱(FallbackHome) 등도 응답하기 때문이다(#1 실측).
     * 기본값이 정해지지 않았으면 선택 화면(`android`)이 돌아오므로 null.
     */
    private fun defaultLauncher(): String? {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = resolveActivity(home)?.activityInfo?.packageName
        return resolved?.takeUnless { it == "android" }
    }

    /** 아무 사이트나 열 수 있는 앱. 특정 도메인만 여는 앱은 빠진다 */
    private fun browsers(): Set<String> {
        val view = Intent(Intent.ACTION_VIEW, "https://example.com".toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        return queryActivities(view).mapTo(mutableSetOf()) { it.activityInfo.packageName }
    }

    /**
     * 기본 전화 앱과 시스템 전화 앱. `ACTION_DIAL`에 응답하는 앱 전부가 아니다 — 줌처럼 전화를 걸 수 있는 앱도
     * 응답하는데(#11 실기기 확인), 줌은 온라인 강의에 쓰이므로 딴짓으로 분류하면 안 된다.
     */
    private fun phoneApps(): Set<String> {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return emptySet()
        return setOfNotNull(telecom.defaultDialerPackage, telecom.systemDialerPackage)
    }

    @Suppress("DEPRECATION")
    private fun hasLegacyGameFlag(info: ApplicationInfo): Boolean = (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    /** 통화 중 화면은 전화 앱과 패키지가 다르다(예: `com.samsung.android.incallui`, `com.android.incallui`) */
    private fun isInCallScreen(packageName: String): Boolean = packageName.endsWith(".incallui")

    private fun applicationInfo(packageName: String): ApplicationInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun resolveActivity(intent: Intent): ResolveInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
}
