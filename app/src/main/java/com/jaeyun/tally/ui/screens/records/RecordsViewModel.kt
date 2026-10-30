package com.jaeyun.tally.ui.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.repository.SessionRepository
import com.jaeyun.tally.data.repository.WeeklyGoalRepository
import com.jaeyun.tally.data.repository.WeeklyGoals
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.countsInStats
import com.jaeyun.tally.domain.baseline.BaselineCalculator
import com.jaeyun.tally.util.weekRangeOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    /** 끝난 세션이 하나도 없다. 섹션 없이 안내 한 줄만 둔다 (§8.5.1) */
    data object Empty : RecordsUiState

    /** [today]는 세션 목록의 날짜 머리말을 "오늘"·"어제"로 바꾸는 기준 */
    data class Content(val verdict: VerdictSection, val week: WeekSection, val today: LocalDate) : RecordsUiState
}

/**
 * 기록 탭 (§8.4): 판정 섹션(늘 이번 주)과 이번 주 섹션(주 선택). 누적 섹션은 P7에서 붙는다.
 *
 * 세션이 끝나거나 리포트에서 다시 계산되면(재분류) 다시 그린다. 목표는 탭에 들어올 때 갱신한 값을 쓴다([refreshGoals]).
 *
 * @param debugBuild debug 빌드인지. 설정의 기준선 완화 플래그는 debug 빌드에서만 효과가 있다 (§5.3)
 */
class RecordsViewModel(
    private val sessions: SessionRepository,
    private val goals: WeeklyGoalRepository,
    settings: AppSettingsStore,
    private val debugBuild: Boolean,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** 보고 있는 주의 시작. null이면 이번 주 */
    private val selectedWeekStart = MutableStateFlow<Long?>(null)

    private val weeklyGoals = MutableStateFlow<WeeklyGoals?>(null)

    private val preferences = settings.settings
        .map { RecordsPreferences(it.selfReportedDailyMin, it.relaxedBaseline) }
        .distinctUntilChanged()

    val uiState: StateFlow<RecordsUiState> =
        combine(sessions.finishedSessions, selectedWeekStart, weeklyGoals, preferences) { finished, selected, goals, prefs ->
            if (finished.isEmpty()) return@combine RecordsUiState.Empty
            val now = clock()
            val zoneId = zone()
            val week = selectedWeek(finished, selected, now, zoneId)
            val usage = sessions.loadAppUsage(finished.startedIn(week).filter { it.countsInStats }.map { it.id })
            RecordsUiState.Content(
                verdict = buildVerdictSection(
                    sessions = finished,
                    goals = goals,
                    now = now,
                    zone = zoneId,
                    requiredSessions = BaselineCalculator.requiredSessions(prefs.relaxedBaseline, debugBuild),
                ),
                week = buildWeekSection(
                    sessions = finished,
                    week = week,
                    selfReportedDailyMin = prefs.selfReportedDailyMin,
                    topApps = topApps(usage.segments, usage.appLabels),
                    now = now,
                    zone = zoneId,
                ),
                today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState.Loading)

    /**
     * 기록 탭에 들어올 때마다 부른다. 지난 목표의 달성 여부를 채우고 이번 주 목표가 없으면 만든다(PLAN.md §4).
     * 판정 섹션이 결과를 보여 준다
     */
    fun refreshGoals() {
        viewModelScope.launch { weeklyGoals.value = goals.refresh(clock(), zone()) }
    }

    /** 앞 주로. 첫 기록의 주보다 앞으로는 가지 않는다(화면이 화살표를 막고, [selectedWeek]가 한 번 더 맞춘다) */
    fun showPreviousWeek() {
        val zoneId = zone()
        val start = selectedWeekStart.value ?: weekRangeOf(clock(), zoneId).startAt
        selectedWeekStart.value = weekRangeOf(start - 1, zoneId).startAt
    }

    /** 다음 주로. 이번 주에 닿으면 이번 주를 따라가도록(날짜가 바뀌어도) null로 둔다 */
    fun showNextWeek() {
        val zoneId = zone()
        val thisWeekStart = weekRangeOf(clock(), zoneId).startAt
        val start = selectedWeekStart.value ?: return
        val next = weekRangeOf(start, zoneId).endAt
        selectedWeekStart.value = next.takeIf { it < thisWeekStart }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RecordsViewModel(
                    sessions = appContainer.sessionRepository,
                    goals = appContainer.weeklyGoalRepository,
                    settings = appContainer.settingsStore,
                    debugBuild = appContainer.debuggable,
                )
            }
        }
    }
}

/** 기록 탭이 읽는 설정 */
private data class RecordsPreferences(val selfReportedDailyMin: Int?, val relaxedBaseline: Boolean)
