package com.jaeyun.tally.ui.screens.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.datastore.AppSettingsStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 자기보고 슬라이더 범위. 1시간 단위 (PLAN.md §4) */
internal val SelfReportHours = 1..12

/** 화면 아래 권한 자리에 보여줄 것 */
enum class PermissionStep {
    /** 꺼져 있다. 설정에서 할 일을 순서대로 보여주고 [권한 설정하기]·[나중에 하기]를 둔다 */
    GUIDE,

    /** [권한 설정하기]로 설정에 다녀왔는데 아직 꺼져 있다. 안내 첫 줄만 바뀐다 */
    STILL_OFF,

    /** 처음부터 켜져 있다(또는 다른 길로 켜고 왔다). [시작하기]만 둔다 */
    GRANTED,
}

/**
 * 화면에 돌아와 권한을 다시 확인한 결과. [requested]는 이 온보딩에서 [권한 설정하기]를 누른 적이 있는지다.
 * 누르고 다녀와서 켜져 있으면 [PermissionStep]이 아니라 온보딩을 끝낸다([finishesOnReturn]).
 */
internal fun permissionStep(granted: Boolean, requested: Boolean): PermissionStep = when {
    granted -> PermissionStep.GRANTED
    requested -> PermissionStep.STILL_OFF
    else -> PermissionStep.GUIDE
}

/** [권한 설정하기]로 설정에 다녀와 권한을 켰으면 바로 타이머로 간다 (PLAN.md §4) */
internal fun finishesOnReturn(granted: Boolean, requested: Boolean): Boolean = granted && requested

/** 고른 시간을 저장할 분으로. 고르지 않았으면(건너뜀) null */
internal fun selfReportedDailyMin(hours: Int?): Int? = hours?.let { it * 60 }

data class OnboardingUiState(
    /** 자기보고 "하루 평균 몇 시간". 고르지 않았으면 null */
    val selfReportedHours: Int?,
    val permissionStep: PermissionStep,
)

/**
 * 온보딩 1화면 (§8.7). 고른 시간과 [권한 설정하기]를 누른 적이 있는지는 [SavedStateHandle]에 둔다 —
 * 시스템 설정에 가 있는 동안 앱 프로세스가 정리돼도 돌아와서 이어진다.
 *
 * 권한은 결과 콜백이 없어서 화면이 보일 때마다([refreshPermission]) 다시 확인한다(§6.4).
 *
 * @param hasPermission 사용 기록 접근 권한이 켜져 있는지
 */
class OnboardingViewModel(
    private val settingsStore: AppSettingsStore,
    private val hasPermission: () -> Boolean,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val requested: Boolean
        get() = savedState[KEY_REQUESTED] ?: false

    private val permission = MutableStateFlow(permissionStep(hasPermission(), requested))

    val uiState: StateFlow<OnboardingUiState> =
        combine(savedState.getStateFlow<Int?>(KEY_HOURS, null), permission) { hours, step -> OnboardingUiState(hours, step) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                OnboardingUiState(savedState[KEY_HOURS], permission.value),
            )

    private val finished = Channel<Unit>(Channel.CONFLATED)

    /** 저장을 마쳤다. 화면이 받아서 타이머로 간다 */
    val done: Flow<Unit> = finished.receiveAsFlow()

    private var finishing = false

    fun setSelfReportedHours(hours: Int) {
        savedState[KEY_HOURS] = hours.coerceIn(SelfReportHours)
    }

    /** 시스템 설정을 열기 직전에 부른다. 안내 첫 줄은 돌아온 뒤에 바뀐다 */
    fun onPermissionRequested() {
        savedState[KEY_REQUESTED] = true
    }

    /** 화면이 보일 때마다 부른다. 설정에 다녀와 켰으면 안내를 바꾸지 않고 바로 끝낸다 */
    fun refreshPermission() {
        val granted = hasPermission()
        if (finishesOnReturn(granted, requested)) {
            finish()
        } else if (!finishing) {
            permission.value = permissionStep(granted, requested)
        }
    }

    /**
     * [나중에 하기]·[시작하기], 또는 권한을 켜고 돌아왔다. 자기보고 값과 온보딩 완료를 저장한 뒤 알린다.
     * 먼저 화면을 떠나면 ViewModel과 함께 저장이 취소될 수 있다. 연타해도 한 번만 끝낸다.
     */
    fun finish() {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            settingsStore.setSelfReportedDailyMin(selfReportedDailyMin(savedState[KEY_HOURS]))
            settingsStore.setOnboardingDone(true)
            finished.send(Unit)
        }
    }

    companion object {
        private const val KEY_HOURS = "selfReportedHours"
        private const val KEY_REQUESTED = "permissionRequested"

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                OnboardingViewModel(container.settingsStore, container.usageEventSource::hasPermission, createSavedStateHandle())
            }
        }
    }
}
