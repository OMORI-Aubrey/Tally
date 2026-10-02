package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.TallyTheme
import com.jaeyun.tally.util.formatElapsedClock
import kotlin.math.roundToInt

/**
 * 숫자마다 같은 폭의 칸에 넣어 그린다. 손글씨 글꼴은 숫자 폭이 제각각이라(개구체 '1'과 '0'이 1.4배 차이)
 * 그냥 그리면 타이머처럼 매초 바뀌는 숫자가 좌우로 흔들린다.
 *
 * 폭이 모자라면 비율을 유지한 채 줄여서 한 줄에 맞춘다(좁은 화면, 큰 글꼴 설정).
 *
 * [charAlpha]는 글자 위치별 투명도다. 글자를 하나씩 나타내는 애니메이션에 쓴다. 그리기 단계에서만 읽으므로
 * 애니메이션 값을 넘겨도 다시 구성(recomposition)하지 않는다.
 */
@Composable
fun TabularDigitsText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    charAlpha: (index: Int) -> Float = { 1f },
) {
    val measurer = rememberTextMeasurer()
    val digitCellPx = remember(measurer, style) {
        ('0'..'9').maxOf { measurer.measure(it.toString(), style).size.width }
    }
    Layout(
        content = {
            text.forEach { Text(it.toString(), style = style, color = color, maxLines = 1, softWrap = false) }
        },
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val cells = placeables.mapIndexed { i, p -> if (text[i].isDigit()) maxOf(digitCellPx, p.width) else p.width }
        val naturalWidth = cells.sum()
        val naturalHeight = placeables.maxOfOrNull { it.height } ?: 0
        val scale = if (constraints.hasBoundedWidth && naturalWidth > constraints.maxWidth) {
            constraints.maxWidth / naturalWidth.toFloat()
        } else {
            1f
        }
        layout((naturalWidth * scale).roundToInt(), (naturalHeight * scale).roundToInt()) {
            var x = 0
            placeables.forEachIndexed { i, p ->
                val centeredX = x + (cells[i] - p.width) / 2
                p.placeWithLayer((centeredX * scale).roundToInt(), 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                    alpha = charAlpha(i)
                }
                x += cells[i]
            }
        }
    }
}

@Preview(widthDp = 300)
@Composable
private fun TabularDigitsTextPreview() {
    TallyTheme {
        Box(Modifier.notebookPaper().padding(16.dp)) {
            TabularDigitsText(
                text = formatElapsedClock((3600 + 11 * 60 + 18) * 1000L),
                style = MaterialTheme.typography.displayLarge,
                color = BluePen,
            )
        }
    }
}

@Preview(widthDp = 200)
@Composable
private fun TabularDigitsTextNarrowPreview() {
    TallyTheme {
        Box(Modifier.notebookPaper().padding(16.dp).width(160.dp)) {
            TabularDigitsText(
                text = formatElapsedClock((12 * 3600 + 34 * 60 + 56) * 1000L),
                style = MaterialTheme.typography.displayLarge,
                color = BluePen,
            )
        }
    }
}
