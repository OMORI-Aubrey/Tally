package com.jaeyun.tally.ui.components.notebook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.MarginLine
import com.jaeyun.tally.ui.theme.Paper
import com.jaeyun.tally.ui.theme.PaperLine
import com.jaeyun.tally.ui.theme.SpiralHole
import com.jaeyun.tally.ui.theme.TallyTheme

/** 줄 노트 바탕의 치수. 화면 내용은 [ContentStart]만큼 왼쪽을 비워 여백선 오른쪽에 둔다 */
object NotebookPaper {
    /** 여백선 오른쪽, 화면 내용이 시작하는 위치 */
    val ContentStart = 56.dp

    internal val MarginX = 44.dp
    internal val LineGap = 32.dp
    internal val FirstLineY = 64.dp
    internal val HoleX = 18.dp
    internal val HoleRadius = 5.dp
    internal val HoleGap = 32.dp
}

/**
 * 줄 노트 바탕: 종이 + 가로줄 + 빨간 여백선 + 왼쪽 스프링 구멍.
 *
 * 줄은 화면에 고정되어 스크롤과 함께 움직이지 않는다. 글자를 줄에 맞추지는 않는다.
 */
fun Modifier.notebookPaper(): Modifier = drawBehind {
    drawRect(Paper)

    val hairline = 1.dp.toPx()
    var y = NotebookPaper.FirstLineY.toPx()
    while (y < size.height) {
        drawLine(PaperLine, Offset(0f, y), Offset(size.width, y), strokeWidth = hairline)
        y += NotebookPaper.LineGap.toPx()
    }

    val marginX = NotebookPaper.MarginX.toPx()
    drawLine(MarginLine, Offset(marginX, 0f), Offset(marginX, size.height), strokeWidth = hairline)

    val holeX = NotebookPaper.HoleX.toPx()
    val holeRadius = NotebookPaper.HoleRadius.toPx()
    var holeY = NotebookPaper.HoleGap.toPx() / 2
    while (holeY < size.height) {
        drawCircle(SpiralHole, holeRadius, Offset(holeX, holeY))
        drawCircle(PaperLine, holeRadius, Offset(holeX, holeY), style = Stroke(hairline))
        holeY += NotebookPaper.HoleGap.toPx()
    }
}

@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun NotebookPaperPreview() {
    TallyTheme {
        Box(Modifier.fillMaxSize().notebookPaper().padding(start = NotebookPaper.ContentStart, top = 72.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        }
    }
}
