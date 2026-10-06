package com.jaeyun.tally

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.repository.AppClassificationRepository
import com.jaeyun.tally.data.repository.PerceivedFocusRepository
import com.jaeyun.tally.data.repository.SessionRepository
import com.jaeyun.tally.data.repository.SubjectRepository
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.data.usagestats.AppClassifier
import com.jaeyun.tally.data.usagestats.UsageEventSource
import com.jaeyun.tally.domain.reconstructor.LONG_SCREEN_OFF_MILLIS
import kotlinx.coroutines.flow.first

/** 임시(#21): 프로브에서 낮춘 ① 화면 꺼짐 확인 기준 */
private const val QUICK_SCREEN_OFF_MILLIS = 60_000L

/**
 * 수동 DI 컨테이너 (§10.1). 앱 전역 단일 인스턴스이며 [TallyApplication]이 소유한다.
 *
 * Room DB, DataStore, UsageStats 소스, Repository를 이곳에서 `by lazy`로 생성한다.
 * 도메인 로직(§10.3)은 순수 함수이므로 컨테이너에 등록하지 않는다.
 */
class AppContainer(private val appContext: Context) {

    val database: TallyDatabase by lazy { TallyDatabase.create(appContext) }

    val settingsStore: AppSettingsStore by lazy { AppSettingsStore.create(appContext) }

    /** debug 빌드인지. BuildConfig 대신 debuggable 플래그로 판단한다 */
    private val debuggable: Boolean
        get() = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(
            database = database,
            usageEvents = usageEventSource,
            classifier = appClassifier,
            settings = settingsStore,
            selfPackage = appContext.packageName,
            // debug 빌드는 복원 불변식이 깨지면 바로 멈춘다
            strictInvariant = debuggable,
            // 임시(#21): debug 빌드는 프로브에서 ① 화면 꺼짐 확인 기준을 1분으로 낮춰 시험할 수 있다
            longScreenOffMillis = {
                if (debuggable && settingsStore.settings.first().quickScreenOffCheck) QUICK_SCREEN_OFF_MILLIS else LONG_SCREEN_OFF_MILLIS
            },
        )
    }

    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.studySessionDao(), settingsStore) }

    val perceivedFocusRepository: PerceivedFocusRepository by lazy {
        PerceivedFocusRepository(database.studySessionDao(), settingsStore)
    }

    val appClassificationRepository: AppClassificationRepository by lazy {
        AppClassificationRepository(database.appClassificationDao())
    }

    val usageEventSource: UsageEventSource by lazy { UsageEventSource(appContext) }

    val appClassifier: AppClassifier by lazy { AppClassifier(appContext) }
}

/**
 * ViewModel 팩토리에서 컨테이너를 꺼낸다.
 *
 * ```
 * val Factory = viewModelFactory {
 *     initializer { SomeViewModel(appContainer.someRepository) }
 * }
 * ```
 */
val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) as TallyApplication).container
