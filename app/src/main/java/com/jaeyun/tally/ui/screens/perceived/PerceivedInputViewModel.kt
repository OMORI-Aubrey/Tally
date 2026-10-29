package com.jaeyun.tally.ui.screens.perceived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.repository.PerceivedFocusRepository
import com.jaeyun.tally.data.room.StudySession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 빈칸에 적은 글자를 체감 집중 시간으로 읽은 결과 */
sealed interface PerceivedMinutes {
    data object Empty : PerceivedMinutes

    data class Valid(val minutes: Int) : PerceivedMinutes

    /** 세션 길이보다 길다 */
    data object TooLong : PerceivedMinutes

    /** 시간 칸이 있는데 분 칸에 60 이상을 적었다 */
    data object MinutesOver59 : PerceivedMinutes
}

/**
 * 1시간 이상 세션이면 시간 칸(한 자리)과 분 칸(두 자리, 0~59)을 따로 둔다. "2시간 반"을 150으로 바꿔 적지 않게 한다.
 * 1시간 미만 세션은 분 칸만 둔다.
 */
internal fun hasHourBlank(maxMinutes: Int): Boolean = maxMinutes >= 60

/**
 * 시간 칸에 적은 [hoursText]가 세션 길이 안이면 분 칸으로 넘어간다. 2시간 1분 세션에 3이나 9를 적으면 시간 칸에 남아
 * 바로 고칠 수 있게 한다(세션 길이 안내는 함께 뜬다).
 */
internal fun hourFitsSession(hoursText: String, maxMinutes: Int): Boolean {
    val hours = hoursText.toIntOrNull() ?: return false
    return hours <= maxMinutes / 60
}

/**
 * 빈칸 [hoursText]·[minutesText](숫자만)를 읽는다. 시간 칸이 없으면 [hoursText]는 비어 있다.
 * 한 칸만 적으면 다른 칸은 0으로 본다(`2시간`만 적으면 120분). 0분도 받는다 — "거의 집중하지 못했다"도 정직한 답이다.
 * 타이머가 보여준 세션 길이([maxMinutes])보다 길게는 적을 수 없다.
 */
internal fun parsePerceivedMinutes(hoursText: String, minutesText: String, maxMinutes: Int): PerceivedMinutes {
    if (hoursText.isEmpty() && minutesText.isEmpty()) return PerceivedMinutes.Empty
    val hours = hoursText.toIntOrNull() ?: 0
    val minutes = minutesText.toIntOrNull() ?: 0
    if (hasHourBlank(maxMinutes) && minutes >= 60) return PerceivedMinutes.MinutesOver59
    val total = hours * 60 + minutes
    return if (total > maxMinutes) PerceivedMinutes.TooLong else PerceivedMinutes.Valid(total)
}

/**
 * 체감으로 적을 수 있는 최대 분. 착석(`tTotalSec`)이 아니라 타이머가 보여준 시작~종료 길이다 — 자리 비움(AWAY)으로
 * 빠진 시간에도 폰 밖에서 공부했을 수 있다. 남는 초는 올린다(47분 30초 세션에 48분을 적어도 받는다).
 */
internal fun maxPerceivedMinutes(session: StudySession): Int {
    val millis = (session.endAt ?: session.startAt) - session.startAt
    return ((millis + 59_999) / 60_000).toInt()
}

sealed interface PerceivedInputUiState {
    data object Loading : PerceivedInputUiState

    data class Ready(val maxMinutes: Int) : PerceivedInputUiState

    /** 세션이 없다(있을 수 없지만). 묻지 않고 넘어간다 */
    data object Missing : PerceivedInputUiState
}

/**
 * 체감 입력 (§3.4, 와이어프레임 §3). 세션 종료 직후, 리포트를 보여주기 전에 묻는다.
 * 물을지는 타이머 화면이 정했고 물었다는 기록도 남겼다. 여기서는 답을 저장하거나 건너뛸 뿐이다.
 */
class PerceivedInputViewModel(
    private val sessionId: Long,
    private val repository: PerceivedFocusRepository,
) : ViewModel() {

    val uiState: StateFlow<PerceivedInputUiState> = flow {
        val session = repository.session(sessionId)
        emit(if (session == null) PerceivedInputUiState.Missing else PerceivedInputUiState.Ready(maxPerceivedMinutes(session)))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerceivedInputUiState.Loading)

    private val finished = Channel<Unit>(Channel.CONFLATED)

    /** 답을 저장했거나 건너뛰었다. 화면이 받아서 리포트로 간다 */
    val done: Flow<Unit> = finished.receiveAsFlow()

    /** 확인을 연타하거나 확인 뒤 뒤로가기를 눌러도 한 번만 끝낸다 */
    private var finishing = false

    /** 저장이 끝난 뒤에 알린다. 먼저 화면을 떠나면 ViewModel과 함께 저장이 취소될 수 있다 */
    fun submit(minutes: Int) {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            repository.save(sessionId, minutes)
            finished.send(Unit)
        }
    }

    /** 건너뛴다. 물었다는 기록은 이미 남았으니 그 주에는 다시 묻지 않는다 */
    fun skip() {
        if (finishing) return
        finishing = true
        finished.trySend(Unit)
    }

    companion object {
        fun factory(sessionId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { PerceivedInputViewModel(sessionId, appContainer.perceivedFocusRepository) }
        }
    }
}
