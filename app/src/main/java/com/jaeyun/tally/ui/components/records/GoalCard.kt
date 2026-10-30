package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.domain.model.GoalMetric
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.TapedNote
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 이번 주 목표 (§7.3, §8.4 (1)): 포스트잇에 하나만 쓴다.
 *
 * ```
 * 이번 주 목표
 * 세션당 이탈
 * 4.4회 → 3.4회
 * 지난 4주 평소값입니다
 * ```
 *
 * 목표값이 본인의 평소 수준이라는 것을 문구로 드러낸다. 4순위([stretch])만 평소보다 5% 앞선다 — 이탈은 적게, 나머지는 높게.
 * 화살표는 손글씨 글꼴에 없는 글자라 연필 화살표 그림으로 그린다.
 *
 * @param fromValue 목표를 정할 때 본 지난주 값. 모르면 null이고 목표값만 쓴다
 */
@Composable
fun GoalCard(metric: GoalMetric, fromValue: Float?, targetValue: Float, stretch: Boolean, modifier: Modifier = Modifier) {
    TapedNote(modifier) {
        Text(stringResource(R.string.goal_title), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        Column(Modifier.semantics(mergeDescendants = true) { }) {
            Text(goalMetricLabel(metric), style = MaterialTheme.typography.bodyLarge, color = Pencil)
            GoalValues(
                from = fromValue?.let { goalValueText(metric, it) },
                to = goalValueText(metric, targetValue),
            )
        }
        Text(
            text = when {
                !stretch -> stringResource(R.string.goal_return)
                metric == GoalMetric.INTERRUPTION -> stringResource(R.string.goal_stretch_down)
                else -> stringResource(R.string.goal_stretch_up)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = PencilSoft,
        )
    }
}

/** `4.4회 → 3.4회`. [from]이 없으면 목표값만 쓴다. 목표값은 파란 볼펜으로 쓴다 */
@Composable
internal fun GoalValues(from: String?, to: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (from != null) {
            Text(from, style = MaterialTheme.typography.titleLarge, color = Pencil)
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = PencilSoft,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(to, style = MaterialTheme.typography.titleLarge, color = BluePen)
    }
}

@Preview(widthDp = 360)
@Composable
private fun GoalCardPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            GoalCard(GoalMetric.INTERRUPTION, fromValue = 4.4f, targetValue = 3.4f, stretch = false)
            GoalCard(GoalMetric.LFS, fromValue = 38f, targetValue = 42f, stretch = true)
            GoalCard(GoalMetric.FOCUS_TOTAL, fromValue = null, targetValue = 600f, stretch = false)
        }
    }
}
