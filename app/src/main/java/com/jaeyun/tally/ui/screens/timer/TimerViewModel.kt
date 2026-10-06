package com.jaeyun.tally.ui.screens.timer

import android.os.SystemClock
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
import com.jaeyun.tally.data.room.countsInStats
import com.jaeyun.tally.domain.metrics.SessionValidity
import com.jaeyun.tally.domain.model.SubjectName
import com.jaeyun.tally.ui.components.WeeklySummary
import com.jaeyun.tally.util.WeekRange
import com.jaeyun.tally.util.weekRangeOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * 주간 요약 줄의 숫자 (§8.1). [week]에 시작한 세션 중 [countsInStats] 세션의 최장 구간 최댓값과 평균.
 *
 * 셀 세션이 없으면 null이다. 화면은 숫자 대신 문장을 쓴다 — 최장 구간이 0초뿐인 주도 "0초"를 보이지 않게 null로 본다(§8.5.1).
 */
internal fun weeklySummary(sessions: List<StudySession>, week: WeekRange): WeeklySummary? {
    val lfs = sessions
        .filter { it.startAt in week && it.countsInStats }
        .map { it.lfsSec }
    val longest = lfs.maxOrNull()?.takeIf { it > 0 } ?: return null
    return WeeklySummary(longestSec = longest, averageSec = (lfs.sumOf { it.toLong() } / lfs.size).toInt())
}

/**
 * 방금 끝낸 [session]에서 체감 집중 시간을 물을지 (§3.4). 주 1회만, 예측할 수 없게 묻는다.
 * - [countsInStats] 세션만 후보다. 실측과 견줄 수 없는 세션에 그 주의 한 번을 쓰지 않는다
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
    if (!session.countsInStats) return false
    val askedThisWeek = (lastAskedAt != null && lastAskedAt in week) || weekSessions.any { it.perceivedFocusMin != null }
    if (askedThisWeek) return false
    val weekSessionIndex = weekSessions.count { it.countsInStats && it.startAt <= session.startAt }
    return random.nextInt(3) == 0 || weekSessionIndex >= 3
}

/** 방금 끝낸 세션. [askPerceived]면 리포트 전에 체감 입력을 거친다 */
data class FinishedSession(val sessionId: Long, val askPerceived: Boolean)

/** 타이머 화면 위에 띄우는 다이얼로그 (§6.3, 와이어프레임 §8). 한 번에 하나만 띄운다 */
sealed interface TimerDialog {
    /** ① 화면 꺼짐 단일 구간이 60분을 넘었다(§6.3 #6). `[공부했어요]` / `[자리 비웠어요]` */
    data class LongScreenOff(val startAt: Long, val endAt: Long) : TimerDialog

    /** ② 자리 비움을 뺀 착석 [totalSec]가 180분을 넘었다(§6.3 #4). `[계속 공부했어요]` / `[종료를 잊었어요]` */
    data class OverLong(val totalSec: Int) : TimerDialog

    /** ③ 세션을 시작한 뒤 기기를 재부팅했다(§6.3 #5). `[이어서 기록]` / `[삭제]` */
    data class Recovery(val startAt: Long, val subjectName: String?) : TimerDialog
}

/**
 * ③ 진행 중 세션을 이어서 기록할지 물을지. 세션을 시작한 뒤([startAt]) 기기가 재부팅됐고([bootAt]), 그 재부팅 뒤에 아직 답하지
 * 않았을 때만([answeredAt]보다 뒤에 재부팅) 묻는다. 강제 종료나 최근 앱에서 지운 경우는 묻지 않는다 — 타이머는 시작 시각만
 * 저장하므로 다시 열면 그대로 이어진다(PLAN.md §4).
 */
internal fun asksRecovery(startAt: Long, bootAt: Long, answeredAt: Long?): Boolean =
    bootAt > startAt && (answeredAt == null || bootAt > answeredAt)

/**
 * 타이머 탭 (§8.1, §8.5). 상태는 Room의 진행 중 세션 하나로 정해지므로, 앱을 강제 종료하거나 재부팅해도
 * 다시 열면 RUNNING이 이어진다(§6.3 #5). 경과 시간은 저장하지 않고 화면이 `now − startAt`으로 그린다.
 *
 * 과목은 선택 사항이다(§8.1.1). 처음에는 마지막 세션의 과목을 골라 둔다. 시트에서 추가한 과목은 아직 세션에
 * 쓰지 않았어도 칩에 남는다.
 *
 * 주간 요약 줄의 "이번 주"는 상태를 새로 만들 때의 현재 시각으로 정한다. 화면을 떠났다 돌아오면 다시 정해진다.
 *
 * 세션을 끝내면 리포트 전에 ① 긴 화면 꺼짐, ② 180분 초과를 차례로 묻고(#21), 체감 입력을 거칠지 정한다([shouldAskPerceived]).
 * 앱을 열었을 때 재부팅 뒤 이어지는 세션이면 ③ 이어서 기록할지 묻는다([asksRecovery]).
 *
 * @param bootTime 기기가 마지막으로 켜진 시각(epoch millis)
 */
class TimerViewModel(
    private val sessions: SessionRepository,
    private val subjects: SubjectRepository,
    private val perceived: PerceivedFocusRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: Random = Random.Default,
    private val bootTime: () -> Long = { System.currentTimeMillis() - SystemClock.elapsedRealtime() },
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

    private val _dialog = MutableStateFlow<TimerDialog?>(null)

    /** 지금 띄울 다이얼로그. 답하면 다음 다이얼로그로 넘어가거나 null이 된다 */
    val dialog: StateFlow<TimerDialog?> = _dialog.asStateFlow()

    /** 띄운 다이얼로그의 답. true면 두 번째 선택지(자리 비웠어요, 종료를 잊었어요, 삭제) */
    private var pendingAnswer: CompletableDeferred<Boolean>? = null

    /** 다이얼로그를 한 번에 하나만 띄운다 */
    private val dialogLock = Mutex()

    init {
        viewModelScope.launch {
            val last = subjects.lastUsedVisibleSubject()
            if (!userPicked) selected.value = last
        }
        viewModelScope.launch { checkRecovery() }
    }

    fun start() {
        viewModelScope.launch { sessions.start(subjectName = selected.value, now = clock()) }
    }

    /**
     * 세션을 끝낸다. 리포트로 가기 전에 ① 60분 넘게 화면이 꺼진 구간마다 공부했는지 자리를 비웠는지 묻고,
     * ② 자리 비움을 빼고도 착석이 180분을 넘으면 종료를 잊었는지 묻는다(§6.3 #4·#6). 답을 저장한 뒤에 체감 입력을 정한다 —
     * 통계 포함 여부가 바뀔 수 있어서다. 답하는 도중 앱이 정리되면 묻지 않은 상태(공부한 시간, 통계 제외)로 남는다.
     */
    fun finish() {
        val running = uiState.value as? TimerUiState.Running ?: return
        viewModelScope.launch {
            var done = sessions.finish(running.sessionId, now = clock()) ?: return@launch
            for (off in sessions.longScreenOffs(done.id)) {
                if (ask(TimerDialog.LongScreenOff(off.startAt, off.endAt))) {
                    done = sessions.markAway(done.id, off.startAt, off.endAt) ?: done
                }
            }
            if (SessionValidity.isOverLong(done.tTotalSec)) {
                val forgot = ask(TimerDialog.OverLong(done.tTotalSec))
                done = sessions.answerOverLong(done.id, forgotToFinish = forgot) ?: done
            }
            finished.send(FinishedSession(done.id, askPerceived = askPerceived(done)))
        }
    }

    /** ③ 앱을 열 때 한 번 본다. 재부팅 뒤 이어지는 세션이면 이어서 기록할지 묻는다 */
    private suspend fun checkRecovery() {
        val running = sessions.runningSession.first() ?: return
        if (!asksRecovery(running.startAt, bootTime(), sessions.recoveryAnsweredAt())) return
        if (ask(TimerDialog.Recovery(running.startAt, running.subjectName))) {
            sessions.deleteUnfinished(running.id)
        } else {
            sessions.continueAfterReboot(now = clock())
        }
    }

    /** 다이얼로그를 띄우고 답을 기다린다. 두 번째 선택지를 골랐으면 true */
    private suspend fun ask(dialog: TimerDialog): Boolean = dialogLock.withLock {
        val answer = CompletableDeferred<Boolean>()
        pendingAnswer = answer
        _dialog.value = dialog
        try {
            answer.await()
        } finally {
            _dialog.value = null
            pendingAnswer = null
        }
    }

    /** ① [away]면 `[자리 비웠어요]` */
    fun answerScreenOff(away: Boolean) = answer<TimerDialog.LongScreenOff>(away)

    /** ② [forgot]이면 `[종료를 잊었어요]` */
    fun answerOverLong(forgot: Boolean) = answer<TimerDialog.OverLong>(forgot)

    /** ③ [delete]면 `[삭제]` */
    fun answerRecovery(delete: Boolean) = answer<TimerDialog.Recovery>(delete)

    /** 띄운 다이얼로그가 [T]일 때만 답한다. 연타해도 처음 답만 쓴다 */
    private inline fun <reified T : TimerDialog> answer(second: Boolean) {
        if (_dialog.value is T) pendingAnswer?.complete(second)
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
