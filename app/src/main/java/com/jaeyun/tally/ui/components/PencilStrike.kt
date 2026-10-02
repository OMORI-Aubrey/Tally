package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 연필로 그은 가운뎃줄. [enabled]일 때만 내용 위에 살짝 기울어진 선을 긋는다.
 * 요소마다 따로 붙여서, 한 줄 전체를 하나의 선으로 긋지 않는다.
 */
fun Modifier.pencilStrike(enabled: Boolean, color: Color = Pencil): Modifier =
    if (!enabled) this else drawWithContent {
        drawContent()
        val overhang = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(-overhang, size.height * 0.58f),
            end = Offset(size.width + overhang, size.height * 0.5f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

@Preview
@Composable
private fun PencilStrikePreview() {
    TallyTheme {
        Box(Modifier.notebookPaper().padding(24.dp)) {
            Text("1시간 38분", style = MaterialTheme.typography.bodyLarge, color = PencilSoft, modifier = Modifier.pencilStrike(true))
        }
    }
}
