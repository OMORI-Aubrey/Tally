package com.jaeyun.tally.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/** 주간 요약 줄의 숫자. 둘 다 최장 구간(LFS)이다 — 이번 주 세션 중 최댓값과 세션 평균 */
data class WeeklySummary(val longestSec: Int, val averageSec: Int)

/**
 * 타이머 IDLE의 주간 요약 한 줄 (§8.1): `이번 주 최장 47분 · 평균 28분 →`. 기록 탭으로 가는 진입점이라 누르면 [onClick].
 *
 * 순공 총량이 아니라 1차 지표인 최장 구간을 쓴다. 판정 문구와 목표는 넣지 않는다 — 공부를 시작하기 직전 화면에서는 압박이 된다.
 * [summary]가 null이면(이번 주에 셀 세션이 없다) 숫자 자리를 비우거나 0으로 두지 않고 `첫 세션을 시작해보세요`로 대신한다(§8.5.1).
 * 이때는 기록 탭에 볼 것이 없으니 누를 수 없다.
 */
@Composable
fun WeeklySummaryLine(summary: WeeklySummary?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyMedium
    if (summary == null) {
        // 숫자 줄과 높이를 맞춰 첫 세션을 마쳤을 때 아래 칩·시작 버튼이 움직이지 않게 한다
        Box(modifier.heightIn(min = MinHeight), contentAlignment = Alignment.CenterStart) {
            Text(stringResource(R.string.weekly_summary_empty), style = style, color = PencilSoft)
        }
        return
    }

    val longestLabel = stringResource(R.string.weekly_summary_longest)
    val averageLabel = stringResource(R.string.weekly_summary_average)
    val longest = sessionDurationText(summary.longestSec.toLong())
    val average = sessionDurationText(summary.averageSec.toLong())
    // 숫자만 파란 볼펜으로 쓴다
    val text = buildAnnotatedString {
        append("$longestLabel ")
        withStyle(SpanStyle(color = BluePen)) { append(longest) }
        append(" $averageLabel ")
        withStyle(SpanStyle(color = BluePen)) { append(average) }
    }
    Row(
        modifier = modifier
            .heightIn(min = MinHeight)
            .clickable(onClickLabel = stringResource(R.string.weekly_summary_open_records), role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 글자를 키웠거나 화면이 좁아 넘치면 줄을 바꾸되 화살표는 끝에 남긴다
        Text(text, style = style, color = Pencil, modifier = Modifier.weight(1f, fill = false))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = PencilSoft,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 누를 수 있는 최소 높이 */
private val MinHeight = 48.dp

@Preview(widthDp = 360)
@Composable
private fun WeeklySummaryLinePreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .width(360.dp)
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 8.dp, bottom = 8.dp),
        ) {
            WeeklySummaryLine(WeeklySummary(longestSec = 47 * 60, averageSec = 28 * 60), onClick = {})
            WeeklySummaryLine(WeeklySummary(longestSec = 88 * 60, averageSec = 47 * 60 + 30), onClick = {})
            WeeklySummaryLine(WeeklySummary(longestSec = 40, averageSec = 25), onClick = {})
            WeeklySummaryLine(summary = null, onClick = {})
        }
    }
}
