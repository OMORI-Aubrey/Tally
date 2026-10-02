package com.jaeyun.tally.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.HighlighterSoft
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilFaint
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 연필로 두 번 덧그린 동그라미 안을 형광펜으로 칠한 버튼. 타이머 시작 버튼으로 쓴다.
 *
 * 크기는 [modifier]로 정한다(정사각형). 글자는 `displayMedium`.
 */
@Composable
fun SketchCircleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val radius = size.minDimension / 2 * 0.9f
            val center = Offset(size.width / 2, size.height / 2)
            drawOval(
                color = HighlighterSoft,
                topLeft = center + Offset(-0.88f * radius, -0.82f * radius),
                size = Size(1.84f * radius, 1.76f * radius),
            )
            drawSketch(CircleMain, center, radius, radius, 2.dp)
            drawSketch(CircleSecond, center, radius, radius, 1.dp)
        }
        Text(text, style = MaterialTheme.typography.displayMedium, color = Pencil)
    }
}

/**
 * 손으로 그린 네모 테두리 버튼. 타이머 종료 버튼, 체감 입력 확인 버튼으로 쓴다.
 *
 * 크기는 [modifier]로 정한다. 글자는 `titleLarge`. 누를 수 없으면([enabled] false) 선과 글자를 흐리게 그린다.
 */
@Composable
fun SketchBoxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val color = if (enabled) Pencil else PencilFaint
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val inset = 4.dp.toPx()
            val halfW = size.width / 2 - inset
            val halfH = size.height / 2 - inset
            drawSketch(BoxOutline, Offset(size.width / 2, size.height / 2), halfW, halfH, 2.dp, color)
        }
        Text(text, style = MaterialTheme.typography.titleLarge, color = color, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

/** 시작점 + 3차 베지어 구간들. 좌표는 중심 기준 -1..1 */
private class Sketch(val start: Offset, val curves: List<Triple<Offset, Offset, Offset>>)

private fun sketch(start: Pair<Float, Float>, vararg curves: Float) = Sketch(
    start = Offset(start.first, start.second),
    curves = curves.toList().chunked(6).map { (x1, y1, x2, y2, x3, y3) ->
        Triple(Offset(x1, y1), Offset(x2, y2), Offset(x3, y3))
    },
)

private operator fun <T> List<T>.component6() = this[5]

// 끝이 살짝 겹치는 동그라미
private val CircleMain = sketch(
    0.02f to -1.0f,
    0.65f, -1.02f, 1.02f, -0.53f, 1.0f, 0.02f,
    0.98f, 0.63f, 0.51f, 1.0f, -0.04f, 0.98f,
    -0.61f, 0.96f, -1.02f, 0.53f, -1.0f, -0.02f,
    -0.98f, -0.59f, -0.53f, -0.98f, 0.16f, -0.94f,
)

// 덧그린 반쪽 획
private val CircleSecond = sketch(
    -0.1f to -0.9f,
    0.53f, -1.04f, 1.08f, -0.61f, 0.94f, 0.14f,
    0.84f, 0.69f, 0.25f, 1.0f, -0.29f, 0.86f,
)

// 모서리가 맞물리지 않는 네모
private val BoxOutline = sketch(
    -0.98f to -0.86f,
    -0.4f, -1.0f, 0.44f, -0.91f, 1.0f, -0.95f,
    1.07f, -0.3f, 1.05f, 0.34f, 1.0f, 0.9f,
    0.32f, 1.0f, -0.4f, 0.95f, -1.0f, 1.0f,
    -1.05f, 0.34f, -1.02f, -0.3f, -0.93f, -1.0f,
)

private fun DrawScope.drawSketch(
    sketch: Sketch,
    center: Offset,
    halfWidth: Float,
    halfHeight: Float,
    strokeWidth: Dp,
    color: Color = Pencil,
) {
    fun Offset.scaled() = Offset(center.x + x * halfWidth, center.y + y * halfHeight)
    val path = Path().apply {
        val start = sketch.start.scaled()
        moveTo(start.x, start.y)
        sketch.curves.forEach { (c1, c2, end) ->
            val p1 = c1.scaled()
            val p2 = c2.scaled()
            val p3 = end.scaled()
            cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        }
    }
    drawPath(path, color, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round))
}

@Preview(widthDp = 280, heightDp = 460)
@Composable
private fun SketchButtonsPreview() {
    TallyTheme {
        Column(
            modifier = Modifier.notebookPaper().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SketchCircleButton(stringResource(R.string.timer_start), onClick = {}, modifier = Modifier.size(200.dp))
            SketchBoxButton(stringResource(R.string.timer_finish), onClick = {}, modifier = Modifier.size(200.dp, 72.dp))
            SketchBoxButton(stringResource(R.string.perceived_confirm), onClick = {}, modifier = Modifier.size(200.dp, 72.dp), enabled = false)
        }
    }
}
