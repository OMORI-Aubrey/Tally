package com.jaeyun.tally.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.repository.ReclassifyResult
import com.jaeyun.tally.data.repository.SessionRepository
import com.jaeyun.tally.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

sealed interface SessionReportUiState {
    data object Loading : SessionReportUiState

    /** 세션이 없거나 아직 끝나지 않았다 */
    data object Missing : SessionReportUiState

    /** [notice]는 방금 앱 분류를 정한 결과. 다시 계산했는지, 다음 세션부터 반영되는지 알린다 */
    data class Ready(val report: SessionReport, val notice: ReclassifyNotice? = null) : SessionReportUiState
}

/** 앱 분류를 정한 뒤(미분류 앱 질문, 이탈 목록 길게 누르기) 남기는 한 줄 */
data class ReclassifyNotice(val appLabel: String, val category: Category, val result: ReclassifyResult)

/**
 * 세션 리포트 (§8.2). 종료 직후(체감 입력을 거쳤을 수도 있다)와 기록 탭 세션 목록에서 연다.
 *
 * 미분류 앱 질문에 답하거나 이탈 목록에서 허용으로 바꾸면(#16) 분류를 저장하고, 분류가 바뀌었으면 이 세션을 다시 계산한 뒤
 * 리포트를 다시 읽는다.
 */
class SessionReportViewModel(
    private val sessionId: Long,
    private val sessions: SessionRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionReportUiState>(SessionReportUiState.Loading)
    val uiState: StateFlow<SessionReportUiState> = _uiState.asStateFlow()

    /** 답을 처리하는 동안 다른 답을 받지 않는다(연타, 두 앱 연달아) */
    private var answering = false

    init {
        viewModelScope.launch { _uiState.value = load(notice = null) }
    }

    fun reclassify(packageName: String, appLabel: String, category: Category) {
        if (answering) return
        answering = true
        viewModelScope.launch {
            val result = sessions.reclassify(sessionId, packageName, category)
            _uiState.value = load(ReclassifyNotice(appLabel, category, result))
            answering = false
        }
    }

    private suspend fun load(notice: ReclassifyNotice?): SessionReportUiState {
        val data = sessions.loadReport(sessionId) ?: return SessionReportUiState.Missing
        return SessionReportUiState.Ready(buildSessionReport(data, zone()), notice)
    }

    companion object {
        fun factory(sessionId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { SessionReportViewModel(sessionId, appContainer.sessionRepository) }
        }
    }
}
