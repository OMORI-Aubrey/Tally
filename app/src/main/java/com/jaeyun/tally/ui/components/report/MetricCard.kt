package com.jaeyun.tally.ui.components.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.IndexCard
import com.jaeyun.tally.ui.theme.MarginLine
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 지표 카드 (§8.2 ④): 흰 색인 카드에 이름과 값. 윗선은 여백선 색이다. 카드마다 [tilt]를 조금씩 달리 줘 손으로 붙인 것처럼 둔다.
 */
@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, tilt: Float = 0f) {
    Column(
        modifier
            .rotate(tilt)
            .background(IndexCard)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawLine(MarginLine, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), strokeWidth = stroke)
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        Text(value, style = MaterialTheme.typography.titleLarge, color = Pencil)
    }
}

@Preview(widthDp = 360)
@Composable
private fun MetricCardPreview() {
    TallyTheme {
        Row(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(stringResource(R.string.card_density), stringResource(R.string.card_density_value, 90), Modifier.weight(1f), tilt = 0.8f)
            MetricCard(stringResource(R.string.card_length), sessionDurationText(98 * 60L), Modifier.weight(1f), tilt = -1f)
        }
    }
}
