package com.jaeyun.tally.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.Highlighter
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilFaint
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme
import kotlin.math.roundToInt

/**
 * 연필로 그은 눈금 위를 형광펜으로 칠해 값을 고르는 슬라이더. 온보딩 자기보고 "하루 평균 몇 시간"에 쓴다(§8.7).
 *
 * [value]가 null이면 아직 고르지 않은 상태라 눈금만 그리고 손잡이와 칠은 그리지 않는다. 손잡이가 처음 놓인 자리로
 * 답이 끌리지 않게 한다(앵커링). 누르거나 끌면 가장 가까운 눈금의 값이 된다. 고른 값은 오른쪽에 [valueText]로 쓴다.
 *
 * @param valueRange 눈금 하나가 1이다
 * @param valueText 고른 값을 쓴 글자(`3시간`). 고르지 않았으면 null
 * @param label 화면 낭독기가 읽는 이름
 * @param emptyText 고르지 않았을 때 화면 낭독기가 읽는 상태
 */
@Composable
fun SketchSlider(
    value: Int?,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    valueText: String?,
    label: String,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = label
            stateDescription = valueText ?: emptyText
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = (value ?: valueRange.first).toFloat(),
                range = valueRange.first.toFloat()..valueRange.last.toFloat(),
                steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
            )
            setProgress { target ->
                currentOnValueChange(target.roundToInt().coerceIn(valueRange))
                true
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(48.dp)
                .pointerInput(valueRange) {
                    detectTapGestures { currentOnValueChange(valueAt(it.x, valueRange)) }
                }
                // 세로로 끌면 화면이 스크롤되고, 가로로 끌 때만 값을 바꾼다
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures(
                        onDragStart = { currentOnValueChange(valueAt(it.x, valueRange)) },
                    ) { change, _ ->
                        change.consume()
                        currentOnValueChange(valueAt(change.position.x, valueRange))
                    }
                }
                .drawBehind { drawSlider(value, valueRange) },
        )
        // 값이 생겨도 트랙 길이가 바뀌지 않게 자리를 미리 잡아 둔다. 화면 낭독기는 위 상태로 읽으므로 글자는 읽지 않는다
        Box(Modifier.width(ValueWidth).padding(start = 12.dp).clearAndSetSemantics {}) {
            if (valueText != null) Text(valueText, style = MaterialTheme.typography.titleLarge, color = BluePen)
        }
    }
}

/** 트랙 위 위치 [fraction](왼쪽 끝 0, 오른쪽 끝 1)에서 가장 가까운 눈금의 값. 트랙 밖은 양 끝 값이다 */
internal fun sliderValueAt(fraction: Float, valueRange: IntRange): Int =
    valueRange.first + (fraction.coerceIn(0f, 1f) * (valueRange.last - valueRange.first)).roundToInt()

/** 양 끝 눈금에 손잡이가 잘리지 않도록 트랙을 손잡이 반지름만큼 들여 그린다 */
private val ThumbRadius = 8.dp
private val ValueWidth = 88.dp

private fun PointerInputScope.valueAt(x: Float, valueRange: IntRange): Int {
    val inset = ThumbRadius.toPx()
    val track = (size.width - inset * 2).coerceAtLeast(1f)
    return sliderValueAt((x - inset) / track, valueRange)
}

private fun DrawScope.drawSlider(value: Int?, valueRange: IntRange) {
    val left = ThumbRadius.toPx()
    val right = size.width - left
    val centerY = size.height / 2
    val span = (valueRange.last - valueRange.first).coerceAtLeast(1)
    fun xOf(v: Int) = left + (right - left) * (v - valueRange.first) / span

    // 형광펜 칠을 먼저, 연필 선을 그 위에 그린다
    if (value != null) {
        val band = 14.dp.toPx()
        val overhang = 4.dp.toPx()
        drawRoundRect(
            color = Highlighter,
            topLeft = Offset(left - overhang, centerY - band / 2),
            size = Size(xOf(value) - left + overhang, band),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
    }
    drawLine(PencilSoft, Offset(left, centerY), Offset(right, centerY), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    val tick = 5.dp.toPx()
    for (v in valueRange) {
        val x = xOf(v)
        drawLine(PencilFaint, Offset(x, centerY - tick), Offset(x, centerY + tick), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
    }
    if (value != null) drawCircle(Pencil, radius = ThumbRadius.toPx(), center = Offset(xOf(value), centerY))
}

@Preview(widthDp = 360)
@Composable
private fun SketchSliderPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(null, 3, 12).forEach { hours ->
                SketchSlider(
                    value = hours,
                    onValueChange = {},
                    valueRange = 1..12,
                    valueText = hours?.let { sessionDurationText(it * 3600L) },
                    label = stringResource(R.string.onboarding_daily_label),
                    emptyText = stringResource(R.string.onboarding_daily_empty),
                )
            }
        }
    }
}
