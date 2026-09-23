package com.jaeyun.tally

import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * 수동 DI 컨테이너 (§10.1). 앱 전역 단일 인스턴스이며 [TallyApplication]이 소유한다.
 *
 * Room DB, DataStore, UsageStats 소스, Repository를 이곳에서 `by lazy`로 생성한다.
 * 도메인 로직(§10.3)은 순수 함수이므로 컨테이너에 등록하지 않는다.
 */
class AppContainer(private val appContext: Context)

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
