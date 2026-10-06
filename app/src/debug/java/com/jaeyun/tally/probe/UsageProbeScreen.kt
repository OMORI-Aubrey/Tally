package com.jaeyun.tally.probe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.domain.model.Category

private val WINDOW_OPTIONS = listOf(5, 15, 30, 60)
private val EXTEND_OPTIONS = listOf(10, 30, 60)

@Composable
fun UsageProbeScreen(
    state: ProbeUiState,
    onOpenSettings: () -> Unit,
    onSelectWindow: (Int) -> Unit,
    onTargetOnlyChange: (Boolean) -> Unit,
    onQuery: () -> Unit,
    onCopy: () -> Unit,
    onClearPerceived: () -> Unit,
    onExtendRunning: (minutes: Int) -> Unit,
    onResetOnboarding: () -> Unit,
    onQuickScreenOffCheckChange: (Boolean) -> Unit,
) {
    val res = LocalResources.current
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(stringResource(R.string.probe_title), style = MaterialTheme.typography.headlineSmall)
            }
            item {
                PermissionSection(granted = state.permissionGranted, onOpenSettings = onOpenSettings)
            }
            item { SectionTitle(stringResource(R.string.probe_section_last_session)) }
            val lastSession = state.lastSession
            if (lastSession == null) {
                item { SecondaryText(stringResource(R.string.probe_last_session_none)) }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProbeText.lastSessionLines(res, lastSession).forEach { HighlightText(it) }
                    }
                }
                items(lastSession.segments) { segment ->
                    Text(
                        text = ProbeText.segment(res, segment, lastSession.labels),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (segment.category == Category.DISTRACT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // 임시(#14): 긴 세션을 기다리지 않고 시험한다
            item { SectionTitle(stringResource(R.string.probe_section_running)) }
            val running = state.runningSession
            if (running == null) {
                item { SecondaryText(stringResource(R.string.probe_running_none)) }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HighlightText(ProbeText.running(res, running, System.currentTimeMillis()))
                        SecondaryText(stringResource(R.string.probe_running_hint))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EXTEND_OPTIONS.forEach { minutes ->
                                OutlinedButton(onClick = { onExtendRunning(minutes) }) {
                                    Text(stringResource(R.string.probe_running_extend, minutes))
                                }
                            }
                        }
                    }
                }
            }

            state.perceivedWeek?.let { perceived ->
                item { SectionTitle(stringResource(R.string.probe_section_perceived)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProbeText.perceivedLines(res, perceived).forEach { HighlightText(it) }
                        OutlinedButton(onClick = onClearPerceived) { Text(stringResource(R.string.probe_perceived_clear)) }
                    }
                }
            }

            // 임시(#21): ① 다이얼로그를 60분 기다리지 않고 시험한다
            item { SectionTitle(stringResource(R.string.probe_section_screen_off_check)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HighlightText(
                        stringResource(
                            if (state.quickScreenOffCheck) R.string.probe_screen_off_check_quick else R.string.probe_screen_off_check_normal,
                        ),
                    )
                    SecondaryText(stringResource(R.string.probe_screen_off_check_hint))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Switch(checked = state.quickScreenOffCheck, onCheckedChange = onQuickScreenOffCheckChange)
                        BodyText(stringResource(R.string.probe_screen_off_check_toggle))
                    }
                }
            }

            state.onboarding?.let { onboarding ->
                item { SectionTitle(stringResource(R.string.probe_section_onboarding)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HighlightText(
                            stringResource(if (onboarding.done) R.string.probe_onboarding_done else R.string.probe_onboarding_not_done),
                        )
                        HighlightText(
                            onboarding.selfReportedDailyMin?.let { stringResource(R.string.probe_onboarding_self_report, it) }
                                ?: stringResource(R.string.probe_onboarding_self_report_none),
                        )
                        SecondaryText(stringResource(R.string.probe_onboarding_reset_hint))
                        OutlinedButton(onClick = onResetOnboarding) { Text(stringResource(R.string.probe_onboarding_reset)) }
                    }
                }
            }

            if (!state.permissionGranted) return@LazyColumn

            item {
                QueryControls(
                    windowMinutes = state.windowMinutes,
                    targetOnly = state.targetOnly,
                    onSelectWindow = onSelectWindow,
                    onTargetOnlyChange = onTargetOnlyChange,
                    onQuery = onQuery,
                    onCopy = onCopy,
                )
            }

            val result = state.result ?: return@LazyColumn

            item { SectionTitle(stringResource(R.string.probe_section_summary)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BodyText(ProbeText.range(res, result))
                    BodyText(ProbeText.latest(res, result))
                    ProbeText.selfCheck(res, result)?.let { HighlightText(it) }
                    SecondaryText(ProbeText.typeCounts(res, result))
                }
            }

            item { SectionTitle(stringResource(R.string.probe_section_screen_on)) }
            if (result.samples.isEmpty()) {
                item { SecondaryText(stringResource(R.string.probe_screen_on_empty)) }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProbeText.statsLines(res, result.stats).forEach { HighlightText(it) }
                    }
                }
                items(result.samples.asReversed()) { sample ->
                    BodyText(ProbeText.sample(res, sample, result.labels))
                }
            }

            item { SectionTitle(stringResource(R.string.probe_section_classification)) }
            items(result.classifications) { item ->
                Text(
                    text = ProbeText.classification(res, item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.category == Category.DISTRACT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }

            val rows = result.rows
                .filter { !state.targetOnly || it.event.type in ScreenOnAnalyzer.TARGET_TYPES }
                .asReversed()
            item { SectionTitle(stringResource(R.string.probe_section_events, rows.size)) }
            items(rows) { row ->
                EventRow(row = row, isTarget = row.event.type in ScreenOnAnalyzer.TARGET_TYPES)
            }
        }
    }
}

@Composable
private fun PermissionSection(granted: Boolean, onOpenSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(if (granted) R.string.probe_permission_granted else R.string.probe_permission_denied),
            style = MaterialTheme.typography.titleMedium,
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        if (!granted) SecondaryText(stringResource(R.string.probe_permission_hint))
        OutlinedButton(onClick = onOpenSettings) {
            Text(stringResource(R.string.probe_open_settings))
        }
    }
}

@Composable
private fun QueryControls(
    windowMinutes: Int,
    targetOnly: Boolean,
    onSelectWindow: (Int) -> Unit,
    onTargetOnlyChange: (Boolean) -> Unit,
    onQuery: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WINDOW_OPTIONS.forEach { minutes ->
                FilterChip(
                    selected = minutes == windowMinutes,
                    onClick = { onSelectWindow(minutes) },
                    label = { Text(stringResource(R.string.probe_window_minutes, minutes)) },
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(checked = targetOnly, onCheckedChange = onTargetOnlyChange)
            BodyText(stringResource(R.string.probe_target_only))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onQuery) { Text(stringResource(R.string.probe_query)) }
            OutlinedButton(onClick = onCopy) { Text(stringResource(R.string.probe_copy)) }
        }
    }
}

@Composable
private fun EventRow(row: ProbeRow, isTarget: Boolean) {
    val res = LocalResources.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = ProbeText.eventTitle(row),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryText(ProbeText.eventDetail(res, row))
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun HighlightText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SecondaryText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
