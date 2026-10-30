package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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

/** 지난주와 견준 방향. 표시하는 자릿수로 견준다(같아 보이면 [SAME]) */
enum class Trend { UP, DOWN, SAME }

/**
 * 이번 주 섹션의 한 줄 (§8.4 (2)): `최장 47분  ↑ 지난주 38분`. 이름과 값, 그 뒤에 지난주 값을 파란 볼펜으로 둔다.
 *
 * 화살표는 손글씨 글꼴에 없는 글자라 연필 화살표 그림으로 그린다. 늘었는지 줄었는지만 보여 주고 좋다/나쁘다는 색으로 말하지 않는다.
 * [previous]가 null이면(전주 데이터가 없다) 비교값을 감춘다(와이어프레임 §5). 좁은 화면에서는 비교값이 다음 줄로 내려간다.
 *
 * @param valueStyle 값의 글씨. 최장·이탈은 L1(`headlineLarge`), 집중 밀도는 L2(`titleLarge`)
 */
@Composable
fun ComparisonStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    previous: String? = null,
    trend: Trend = Trend.SAME,
    valueStyle: TextStyle = MaterialTheme.typography.headlineLarge,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
            Text(value, style = valueStyle, color = Pencil)
        }
        if (previous != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trend != Trend.SAME) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = stringResource(
                            if (trend == Trend.UP) R.string.week_compare_up else R.string.week_compare_down,
                        ),
                        tint = BluePen,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(20.dp)
                            .rotate(if (trend == Trend.UP) -90f else 90f),
                    )
                }
                Text(stringResource(R.string.week_previous_value, previous), style = MaterialTheme.typography.bodyMedium, color = BluePen)
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun ComparisonStatPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComparisonStat(
                label = stringResource(R.string.week_longest),
                value = sessionDurationText(47 * 60L),
                previous = sessionDurationText(38 * 60L),
                trend = Trend.UP,
            )
            ComparisonStat(
                label = stringResource(R.string.week_interruptions),
                value = perSessionCountText(4.2f),
                previous = perSessionCountText(5.1f),
                trend = Trend.DOWN,
            )
            ComparisonStat(
                label = stringResource(R.string.week_density),
                value = stringResource(R.string.card_density_value, 78),
                previous = stringResource(R.string.card_density_value, 78),
                valueStyle = MaterialTheme.typography.titleLarge,
            )
            // 전주 데이터가 없으면 비교값을 감춘다
            ComparisonStat(label = stringResource(R.string.week_longest), value = sessionDurationText(1 * 3600 + 5 * 60L))
        }
    }
}
