package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilFaint
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 기록 탭 세션 목록의 한 줄: `19:00  수학 ········ 1시간 38분`. 오른쪽 값의 이름(공부 등)은 목록 머리말에 한 번만 쓴다.
 *
 * 시각을 맨 앞에 고정 폭으로 둔다. 시각은 모든 세션에 있어 줄의 시작이 늘 맞고, 과목이 없는 세션은 그 뒤가 비어
 * 있을 뿐 어긋나 보이지 않는다. `(과목 없음)`으로 채우지 않는다(와이어프레임 §0 원칙 6). 과목 이름은 굵게 쓴다.
 *
 * 통계에 들어가는 세션은 시각까지 진한 연필색으로 쓴다. 통계에서 빠지는 세션(3분 미만·180분 초과)은 글자와
 * 가운뎃줄을 모두 가장 흐린 연필색([PencilFaint])으로 쓰고, 시각·과목·값에 줄을 각각 따로 긋는다.
 *
 * @param summary 오른쪽에 둘 값. P1에서는 착석 시간, P2부터는 순공·밀도
 */
@Composable
fun SessionListItem(
    startTime: String,
    subjectName: String?,
    summary: String,
    excludedFromStats: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (excludedFromStats) PencilFaint else Pencil
    val strike = Modifier.pencilStrike(excludedFromStats, color = PencilFaint)
    val excludedLabel = stringResource(R.string.records_excluded)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) { if (excludedFromStats) stateDescription = excludedLabel },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 시각 + 과목이 남는 폭을 다 쓰고, 과목이 길면 이 안에서만 말줄임한다
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            TabularDigitsText(
                text = startTime,
                style = MaterialTheme.typography.bodyLarge,
                color = color,
                modifier = strike,
            )
            if (subjectName != null) {
                Spacer(Modifier.width(16.dp))
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(strike),
                )
            }
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier
                .padding(start = 12.dp)
                .then(strike),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun SessionListItemPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .width(360.dp)
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 8.dp, bottom = 8.dp),
        ) {
            SessionListItem("19:00", subjectName = "코틀린", summary = "1시간 38분", excludedFromStats = false)
            SessionListItem("14:20", subjectName = null, summary = "52분", excludedFromStats = false)
            SessionListItem("11:05", subjectName = "아주 긴 과목 이름이 들어가면", summary = "2시간", excludedFromStats = false)
            SessionListItem("08:58", subjectName = "자바", summary = "40초", excludedFromStats = true)
            SessionListItem("08:41", subjectName = null, summary = "1분", excludedFromStats = true)
        }
    }
}
