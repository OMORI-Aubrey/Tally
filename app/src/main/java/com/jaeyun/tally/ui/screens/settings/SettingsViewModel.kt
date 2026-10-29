package com.jaeyun.tally.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.repository.AppDataRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZoneId

data class SettingsUiState(
    /** 사용 기록 접근 권한이 켜져 있는지 */
    val permissionGranted: Boolean,
    /** 기준선 활성 조건 완화(12 → 6세션). 개발자 영역 */
    val relaxedBaseline: Boolean = false,
    /** 세션이 하나라도 있는지. 있으면 데모 데이터를 넣지 않는다. 읽기 전에는 있다고 본다 */
    val hasSessions: Boolean = true,
    /** 이 화면에서 데모 데이터를 넣었다 */
    val demoInserted: Boolean = false,
)

/** 화면이 한 번만 처리할 일 */
sealed interface SettingsEvent {
    /** 내보낸 CSV를 시스템 공유로 넘긴다 */
    data class ShareCsv(val file: File) : SettingsEvent

    /** 데이터를 모두 지웠다. 온보딩으로 간다 */
    data object DataDeleted : SettingsEvent
}

/**
 * 설정 탭 (와이어프레임 §6). 권한은 결과 콜백이 없어서 화면이 보일 때마다([refreshPermission]) 다시 확인한다(§6.4).
 * 개발자 영역(CSV 내보내기, 데모 데이터, 기준선 완화)은 화면이 debug 빌드에서만 보여 준다.
 *
 * @param hasPermission 사용 기록 접근 권한이 켜져 있는지
 */
class SettingsViewModel(
    private val settingsStore: AppSettingsStore,
    private val appData: AppDataRepository,
    private val hasPermission: () -> Boolean,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val permission = MutableStateFlow(hasPermission())
    private val demoInserted = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
        combine(permission, settingsStore.settings, appData.hasSessions, demoInserted) { granted, settings, hasSessions, inserted ->
            SettingsUiState(granted, settings.relaxedBaseline, hasSessions, inserted)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(permission.value))

    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val event: Flow<SettingsEvent> = events.receiveAsFlow()

    /** 삭제·내보내기·데모 주입 중이다. 연타해도 한 번만 한다 */
    private var working = false

    fun refreshPermission() {
        permission.value = hasPermission()
    }

    fun toggleRelaxedBaseline() {
        viewModelScope.launch { settingsStore.setRelaxedBaseline(!settingsStore.settings.first().relaxedBaseline) }
    }

    /** 확인 다이얼로그에서 `[모두 지우기]`를 고른 뒤에 부른다 */
    fun deleteAll() = runOnce {
        appData.deleteAll()
        events.send(SettingsEvent.DataDeleted)
    }

    fun exportCsv() = runOnce {
        events.send(SettingsEvent.ShareCsv(appData.exportSessionsCsv(clock(), zone())))
    }

    fun insertDemoData() = runOnce {
        if (appData.insertDemoData(clock(), zone())) demoInserted.value = true
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
                SettingsViewModel(container.settingsStore, container.appDataRepository, container.usageEventSource::hasPermission)
            }
        }
    }
}
