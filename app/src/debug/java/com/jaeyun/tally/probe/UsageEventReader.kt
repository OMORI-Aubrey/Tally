package com.jaeyun.tally.probe

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process

/** `UsageStatsManager` 원본 이벤트를 그대로 읽는다. 필터링·가공은 하지 않는다 */
class UsageEventReader(private val context: Context) {

    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)
    private val packageManager = context.packageManager

    /**
     * 결과 콜백이 없으므로 화면에 돌아올 때마다 다시 확인한다 (§6.4).
     * 기획서 예시의 `unsafeCheckOpNoThrow`는 SDK 36.1에서 deprecated되어 `checkOpNoThrow`를 쓴다(API 19부터 존재).
     */
    fun hasPermission(): Boolean = appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName,
    ) == AppOpsManager.MODE_ALLOWED

    fun read(beginAt: Long, endAt: Long): List<ProbeEvent> {
        val usageEvents = usageStatsManager.queryEvents(beginAt, endAt) ?: return emptyList()
        val event = UsageEvents.Event()
        val result = mutableListOf<ProbeEvent>()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            result += ProbeEvent(event.timeStamp, event.eventType, event.packageName)
        }
        return result
    }

    fun launcherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    fun appLabel(packageName: String): String? = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
