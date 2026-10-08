package com.jaeyun.tally.ui.screens.report

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.data.repository.ReclassifyResult
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.ui.components.DistractionListItem
import com.jaeyun.tally.ui.components.EmptyState
import com.jaeyun.tally.ui.components.GapNote
import com.jaeyun.tally.ui.components.MetricCard
import com.jaeyun.tally.ui.components.NoteSectionTitle
import com.jaeyun.tally.ui.components.NoteTopBar
import com.jaeyun.tally.ui.components.NotebookPaper
import com.jaeyun.tally.ui.components.PermissionBanner
import com.jaeyun.tally.ui.components.StructureHeadline
import com.jaeyun.tally.ui.components.TimelineStrip
import com.jaeyun.tally.ui.components.TotalsRow
import com.jaeyun.tally.ui.components.UnclassifiedAppPrompt
import com.jaeyun.tally.ui.components.sessionDurationText
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.util.Josa
import java.time.format.DateTimeFormatter

/**
 * 세션 리포트 (§8.2, 와이어프레임 §4). 위에서부터 ⓪ 격차 → ① 구조 헤드라인 → ② 타임라인 스트립 → ③ 총량 → ④ 지표 카드 → ⑤ 이탈 목록 →
 * ⑥ 미분류 앱 질문 → ⑦ 메타. 무게중심은 ⓪과 ①이고 나머지는 스크롤해서 본다. ⓪이 없으면 ①이 맨 위가 된다.
 * 이탈 목록의 앱은 길게 눌러 허용으로 바꿀 수 있고(#16), 바꾸면 이 세션을 다시 계산한다(§5.1).
 *
 * 사용 기록 권한 없이 끝낸 세션은 ①·④·⑤가 없고 ② 자리에 권한 유도 배너를 둔다(§8.2, §8.5.1). 배너는 화면이 보일 때마다
 * 권한을 다시 확인해, 설정에서 켜고 돌아오면 다음 세션부터 보인다는 한 줄로 바뀐다.
 *
 * 판정·목표는 주 단위라 이 화면에 두지 않는다. 문구는 원인을 단정하지 않는다(§5.5).
 *
 * @param onClose 닫기. 뒤로가기와 같다
 */
@Composable
fun SessionReportScreen(
    sessionId: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionReportViewModel = viewModel(factory = SessionReportViewModel.factory(sessionId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 8.dp, bottom = 32.dp),
    ) {
        NoteTopBar(stringResource(R.string.report_title), stringResource(R.string.report_close), onBack = onClose)
        when (val state = uiState) {
            SessionReportUiState.Loading -> Unit
            SessionReportUiState.Missing -> EmptyState(stringResource(R.string.report_missing), Modifier.padding(top = 48.dp))
            is SessionReportUiState.Ready -> ReportBody(
                state = state,
                permissionGranted = permissionGranted,
                onReclassify = viewModel::reclassify,
                // 앱을 지정해 열 수 없어 설치된 앱 전체 목록이 열린다(§6.4)
                onOpenSettings = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            )
        }
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat = DateTimeFormatter.ofPattern("M/d")

@Composable
private fun ColumnScope.ReportBody(
    state: SessionReportUiState.Ready,
    permissionGranted: Boolean,
    onReclassify: (packageName: String, appLabel: String, category: Category) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val report = state.report

    report.gap?.let { gap ->
        Spacer(Modifier.height(8.dp))
        GapNote(perceivedMin = gap.perceivedMin, focusSec = gap.focusSec, gapPct = gap.gapPct, lfsSec = gap.lfsSec)
    }

    report.structure?.let { structure ->
        Spacer(Modifier.height(24.dp))
        StructureHeadline(
            lfsSec = structure.lfsSec,
            pieces = structure.pieces,
            sittingSec = structure.sittingSec,
            lfsSharePct = structure.lfsSharePct,
            recordLfsSec = (structure.record as? RecordMark.Best)?.lfsSec,
            isNewRecord = structure.record == RecordMark.NewRecord,
        )
    }
    // 구조 헤드라인이 없으면 사용 기록 권한 없이 끝낸 세션이다. 스트립 자리에 권한 유도 배너를 둔다
    if (report.structure == null) {
        Spacer(Modifier.height(24.dp))
        PermissionBanner(permissionGranted = permissionGranted, onOpenSettings = onOpenSettings)
    }
    report.strip?.let { segments ->
        Spacer(Modifier.height(24.dp))
        NoteSectionTitle(stringResource(R.string.timeline_title))
        Spacer(Modifier.height(8.dp))
        TimelineStrip(
            segments = segments,
            startTime = report.meta.startTime.format(TimeFormat),
            endTime = report.meta.endTime.format(TimeFormat),
        )
    }

    Spacer(Modifier.height(24.dp))
    TotalsRow(focusSec = report.totals.focusSec, sittingSec = report.totals.sittingSec, distractSec = report.totals.distractSec)

    report.cards?.let { cards ->
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.densityPct?.let { density ->
                MetricCard(
                    label = stringResource(R.string.card_density),
                    value = stringResource(R.string.card_density_value, density),
                    tilt = 0.8f,
                    modifier = Modifier.weight(1f),
                )
            }
            MetricCard(
                label = stringResource(R.string.card_length),
                value = sessionDurationText(cards.lengthSec.toLong()),
                tilt = -1f,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (report.distractions.isNotEmpty()) {
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            NoteSectionTitle(stringResource(R.string.report_distractions_title))
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.report_distractions_hint), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
        }
        report.distractions.forEach { item ->
            DistractionListItem(
                time = item.startTime.format(TimeFormat),
                appLabel = item.appLabel,
                durationSec = item.durationSec,
                onAllow = item.packageName?.let { pkg -> { onReclassify(pkg, item.appLabel ?: pkg, Category.ALLOWED) } },
            )
        }
    }

    if (report.unclassified.isNotEmpty() || state.notice != null) {
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            report.unclassified.forEach { app ->
                UnclassifiedAppPrompt(appLabel = app.appLabel, onAnswer = { onReclassify(app.packageName, app.appLabel, it) })
            }
            state.notice?.let { NoticeText(it) }
        }
    }

    Spacer(Modifier.height(32.dp))
    val meta = report.meta
    val date = meta.date.format(DateFormat)
    val start = meta.startTime.format(TimeFormat)
    val end = meta.endTime.format(TimeFormat)
    // 과목이 없으면 자리를 생략한다(와이어프레임 §0 원칙 6)
    Text(
        text = if (meta.subjectName != null) {
            stringResource(R.string.report_meta, meta.subjectName, date, start, end)
        } else {
            stringResource(R.string.report_meta_no_subject, date, start, end)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = PencilSoft,
        modifier = Modifier.align(Alignment.End),
    )
    if (meta.excludedFromStats) {
        Text(
            stringResource(R.string.report_excluded),
            style = MaterialTheme.typography.bodyMedium,
            color = PencilSoft,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/** 방금 정한 분류와, 이 세션을 다시 계산했는지 */
@Composable
private fun NoticeText(notice: ReclassifyNotice) {
    val label = notice.appLabel
    val particle = Josa.objectOf(label)
    val allowed = notice.category == Category.ALLOWED
    val text = when (notice.result) {
        ReclassifyResult.RECOMPUTED -> stringResource(
            if (allowed) R.string.reclassify_recomputed_allowed else R.string.reclassify_recomputed_distract,
            label,
            particle,
        )
        ReclassifyResult.UNCHANGED, ReclassifyResult.SAVED_ONLY -> stringResource(
            if (allowed) R.string.reclassify_saved_allowed else R.string.reclassify_saved_distract,
            label,
            particle,
        )
    }
    Column {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Pencil)
        if (notice.result == ReclassifyResult.SAVED_ONLY) {
            Text(stringResource(R.string.reclassify_next_session), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        }
    }
}
