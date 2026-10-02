package com.jaeyun.tally

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.repository.PerceivedFocusRepository
import com.jaeyun.tally.data.repository.SessionRepository
import com.jaeyun.tally.data.repository.SubjectRepository
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.data.usagestats.AppClassifier
import com.jaeyun.tally.data.usagestats.UsageEventSource

/**
 * 수동 DI 컨테이너 (§10.1). 앱 전역 단일 인스턴스이며 [TallyApplication]이 소유한다.
 *
 * Room DB, DataStore, UsageStats 소스, Repository를 이곳에서 `by lazy`로 생성한다.
 * 도메인 로직(§10.3)은 순수 함수이므로 컨테이너에 등록하지 않는다.
 */
class AppContainer(private val appContext: Context) {

    val database: TallyDatabase by lazy { TallyDatabase.create(appContext) }

    val settingsStore: AppSettingsStore by lazy { AppSettingsStore.create(appContext) }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(
            database = database,
            usageEvents = usageEventSource,
            classifier = appClassifier,
            selfPackage = appContext.packageName,
            // debug 빌드는 복원 불변식이 깨지면 바로 멈춘다. BuildConfig 대신 debuggable 플래그로 판단한다
            strictInvariant = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
        )
    }

    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.studySessionDao(), settingsStore) }

    val perceivedFocusRepository: PerceivedFocusRepository by lazy {
        PerceivedFocusRepository(database.studySessionDao(), settingsStore)
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
