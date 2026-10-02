package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.PencilFaint
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/** 노트 안의 작은 소제목: 펜 글씨에 점선 밑줄. 세션 리포트의 `딴짓한 순간` 등에 쓴다 */
@Composable
fun NoteSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = PencilSoft,
        modifier = modifier
            .semantics { heading() }
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                val y = size.height - stroke
                drawLine(
                    color = PencilFaint,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                )
            },
    )
}

@Preview
@Composable
private fun NoteSectionTitlePreview() {
    TallyTheme {
        NoteSectionTitle(stringResource(R.string.report_distractions_title), Modifier.notebookPaper().padding(start = NotebookPaper.ContentStart, end = 16.dp, top = 16.dp, bottom = 16.dp))
    }
}
