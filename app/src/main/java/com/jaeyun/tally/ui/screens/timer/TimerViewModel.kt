package com.jaeyun.tally.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.repository.SessionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TimerUiState {
    /** 진행 중 세션을 아직 확인하지 못했다. IDLE을 잠깐 보였다가 RUNNING으로 바뀌는 깜빡임을 막는다 */
    data object Loading : TimerUiState

    data object Idle : TimerUiState

    /** [subjectName]이 null이면 과목 없이 시작한 세션이다 */
    data class Running(val sessionId: Long, val startAt: Long, val subjectName: String?) : TimerUiState
}

/**
 * 타이머 탭 (§8.1, §8.5). 상태는 Room의 진행 중 세션 하나로 정해지므로, 앱을 강제 종료하거나 재부팅해도
 * 다시 열면 RUNNING이 이어진다(§6.3 #5). 경과 시간은 저장하지 않고 화면이 `now − startAt`으로 그린다.
 */
class TimerViewModel(
    private val sessions: SessionRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<TimerUiState> = sessions.runningSession
        .map { session ->
            if (session == null) TimerUiState.Idle
            else TimerUiState.Running(session.id, session.startAt, session.subjectName)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState.Loading)

    private val finished = Channel<Long>(Channel.BUFFERED)

    /** 방금 종료한 세션 id. 화면이 받아서 리포트로 이동한다 */
    val finishedSessions: Flow<Long> = finished.receiveAsFlow()

    // TODO(#7) 선택한 과목을 넘긴다
    fun start() {
        viewModelScope.launch { sessions.start(subjectName = null, now = clock()) }
    }

    fun finish() {
        val running = uiState.value as? TimerUiState.Running ?: return
        viewModelScope.launch {
            sessions.finish(running.sessionId, now = clock())?.let { finished.send(it.id) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TimerViewModel(appContainer.sessionRepository) }
        }
    }
}
