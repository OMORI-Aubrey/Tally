package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.jaeyun.tally.ui.theme.HighlighterSoft
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 총량 (§8.2 ③): `순공 최대 1시간 28분` / `공부 1시간 38분 · 딴짓 10분`. 착석(`T_total`)을 "공부"로 쓴다(PLAN.md §4).
 *
 * 순공에는 늘 "최대"를 붙이고(§5.4) 착석과 나란히 둔다. 딴짓이 없으면 딴짓 자리를 생략한다(0분을 쓰지 않는다).
 * 순공을 모르면([focusSec] null, 타임라인 없음) 착석만 쓴다.
 */
@Composable
fun TotalsRow(focusSec: Int?, sittingSec: Int, distractSec: Int?, modifier: Modifier = Modifier) {
    val sitting = stringResource(R.string.totals_sitting, sessionDurationText(sittingSec.toLong()))
    Column(modifier) {
        if (focusSec == null) {
            Text(sitting, style = MaterialTheme.typography.titleLarge, color = Pencil)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.totals_focus), style = MaterialTheme.typography.titleLarge, color = Pencil)
            HighlightedText(
                text = stringResource(R.string.focus_max, sessionDurationText(focusSec.toLong())),
                style = MaterialTheme.typography.titleLarge,
                highlight = HighlighterSoft,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        val distract = distractSec?.takeIf { it > 0 }
        Text(
            text = if (distract == null) {
                sitting
            } else {
                stringResource(R.string.totals_sitting_distract, sitting, sessionDurationText(distract.toLong()))
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Pencil,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun TotalsRowPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TotalsRow(focusSec = 88 * 60, sittingSec = 98 * 60, distractSec = 10 * 60)
            TotalsRow(focusSec = 52 * 60, sittingSec = 52 * 60, distractSec = 0)
            TotalsRow(focusSec = null, sittingSec = 45 * 60, distractSec = null)
        }
    }
}
