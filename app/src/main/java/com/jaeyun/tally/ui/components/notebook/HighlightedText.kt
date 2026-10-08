package com.jaeyun.tally.ui.components.notebook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.ui.theme.Highlighter
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.TallyTheme

/** 글자 아래쪽 절반을 형광펜으로 그은 텍스트. 선택한 과목, 순공 강조에 쓴다 */
@Composable
fun HighlightedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Pencil,
    highlight: Color = Highlighter,
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.drawBehind {
            val overhang = 4.dp.toPx()
            rotate(degrees = -1.5f) {
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(-overhang, size.height * 0.4f),
                    size = Size(size.width + overhang * 2, size.height * 0.5f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        },
    )
}

@Preview
@Composable
private fun HighlightedTextPreview() {
    TallyTheme {
        Box(Modifier.notebookPaper().padding(24.dp)) {
            HighlightedText("코틀린", style = MaterialTheme.typography.titleLarge)
        }
    }
}
