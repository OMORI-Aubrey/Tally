package com.jaeyun.tally.ui.screens.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.feedback.EmptyState
import com.jaeyun.tally.ui.components.notebook.HighlightedText
import com.jaeyun.tally.ui.components.notebook.NoteSectionTitle
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.records.AchievementBadge
import com.jaeyun.tally.ui.components.records.ComparisonStat
import com.jaeyun.tally.ui.components.records.DailyBarChart
import com.jaeyun.tally.ui.components.records.FirstWeekGapNote
import com.jaeyun.tally.ui.components.records.GoalCard
import com.jaeyun.tally.ui.components.records.SessionListItem
import com.jaeyun.tally.ui.components.records.TopAppsList
import com.jaeyun.tally.ui.components.records.VerdictCard
import com.jaeyun.tally.ui.components.records.WeekSelector
import com.jaeyun.tally.ui.components.records.perSessionCountText
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.components.text.totalDurationText
import com.jaeyun.tally.ui.theme.HighlighterSoft
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 기록 탭 (§8.4, 와이어프레임 §5). 위에서부터 판정 섹션(늘 이번 주)과 이번 주 섹션(주 선택)이 스크롤로 이어진다.
 * 누적 섹션은 P7에서 붙는다. 판정과 목표는 이 화면에만 있다(와이어프레임 §0 원칙 4).
 *
 * 빈 상태(§8.5.1): 세션이 하나도 없으면 섹션 없이 안내 한 줄만 둔다. 기준선이 미활성이면 판정 섹션만 안내 문구로 바꾸고,
 * 보고 있는 주에 세션이 없으면 이번 주 섹션만 안내 문구로 바꾸되 주 선택 화살표는 남긴다.
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
    // 들어올 때와 앱으로 돌아올 때마다 주 경계를 처리한다(지난 목표 판정, 이번 주 목표 생성)
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshGoals()
        onPauseOrDispose { }
    }
    when (val state = uiState) {
        RecordsUiState.Loading -> Box(modifier.fillMaxSize())
        RecordsUiState.Empty -> RecordsEmpty(modifier)
        is RecordsUiState.Content -> RecordsContent(
            state = state,
            onPreviousWeek = viewModel::showPreviousWeek,
            onNextWeek = viewModel::showNextWeek,
            onOpenSession = onOpenSession,
            modifier = modifier,
        )
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
private fun RecordsContent(
    state: RecordsUiState.Content,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val week = state.week
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = NotebookPaper.ContentStart, end = 24.dp, top = 24.dp, bottom = 32.dp),
    ) {
        item(key = "verdict") { VerdictSectionContent(state.verdict) }

        item(key = "week") {
            WeekSelector(
                monday = week.monday,
                canGoPrevious = week.canGoPrevious,
                canGoNext = week.canGoNext,
                onPrevious = onPreviousWeek,
                onNext = onNextWeek,
                modifier = Modifier.padding(top = 40.dp),
            )
        }
        if (week.days.isEmpty()) {
            item(key = "week-empty") {
                EmptyState(
                    message = stringResource(if (week.isThisWeek) R.string.week_empty_current else R.string.week_empty_past),
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
            return@LazyColumn
        }
        week.firstWeekGap?.let { gap ->
            item(key = "first-week-gap") {
                FirstWeekGapNote(gap.selfReportedDailyMin, gap.dailyFocusSec, Modifier.padding(top = 16.dp))
            }
        }
        week.numbers?.let { numbers -> item(key = "week-numbers") { WeekNumbersContent(numbers) } }
        if (week.topApps.isNotEmpty()) {
            item(key = "top-apps") {
                Column(Modifier.padding(top = 32.dp)) {
                    NoteSectionTitle(stringResource(R.string.top_apps_title))
                    TopAppsList(week.topApps, Modifier.padding(top = 4.dp))
                }
            }
        }
        sessionList(week.days, state.today, onOpenSession)
    }
}

/** 판정 섹션: 달성 배지 → 판정 → 이번 주 목표. 기준선이 미활성이면 안내 한 줄로 바꾼다 */
@Composable
private fun VerdictSectionContent(section: VerdictSection) {
    when (section) {
        is VerdictSection.Inactive -> EmptyState(stringResource(R.string.baseline_needed, section.neededSessions))
        is VerdictSection.Active -> Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            section.badge?.let { badge ->
                AchievementBadge(badge.metric, badge.fromValue, badge.targetValue, badge.achievedValue)
            }
            val verdict = section.verdict
            if (verdict == null) {
                EmptyState(stringResource(R.string.verdict_waiting))
            } else {
                VerdictCard(
                    verdict = verdict.verdict,
                    clamped = verdict.clamped,
                    longestSec = verdict.longestSec,
                    averageLfsSec = verdict.averageLfsSec,
                    baselineLfsSec = verdict.baselineLfsSec,
                    densityPct = verdict.densityPct,
                    baselineDensityPct = verdict.baselineDensityPct,
                    focusSec = verdict.focusSec,
                    baselineWeeklyFocusSec = verdict.baselineWeeklyFocusSec,
                )
            }
            section.goal?.let { goal -> GoalCard(goal.metric, goal.fromValue, goal.targetValue, goal.stretch) }
        }
    }
}

/**
 * 이번 주 숫자: L1 최장·이탈 → L2 순공/공부·딴짓·집중 밀도 → 세션 요약 → 일별 막대(와이어프레임 §0 타이포 위계).
 * 0이 되는 줄은 감추거나 문장으로 바꾼다(§8.5.1)
 */
@Composable
private fun WeekNumbersContent(numbers: WeekNumbers) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val longest = numbers.longestSec
        ComparisonStat(
            label = stringResource(R.string.week_longest),
            value = sessionDurationText(longest.value.toLong()),
            previous = longest.previous?.let { sessionDurationText(it.toLong()) },
            trend = longest.trend,
        )
        val interruptions = numbers.interruptionsPerSession
        if (interruptions.value > 0f) {
            ComparisonStat(
                label = stringResource(R.string.week_interruptions),
                value = perSessionCountText(interruptions.value),
                previous = interruptions.previous?.let { perSessionCountText(it) },
                trend = interruptions.trend,
            )
        } else {
            Text(stringResource(R.string.week_no_interruption), style = MaterialTheme.typography.titleLarge, color = Pencil)
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.totals_focus), style = MaterialTheme.typography.titleLarge, color = Pencil)
                HighlightedText(
                    text = stringResource(R.string.focus_max, totalDurationText(numbers.focusSec.toLong())),
                    style = MaterialTheme.typography.titleLarge,
                    highlight = HighlighterSoft,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.totals_sitting, totalDurationText(numbers.sittingSec.toLong())),
                style = MaterialTheme.typography.titleLarge,
                color = Pencil,
            )
            if (numbers.distractSec > 0) {
                val distract = totalDurationText(numbers.distractSec.toLong())
                val share = (numbers.distractSec * 100L / numbers.sittingSec).toInt()
                Text(
                    text = if (share > 0) {
                        stringResource(R.string.week_distract_share, distract, share)
                    } else {
                        stringResource(R.string.week_distract, distract)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Pencil,
                )
            }
        }

        val density = numbers.densityPct
        if (density.value > 0) {
            ComparisonStat(
                label = stringResource(R.string.week_density),
                value = stringResource(R.string.card_density_value, density.value),
                previous = density.previous?.let { stringResource(R.string.card_density_value, it) },
                trend = density.trend,
                valueStyle = MaterialTheme.typography.titleLarge,
            )
        }
        Text(
            text = stringResource(R.string.week_sessions, numbers.sessionCount, sessionDurationText(numbers.averageSessionSec.toLong())),
            style = MaterialTheme.typography.bodyMedium,
            color = PencilSoft,
        )
        DailyBarChart(numbers.dailyFocusSec, Modifier.padding(top = 8.dp))
    }
}

/** 보고 있는 주의 세션 목록. 날짜별로 묶고 최근 순서다 */
private fun LazyListScope.sessionList(days: List<SessionDay>, today: LocalDate, onOpenSession: (Long) -> Unit) {
    item(key = "sessions-title") {
        // 오른쪽 값이 무엇인지 여기서 한 번만 밝힌다. 줄마다 "공부"를 반복하지 않는다
        Row(Modifier.fillMaxWidth().padding(top = 32.dp), verticalAlignment = Alignment.Bottom) {
            NoteSectionTitle(stringResource(R.string.records_sessions_title))
            Spacer(Modifier.weight(1f))
            // 착석(T_total)을 "공부"로 쓴다 (PLAN.md §4 착석 표기)
            Text(stringResource(R.string.records_sitting_column), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
        }
    }
    days.forEach { day ->
        item(key = day.date.toEpochDay()) {
            HighlightedText(
                text = dayLabel(day.date, today),
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
