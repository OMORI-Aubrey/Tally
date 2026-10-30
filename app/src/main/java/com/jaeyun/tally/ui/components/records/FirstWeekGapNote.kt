package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.TapedNote
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 1주차 격차 (§5.8): 온보딩에서 적은 하루 공부 시간과 이번 주 실측 하루 평균 순공을 나란히 둔 포스트잇.
 * 첫 주에만 한 번 보인다. 판정 기준으로 쓰지 않고 대조만 한다.
 *
 * ```
 * 시작할 때 적으신 하루       3시간
 * 이번 주 실측 하루 평균      최대 1시간 42분
 * ```
 *
 * @param selfReportedDailyMin 온보딩에서 적은 하루 평균 공부 시간(분)
 * @param dailyFocusSec 이번 주 실측 하루 평균 순공. 순공 합을 오늘까지 지난 날 수로 나눈 값
 */
@Composable
fun FirstWeekGapNote(selfReportedDailyMin: Int, dailyFocusSec: Int, modifier: Modifier = Modifier) {
    TapedNote(modifier) {
        Column(Modifier.semantics(mergeDescendants = true) { }) {
            Text(stringResource(R.string.first_week_self), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
            Text(sessionDurationText(selfReportedDailyMin * 60L), style = MaterialTheme.typography.titleLarge, color = Pencil)
        }
        Column(Modifier.padding(top = 8.dp).semantics(mergeDescendants = true) { }) {
            Text(stringResource(R.string.first_week_actual), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
            Text(
                text = stringResource(R.string.focus_max, sessionDurationText(dailyFocusSec.toLong())),
                style = MaterialTheme.typography.titleLarge,
                color = BluePen,
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun FirstWeekGapNotePreview() {
    TallyTheme {
        Box(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
        ) {
            FirstWeekGapNote(selfReportedDailyMin = 180, dailyFocusSec = 102 * 60)
        }
    }
}
