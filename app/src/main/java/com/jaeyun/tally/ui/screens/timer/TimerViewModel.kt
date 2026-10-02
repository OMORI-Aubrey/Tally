package com.jaeyun.tally.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.repository.SessionRepository
import com.jaeyun.tally.data.repository.SubjectRepository
import com.jaeyun.tally.data.repository.SubjectSummary
import com.jaeyun.tally.domain.model.SubjectName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TimerUiState {
    /** 진행 중 세션을 아직 확인하지 못했다. IDLE을 잠깐 보였다가 RUNNING으로 바뀌는 깜빡임을 막는다 */
    data object Loading : TimerUiState

    /**
     * @param chips 칩에 보일 과목. 선택한 과목이 항상 들어 있다
     * @param selected 시작하면 이 과목으로 저장한다. null이면 과목 없이 시작한다
     * @param sheetSubjects 과목 시트 목록(숨긴 과목 포함)
     */
    data class Idle(
        val chips: List<String>,
        val selected: String?,
        val sheetSubjects: List<SubjectSummary>,
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
 * 타이머 탭 (§8.1, §8.5). 상태는 Room의 진행 중 세션 하나로 정해지므로, 앱을 강제 종료하거나 재부팅해도
 * 다시 열면 RUNNING이 이어진다(§6.3 #5). 경과 시간은 저장하지 않고 화면이 `now − startAt`으로 그린다.
 *
 * 과목은 선택 사항이다(§8.1.1). 처음에는 마지막 세션의 과목을 골라 둔다. 시트에서 추가한 과목은 아직 세션에
 * 쓰지 않았어도 칩에 남는다.
 */
class TimerViewModel(
    private val sessions: SessionRepository,
    private val subjects: SubjectRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val selected = MutableStateFlow<String?>(null)

    /** 사용자가 과목을 직접 고르면 마지막 과목 자동 선택이 늦게 도착해도 덮어쓰지 않는다 */
    private var userPicked = false

    val uiState: StateFlow<TimerUiState> = combine(
        sessions.runningSession,
        subjects.recentVisibleSubjects,
        subjects.allSubjects,
        selected,
    ) { running, recent, all, picked ->
        if (running != null) {
            TimerUiState.Running(running.id, running.startAt, running.subjectName)
        } else {
            TimerUiState.Idle(chips = subjectChips(recent, picked), selected = picked, sheetSubjects = all)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState.Loading)

    private val finished = Channel<Long>(Channel.BUFFERED)

    /** 방금 종료한 세션 id. 화면이 받아서 리포트로 이동한다 */
    val finishedSessions: Flow<Long> = finished.receiveAsFlow()

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
            sessions.finish(running.sessionId, now = clock())?.let { finished.send(it.id) }
        }
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
            initializer { TimerViewModel(appContainer.sessionRepository, appContainer.subjectRepository) }
        }
    }
}
