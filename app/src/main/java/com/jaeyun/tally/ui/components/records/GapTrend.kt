package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 인식 격차 주별 추이 (§8.4 (3)): `인식 격차  +38% → +31% → +24% → +19%`. 오래된 주부터 쓴다.
 * §5.2 부호 그대로 양수가 과대평가다. 주 1회 측정이라 크게 띄우지 않고 한 줄로 둔다.
 *
 * 화살표는 손글씨 글꼴에 없는 글자라 연필 화살표 그림으로 그린다. 좁은 화면에서는 다음 줄로 이어진다.
 *
 * @param gapPct 주별 `G`(반올림). 2주 이상
 */
@Composable
fun GapTrend(gapPct: List<Int>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.cumulative_gap),
            style = MaterialTheme.typography.bodyMedium,
            color = PencilSoft,
            modifier = Modifier.padding(end = 4.dp),
        )
        gapPct.forEachIndexed { index, pct ->
            if (index > 0) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = PencilSoft,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = if (pct > 0) stringResource(R.string.cumulative_gap_over, pct) else stringResource(R.string.cumulative_gap_value, pct),
                style = MaterialTheme.typography.titleLarge,
                color = Pencil,
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun GapTrendPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GapTrend(listOf(38, 31, 24, 19))
            // 과소평가와 0이 섞인 추이
            GapTrend(listOf(12, 0, -8))
        }
    }
}
