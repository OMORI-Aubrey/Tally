package com.jaeyun.tally.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.repository.PerceivedFocusRepository
import com.jaeyun.tally.data.repository.SessionRepository
import com.jaeyun.tally.data.repository.SubjectRepository
import com.jaeyun.tally.data.repository.SubjectSummary
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.domain.model.SubjectName
import com.jaeyun.tally.ui.components.WeeklySummary
import com.jaeyun.tally.util.WeekRange
import com.jaeyun.tally.util.weekRangeOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.random.Random

sealed interface TimerUiState {
    /** 진행 중 세션을 아직 확인하지 못했다. IDLE을 잠깐 보였다가 RUNNING으로 바뀌는 깜빡임을 막는다 */
    data object Loading : TimerUiState

    /**
     * @param chips 칩에 보일 과목. 선택한 과목이 항상 들어 있다
     * @param selected 시작하면 이 과목으로 저장한다. null이면 과목 없이 시작한다
     * @param sheetSubjects 과목 시트 목록(숨긴 과목 포함)
     * @param weeklySummary 주간 요약 줄. null이면 이번 주에 셀 세션이 없다
     */
    data class Idle(
        val chips: List<String>,
        val selected: String?,
        val sheetSubjects: List<SubjectSummary>,
        val weeklySummary: WeeklySummary?,
    ) : TimerUiState

    /** [subjectName]이 null이면 과목 없이 시작한 세션이다 */
    data class Running(val sessionId: Long, val startAt: Long, val subjectName: String?) : TimerUiState
}

/** 칩은 최근에 쓴 과목 5개까지 (§9) */
internal const val MAX_SUBJECT_CHIPS = 5

/**
 * 칩에 보일 과목. 최근 순서([recent], 숨긴 과목 제외)에서 [max]개까지 고르되, [selected]가 그 안에 없으면
 * (시트에서 오래된 과목이나 숨긴 과목을 골랐거나, 방금 새로 입력한 과목) 맨 앞에 넣는다.
 */
internal fun subjectChips(recent: List<String>, selected: String?, max: Int = MAX_SUBJECT_CHIPS): List<String> {
    val top = recent.take(max)
    return if (selected == null || selected in top) top else listOf(selected) + recent.take(max - 1)
}

/**
 * 주 단위로 셀 세션: 통계에 들고 타임라인이 있다. 3분 미만 세션은 오조작이고(§6.3 #3), 사용 기록 권한 없이 끝낸 세션은
 * 실측 지표가 없다 (PLAN.md §4 주간 요약 줄의 숫자, 체감 질문 트리거)
 */
internal val StudySession.countsForWeek: Boolean get() = isValidForStats && hasTimeline

/**
 * 주간 요약 줄의 숫자 (§8.1). [week]에 시작한 세션 중 [countsForWeek] 세션의 최장 구간 최댓값과 평균.
 *
 * 셀 세션이 없으면 null이다. 화면은 숫자 대신 문장을 쓴다 — 최장 구간이 0초뿐인 주도 "0초"를 보이지 않게 null로 본다(§8.5.1).
 */
internal fun weeklySummary(sessions: List<StudySession>, week: WeekRange): WeeklySummary? {
    val lfs = sessions
        .filter { it.startAt in week && it.countsForWeek }
        .map { it.lfsSec }
    val longest = lfs.maxOrNull()?.takeIf { it > 0 } ?: return null
    return WeeklySummary(longestSec = longest, averageSec = (lfs.sumOf { it.toLong() } / lfs.size).toInt())
}

/**
 * 방금 끝낸 [session]에서 체감 집중 시간을 물을지 (§3.4). 주 1회만, 예측할 수 없게 묻는다.
 * - [countsForWeek] 세션만 후보다. 실측과 견줄 수 없는 세션에 그 주의 한 번을 쓰지 않는다
 * - 이번 주에 이미 물었으면 묻지 않는다. 답했든 건너뛰었든 같다 (PLAN.md §4)
 * - 1/3 확률로 묻는다. 이번 주 3번째 후보 세션까지 한 번도 걸리지 않았으면 확정으로 묻는다
 *
 * @param weekSessions [session]이 시작한 주의 끝난 세션. [session]을 포함한다
 * @param lastAskedAt 마지막으로 물은 세션의 시작 시각
 */
internal fun shouldAskPerceived(
    session: StudySession,
    weekSessions: List<StudySession>,
    lastAskedAt: Long?,
    week: WeekRange,
    random: Random,
): Boolean {
    if (!session.countsForWeek) return false
    val askedThisWeek = (lastAskedAt != null && lastAskedAt in week) || weekSessions.any { it.perceivedFocusMin != null }
    if (askedThisWeek) return false
    val weekSessionIndex = weekSessions.count { it.countsForWeek && it.startAt <= session.startAt }
    return random.nextInt(3) == 0 || weekSessionIndex >= 3
}

/** 방금 끝낸 세션. [askPerceived]면 리포트 전에 체감 입력을 거친다 */
data class FinishedSession(val sessionId: Long, val askPerceived: Boolean)

/**
 * 타이머 탭 (§8.1, §8.5). 상태는 Room의 진행 중 세션 하나로 정해지므로, 앱을 강제 종료하거나 재부팅해도
 * 다시 열면 RUNNING이 이어진다(§6.3 #5). 경과 시간은 저장하지 않고 화면이 `now − startAt`으로 그린다.
 *
 * 과목은 선택 사항이다(§8.1.1). 처음에는 마지막 세션의 과목을 골라 둔다. 시트에서 추가한 과목은 아직 세션에
 * 쓰지 않았어도 칩에 남는다.
 *
 * 주간 요약 줄의 "이번 주"는 상태를 새로 만들 때의 현재 시각으로 정한다. 화면을 떠났다 돌아오면 다시 정해진다.
 *
 * 세션을 끝내면 리포트 전에 체감 입력을 거칠지 정한다([shouldAskPerceived]).
 */
class TimerViewModel(
    private val sessions: SessionRepository,
    private val subjects: SubjectRepository,
    private val perceived: PerceivedFocusRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val selected = MutableStateFlow<String?>(null)

    /** 사용자가 과목을 직접 고르면 마지막 과목 자동 선택이 늦게 도착해도 덮어쓰지 않는다 */
    private var userPicked = false

    val uiState: StateFlow<TimerUiState> = combine(
        sessions.runningSession,
        subjects.recentVisibleSubjects,
        subjects.allSubjects,
        selected,
        sessions.finishedSessions,
    ) { running, recent, all, picked, finished ->
        if (running != null) {
            TimerUiState.Running(running.id, running.startAt, running.subjectName)
        } else {
            TimerUiState.Idle(
                chips = subjectChips(recent, picked),
                selected = picked,
                sheetSubjects = all,
                weeklySummary = weeklySummary(finished, weekRangeOf(clock(), zone())),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState.Loading)

    private val finished = Channel<FinishedSession>(Channel.BUFFERED)

    /** 방금 종료한 세션. 화면이 받아서 체감 입력이나 리포트로 이동한다 */
    val finishedSessions: Flow<FinishedSession> = finished.receiveAsFlow()

    init {
        viewModelScope.launch {
            val last = subjects.lastUsedVisibleSubject()
            if (!userPicked) selected.value = last
        }
    }

    fun start() {
        viewModelScope.launch { sessions.start(subjectName = selected.value, now = clock()) }
    }

    fun finish() {
        val running = uiState.value as? TimerUiState.Running ?: return
        viewModelScope.launch {
            val done = sessions.finish(running.sessionId, now = clock()) ?: return@launch
            finished.send(FinishedSession(done.id, askPerceived = askPerceived(done)))
        }
    }

    /** 물기로 했으면 화면을 띄우기 전에 물었다고 적어 둔다 */
    private suspend fun askPerceived(done: StudySession): Boolean {
        val week = weekRangeOf(done.startAt, zone())
        val ask = shouldAskPerceived(
            session = done,
            weekSessions = perceived.finishedStartedBetween(week.startAt, week.endAt),
            lastAskedAt = perceived.lastAskedAt(),
            week = week,
            random = random,
        )
        if (ask) perceived.markAsked(done.startAt)
        return ask
    }

    /** 칩·시트에서 고른다. 이미 고른 과목을 다시 누르면 선택을 푼다(과목 없이 시작) */
    fun toggleSubject(name: String) {
        userPicked = true
        selected.value = if (selected.value == name) null else name
    }

    /** 시트에서 고른다. 다시 눌러도 선택을 풀지 않는다 */
    fun selectSubject(name: String) {
        userPicked = true
        selected.value = name
    }

    /** 새 과목을 추가하고 고른다. 이미 있는 이름이면 그 과목을 앞으로 가져와 고른다. 비었으면 무시하고 false */
    fun addSubject(raw: String): Boolean {
        val name = SubjectName.normalize(raw) ?: return false
        selectSubject(name)
        viewModelScope.launch { subjects.add(name, now = clock()) }
        return true
    }

    /** 과거 세션까지 소급해 이름을 바꾼다. 비었거나 같은 이름이면 무시하고 false */
    fun renameSubject(oldName: String, raw: String): Boolean {
        val newName = SubjectName.normalize(raw)
        if (newName == null || newName == oldName) return false
        if (selected.value == oldName) selected.value = newName
        viewModelScope.launch { subjects.rename(oldName, newName) }
        return true
    }

    /** 칩에서 숨긴다(시트에는 남는다). 고른 과목이었으면 선택을 푼다 */
    fun setSubjectHidden(name: String, hidden: Boolean) {
        if (hidden && selected.value == name) selected.value = null
        viewModelScope.launch { subjects.setHidden(name, hidden) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TimerViewModel(appContainer.sessionRepository, appContainer.subjectRepository, appContainer.perceivedFocusRepository)
            }
        }
    }
}
