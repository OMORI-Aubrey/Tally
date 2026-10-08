package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
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
import com.jaeyun.tally.ui.theme.RedPen
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 격차 포스트잇 (§8.2 ⓪): 체감 · 실측 최대 · 격차 · `가장 길게 이어간 건 11분`. 체감을 적은 세션(주 1회)만 리포트 맨 위에 붙는다.
 *
 * 실측 순공에는 "최대"를 붙인다(§5.4). 격차 [gapPct]는 §5.2 부호 그대로 양수가 과대평가다. 과대평가는 빨간 펜 동그라미
 * (빨간 펜의 두 쓰임 중 하나), 과소평가는 파란 볼펜으로 쓴다. 0%면 숫자 대신 `체감과 실측이 같아요`로 쓴다.
 * 최장 구간을 이 화면에서 가장 크게 쓴다(와이어프레임 §4).
 */
@Composable
fun GapNote(perceivedMin: Int, focusSec: Int, gapPct: Int, lfsSec: Int, modifier: Modifier = Modifier) {
    TapedNote(modifier) {
        // 체감 0분은 사용자가 적은 답이라 그대로 쓴다(PLAN.md §4). 아직 없는 것을 세는 0과 다르다
        val perceived = if (perceivedMin == 0) stringResource(R.string.duration_minutes, 0) else sessionDurationText(perceivedMin * 60L)
        GapRow(stringResource(R.string.gap_perceived), perceived)
        GapRow(stringResource(R.string.gap_measured), stringResource(R.string.focus_max, sessionDurationText(focusSec.toLong())))
        Spacer(Modifier.height(8.dp))
        GapMark(gapPct)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.gap_longest_lead), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        Text(sessionDurationText(lfsSec.toLong()), style = MaterialTheme.typography.displayMedium, color = BluePen)
    }
}

@Composable
private fun GapRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PencilSoft, modifier = Modifier.width(56.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Pencil)
    }
}

/** 과대평가는 빨간 펜으로 동그라미를 친다 */
@Composable
private fun GapMark(gapPct: Int) {
    val style = MaterialTheme.typography.bodyMedium
    when {
        gapPct > 0 -> Text(
            text = stringResource(R.string.gap_over, gapPct),
            style = style,
            color = RedPen,
            modifier = Modifier
                .rotate(-3f)
                .drawBehind { drawOval(RedPen, style = Stroke(width = 1.5.dp.toPx())) }
                .padding(horizontal = 14.dp, vertical = 2.dp),
        )
        gapPct < 0 -> Text(stringResource(R.string.gap_under, -gapPct), style = style, color = BluePen)
        else -> Text(stringResource(R.string.gap_same), style = style, color = BluePen)
    }
}

@Preview(widthDp = 360)
@Composable
private fun GapNotePreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            GapNote(perceivedMin = 120, focusSec = 88 * 60, gapPct = 36, lfsSec = 34 * 60)
            GapNote(perceivedMin = 40, focusSec = 50 * 60, gapPct = -20, lfsSec = 22 * 60)
        }
    }
}
