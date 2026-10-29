package com.jaeyun.tally.ui.components.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 구조 헤드라인 (§8.2 ①): `최장 34분` · `6조각`, 그 아래 `세션 1시간 38분 중 35%`와 `기록 47분`. 리포트의 무게중심이다.
 *
 * 조각 수는 `N_int + 1`이다. 숫자보다 "부서졌다"는 감각을 전하려는 표현이다. 이탈이 없으면 조각 수 대신
 * `이 세션에는 이탈이 없었어요`로 쓴다(§5.6 — 0회가 아니라 성취로).
 * 비율은 최장 구간이 세션 길이에 묶인다는 것을 보여 준다(4시간 세션과 40분 세션의 최장 구간은 바로 비교할 수 없다).
 *
 * @param lfsSharePct 착석 대비 최장 구간 비율. null이면 줄을 두지 않는다
 * @param recordLfsSec 다른 세션의 최장 구간 기록. null이면(견줄 세션이 없다) 기록 줄을 두지 않는다
 * @param isNewRecord 이 세션이 다른 모든 세션보다 길다
 */
@Composable
fun StructureHeadline(
    lfsSec: Int,
    pieces: Int,
    sittingSec: Int,
    lfsSharePct: Int?,
    recordLfsSec: Int?,
    isNewRecord: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.Bottom) {
            // 세션 내내 딴짓이면 최장 구간이 0이다. 0을 쓰지 않고 조각 수만 둔다
            if (lfsSec > 0) HeadlineNumber(stringResource(R.string.structure_longest), sessionDurationText(lfsSec.toLong()))
            if (pieces > 1) HeadlineNumber(stringResource(R.string.structure_pieces_label), stringResource(R.string.structure_pieces, pieces))
        }
        if (pieces <= 1) {
            Text(stringResource(R.string.structure_no_interruption), style = MaterialTheme.typography.bodyLarge, color = Pencil)
        }
        Spacer(Modifier.height(4.dp))
        val detail = MaterialTheme.typography.bodyMedium
        if (lfsSharePct != null && lfsSec > 0) {
            Text(stringResource(R.string.structure_share, sessionDurationText(sittingSec.toLong()), lfsSharePct), style = detail, color = PencilSoft)
        }
        when {
            isNewRecord -> Text(stringResource(R.string.structure_new_record), style = detail, color = PencilSoft)
            recordLfsSec != null -> Text(stringResource(R.string.structure_record, sessionDurationText(recordLfsSec.toLong())), style = detail, color = PencilSoft)
        }
    }
}

@Composable
private fun HeadlineNumber(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        Text(value, style = MaterialTheme.typography.headlineLarge, color = Pencil)
    }
}

@Preview(widthDp = 360)
@Composable
private fun StructureHeadlinePreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            StructureHeadline(lfsSec = 34 * 60, pieces = 6, sittingSec = 98 * 60, lfsSharePct = 34, recordLfsSec = 47 * 60, isNewRecord = false)
            StructureHeadline(lfsSec = 52 * 60, pieces = 1, sittingSec = 52 * 60, lfsSharePct = 100, recordLfsSec = 47 * 60, isNewRecord = true)
        }
    }
}
