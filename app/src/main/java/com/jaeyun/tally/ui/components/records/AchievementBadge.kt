package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.domain.model.GoalMetric
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.RedPen
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 지난주 목표 달성 배지 (§7.3, §8.4 (1)): 빨간 펜 도장 `지난주 목표 달성`과 그 아래 `세션당 이탈 4.4회 → 3.2회`.
 * 달성했을 때만 판정 위에 둔다. 미달성은 아무것도 보이지 않는다 — 실패를 알리지 않는다.
 *
 * @param fromValue 목표를 정할 때 본 값(지지난주). 모르면 null이고 `목표 3.4회 → 3.2회`로 쓴다
 * @param achievedValue 지난주 실제 값
 */
@Composable
fun AchievementBadge(metric: GoalMetric, fromValue: Float?, targetValue: Float, achievedValue: Float, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.badge_achieved),
            style = MaterialTheme.typography.titleLarge,
            color = RedPen,
            modifier = Modifier
                .rotate(-4f)
                .drawBehind {
                    drawRoundRect(
                        color = RedPen,
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
                .padding(horizontal = 12.dp, vertical = 2.dp),
        )
        val from = fromValue?.let { goalValueText(metric, it) } ?: stringResource(R.string.badge_target, goalValueText(metric, targetValue))
        Text(
            text = goalMetricLabel(metric),
            style = MaterialTheme.typography.bodyMedium,
            color = Pencil,
        )
        GoalValues(from = from, to = goalValueText(metric, achievedValue))
    }
}

@Preview(widthDp = 360)
@Composable
private fun AchievementBadgePreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AchievementBadge(GoalMetric.INTERRUPTION, fromValue = 4.4f, targetValue = 3.4f, achievedValue = 3.2f)
            AchievementBadge(GoalMetric.LFS, fromValue = null, targetValue = 40f, achievedValue = 43f)
        }
    }
}
