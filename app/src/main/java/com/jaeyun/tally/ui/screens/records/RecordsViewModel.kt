package com.jaeyun.tally.ui.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.repository.SessionRepository
import com.jaeyun.tally.data.room.StudySession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** 세션 목록의 한 줄 */
data class SessionRow(
    val id: Long,
    val startTime: LocalTime,
    /** null이면 과목 없이 시작한 세션. 자리를 생략한다 */
    val subjectName: String?,
    val sessionSec: Int,
    /** 3분 미만·180분 초과는 false. 목록에는 남기되 흐리게 그린다 (§6.3 #3·#4) */
    val validForStats: Boolean,
)

/** 같은 날(로컬 시간대, 시작 시각 기준) 세션 묶음 */
data class SessionDay(val date: LocalDate, val sessions: List<SessionRow>)

/**
 * 시작 시각의 로컬 날짜로 묶는다. 자정을 넘긴 세션은 시작한 날에 들어간다.
 * [sessions]가 최근 순이면 날짜도 최근 순이고 같은 날 안에서도 최근 순이다.
 */
internal fun groupByDay(sessions: List<StudySession>, zone: ZoneId): List<SessionDay> =
    sessions
        .groupBy { Instant.ofEpochMilli(it.startAt).atZone(zone).toLocalDate() }
        .map { (date, daySessions) ->
            SessionDay(
                date = date,
                sessions = daySessions.map { session ->
                    SessionRow(
                        id = session.id,
                        startTime = Instant.ofEpochMilli(session.startAt).atZone(zone).toLocalTime(),
                        subjectName = session.subjectName,
                        sessionSec = session.tTotalSec,
                        validForStats = session.isValidForStats,
                    )
                },
            )
        }

sealed interface RecordsUiState {
    data object Loading : RecordsUiState

    /** 끝난 세션이 하나도 없다 (§8.5.1) */
    data object Empty : RecordsUiState

    /** [today]는 날짜 머리말을 "오늘"·"어제"로 바꾸는 기준 */
    data class Content(val days: List<SessionDay>, val today: LocalDate) : RecordsUiState
}

/** 기록 탭 (§8.4). P1에서는 세션 목록만 둔다. 판정·이번 주·누적 섹션은 P6·P7에서 위에 붙는다 */
class RecordsViewModel(
    sessions: SessionRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<RecordsUiState> = sessions.finishedSessions
        .map { finished ->
            if (finished.isEmpty()) {
                RecordsUiState.Empty
            } else {
                val zoneId = zone()
                RecordsUiState.Content(
                    days = groupByDay(finished, zoneId),
                    today = Instant.ofEpochMilli(clock()).atZone(zoneId).toLocalDate(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState.Loading)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RecordsViewModel(appContainer.sessionRepository) }
        }
    }
}
