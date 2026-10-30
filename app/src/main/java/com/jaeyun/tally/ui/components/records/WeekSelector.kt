package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilFaint
import com.jaeyun.tally.ui.theme.TallyTheme
import java.time.LocalDate

/**
 * 주 선택 (§8.4 (2)): `◀  9월 1일 ~ 9월 7일  ▶`. 주는 월요일에 시작한다.
 *
 * 화살표는 연필 화살표 그림이다. 더 갈 주가 없으면(첫 기록의 주, 이번 주) 흐리게 두고 누를 수 없다.
 * 그 주에 세션이 없어도 화살표는 그대로 둔다(와이어프레임 §5 빈 상태).
 *
 * @param monday 보고 있는 주의 월요일
 */
@Composable
fun WeekSelector(
    monday: LocalDate,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sunday = monday.plusDays(6)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        WeekArrow(stringResource(R.string.week_previous), enabled = canGoPrevious, rotation = 180f, onClick = onPrevious)
        Text(
            text = stringResource(R.string.week_range, monday.monthValue, monday.dayOfMonth, sunday.monthValue, sunday.dayOfMonth),
            style = MaterialTheme.typography.titleLarge,
            color = Pencil,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        WeekArrow(stringResource(R.string.week_next), enabled = canGoNext, rotation = 0f, onClick = onNext)
    }
}

@Composable
private fun WeekArrow(label: String, enabled: Boolean, rotation: Float, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = label,
            tint = if (enabled) Pencil else PencilFaint,
            modifier = Modifier
                .size(28.dp)
                .rotate(rotation),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun WeekSelectorPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
        ) {
            WeekSelector(LocalDate.of(2026, 9, 28), canGoPrevious = true, canGoNext = true, onPrevious = {}, onNext = {})
            WeekSelector(LocalDate.of(2026, 10, 26), canGoPrevious = true, canGoNext = false, onPrevious = {}, onNext = {})
        }
    }
}
