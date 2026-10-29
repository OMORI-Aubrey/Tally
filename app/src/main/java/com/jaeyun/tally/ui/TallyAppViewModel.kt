package com.jaeyun.tally.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.datastore.AppSettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** 앱 루트 상태. 시작 목적지만 정한다 */
class TallyAppViewModel(settingsStore: AppSettingsStore) : ViewModel() {

    /**
     * 온보딩을 마치기 전이면 [Onboarding], 아니면 [Timer] (§8.7). null이면 아직 설정을 읽는 중이다.
     *
     * 앱을 열 때 한 번만 읽는다. 온보딩을 마쳐도 바꾸지 않는다 — 시작 목적지가 바뀌면 NavHost가 그래프를 새로 만들어
     * 백스택이 처음으로 돌아간다. 온보딩을 마친 뒤의 이동은 [Onboarding]을 백스택에서 지우는 것으로 처리한다.
     */
    val startDestination: StateFlow<Any?> = flow {
        emit(if (settingsStore.settings.first().onboardingDone) Timer else Onboarding)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory = viewModelFactory {
            initializer { TallyAppViewModel(appContainer.settingsStore) }
        }
    }
}
