package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.HighlighterSoft
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val BarAreaHeight = 96.dp

/** 순공이 아주 적은 날도 막대가 보이도록 두는 최소 높이(비율) */
private const val MIN_BAR_FRACTION = 0.04f

/**
 * 일별 막대 (§8.4 (2)): 월요일부터 일요일까지 그날 순공을 형광펜 막대로 그린다. 높이는 그 주 가장 많은 날에 대한 비율이다.
 * 외부 차트 라이브러리 없이 `Row`·`Box`로만 그린다(§10.1).
 *
 * 순공이 없는 날은 막대 없이 요일만 둔다(0을 그리지 않는다, §8.5.1). 화면 낭독기는 요일마다 `월요일 순공 최대 1시간 20분`으로 읽는다.
 *
 * @param dailyFocusSec 월요일부터 일요일까지 7개
 */
@Composable
fun DailyBarChart(dailyFocusSec: List<Int>, modifier: Modifier = Modifier) {
    val max = dailyFocusSec.maxOrNull()?.takeIf { it > 0 } ?: 1
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEachIndexed { index, day ->
            val sec = dailyFocusSec.getOrElse(index) { 0 }
            val dayName = day.getDisplayName(TextStyle.FULL, Locale.KOREAN)
            val description = if (sec > 0) {
                stringResource(R.string.daily_focus_description, dayName, stringResource(R.string.focus_max, sessionDurationText(sec.toLong())))
            } else {
                stringResource(R.string.daily_focus_none, dayName)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
            ) {
                Box(Modifier.height(BarAreaHeight).width(24.dp), contentAlignment = Alignment.BottomCenter) {
                    if (sec > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((sec.toFloat() / max).coerceAtLeast(MIN_BAR_FRACTION))
                                .background(HighlighterSoft)
                                .border(1.5.dp, Pencil),
                        )
                    }
                }
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    style = MaterialTheme.typography.labelMedium,
                    color = PencilSoft,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun DailyBarChartPreview() {
    TallyTheme {
        Box(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
        ) {
            DailyBarChart(listOf(90 * 60, 100 * 60, 0, 30, 110 * 60, 0, 45 * 60))
        }
    }
}
