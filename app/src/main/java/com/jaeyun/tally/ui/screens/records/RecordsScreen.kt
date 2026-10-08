package com.jaeyun.tally.ui.screens.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.feedback.EmptyState
import com.jaeyun.tally.ui.components.notebook.HighlightedText
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.records.SessionListItem
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.PencilSoft
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 기록 탭 (§8.4). P1에서는 날짜별 세션 목록만 있다. 판정·이번 주·누적 섹션은 P6·P7에서 목록 위에 붙는다.
 * 세션이 하나도 없으면 섹션 없이 안내 한 줄만 둔다(§8.5.1).
 *
 * @param onOpenSession 세션 목록 한 줄을 눌렀다. 그 세션의 리포트를 연다
 */
@Composable
fun RecordsScreen(
    onOpenSession: (sessionId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = viewModel(factory = RecordsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        RecordsUiState.Loading -> Box(modifier.fillMaxSize())
        RecordsUiState.Empty -> RecordsEmpty(modifier)
        is RecordsUiState.Content -> SessionList(state, onOpenSession, modifier)
    }
}

@Composable
private fun RecordsEmpty(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = NotebookPaper.ContentStart, end = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(stringResource(R.string.records_empty))
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun SessionList(state: RecordsUiState.Content, onOpenSession: (Long) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = NotebookPaper.ContentStart, end = 24.dp, top = 24.dp, bottom = 24.dp),
    ) {
        item {
            // 오른쪽 값이 무엇인지 여기서 한 번만 밝힌다. 줄마다 "공부"를 반복하지 않는다
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.records_sessions_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = PencilSoft,
                    modifier = Modifier.weight(1f),
                )
                // 착석(T_total)을 "공부"로 쓴다 (PLAN.md §4 착석 표기)
                Text(stringResource(R.string.records_sitting_column), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
            }
        }
        state.days.forEach { day ->
            item(key = day.date.toEpochDay()) {
                HighlightedText(
                    text = dayLabel(day.date, state.today),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
                )
            }
            items(day.sessions, key = { it.id }) { row ->
                SessionListItem(
                    startTime = row.startTime.format(TimeFormat),
                    subjectName = row.subjectName,
                    summary = sessionDurationText(row.sessionSec.toLong()),
                    excludedFromStats = !row.validForStats,
                    modifier = Modifier.clickable(onClickLabel = stringResource(R.string.records_open_report)) { onOpenSession(row.id) },
                )
            }
        }
    }
}

/** `오늘`, `어제`, 그 밖에는 `9월 28일 (월)` */
@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.records_today)
    today.minusDays(1) -> stringResource(R.string.records_yesterday)
    else -> stringResource(
        R.string.records_date,
        date.monthValue,
        date.dayOfMonth,
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
    )
}
