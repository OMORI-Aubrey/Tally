package com.jaeyun.tally.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.repository.AppDataRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    /** 사용 기록 접근 권한이 켜져 있는지 */
    val permissionGranted: Boolean,
)

/** 화면이 한 번만 처리할 일 */
sealed interface SettingsEvent {
    /** 데이터를 모두 지웠다. 온보딩으로 간다 */
    data object DataDeleted : SettingsEvent
}

/**
 * 설정 탭 (와이어프레임 §6). 권한은 결과 콜백이 없어서 화면이 보일 때마다([refreshPermission]) 다시 확인한다(§6.4).
 *
 * @param hasPermission 사용 기록 접근 권한이 켜져 있는지
 */
class SettingsViewModel(
    private val appData: AppDataRepository,
    private val hasPermission: () -> Boolean,
) : ViewModel() {

    private val permission = MutableStateFlow(hasPermission())

    val uiState: StateFlow<SettingsUiState> =
        permission.map { granted -> SettingsUiState(granted) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(permission.value))

    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val event: Flow<SettingsEvent> = events.receiveAsFlow()

    /** 삭제 중이다. 연타해도 한 번만 한다 */
    private var working = false

    fun refreshPermission() {
        permission.value = hasPermission()
    }

    /** 확인 다이얼로그에서 `[모두 지우기]`를 고른 뒤에 부른다 */
    fun deleteAll() = runOnce {
        appData.deleteAll()
        events.send(SettingsEvent.DataDeleted)
    }

    private fun runOnce(block: suspend () -> Unit) {
        if (working) return
        working = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                working = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                SettingsViewModel(container.appDataRepository, container.usageEventSource::hasPermission)
            }
        }
    }
}
