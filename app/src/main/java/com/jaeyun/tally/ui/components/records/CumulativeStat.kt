package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.components.text.totalDurationText
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 누적 섹션의 한 줄 (§8.4 (3)): `전체 순공  최대 312h 45m`, `최고 집중 시간대  14~16시 91%  (12세션)`.
 * 이름과 값, 그 뒤에 덧붙임(지난달 값, 표본 수, 날짜)을 흐린 연필로 둔다. 좁은 화면에서는 덧붙임이 다음 줄로 내려간다.
 *
 * @param note 값 뒤의 괄호. 없으면 null
 */
@Composable
fun CumulativeStat(label: String, value: String, modifier: Modifier = Modifier, note: String? = null) {
    FlowRow(
        modifier = modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        Text(value, style = MaterialTheme.typography.titleLarge, color = Pencil)
        if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
    }
}

@Preview(widthDp = 360)
@Composable
private fun CumulativeStatPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CumulativeStat(
                label = stringResource(R.string.cumulative_month),
                value = stringResource(R.string.focus_max, totalDurationText(68 * 3600 + 12 * 60L)),
                note = stringResource(R.string.cumulative_last_month, totalDurationText(54 * 3600 + 30 * 60L)),
            )
            CumulativeStat(
                label = stringResource(R.string.cumulative_total),
                value = stringResource(R.string.focus_max, totalDurationText(312 * 3600 + 45 * 60L)),
            )
            CumulativeStat(
                label = stringResource(R.string.cumulative_best_hours),
                value = stringResource(R.string.cumulative_hours_value, 14, 16, 91),
                note = stringResource(R.string.cumulative_sessions, 12),
            )
            CumulativeStat(
                label = stringResource(R.string.cumulative_longest),
                value = sessionDurationText(47 * 60L),
                note = stringResource(R.string.cumulative_date, 9, 4),
            )
        }
    }
}
