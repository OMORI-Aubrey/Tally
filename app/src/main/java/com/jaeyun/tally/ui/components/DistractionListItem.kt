package com.jaeyun.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.SegmentDistract
import com.jaeyun.tally.ui.theme.SegmentScreenOn
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 이탈 목록 한 줄 (§8.2 ⑤): `19:24 ● 인스타그램 ······ 3분`. 점 색은 타임라인 스트립과 같은 규칙이다(와이어프레임 §0).
 *
 * [appLabel]이 null이면 화면만 켜고 앱은 열지 않은 구간이다. `화면 켜짐 (앱 없음)`으로 쓰고 "알림을 봤습니다" 같은
 * 추정은 덧붙이지 않는다(와이어프레임 §4).
 */
@Composable
fun DistractionListItem(time: String, appLabel: String?, durationSec: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabularDigitsText(text = time, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        Spacer(Modifier.width(12.dp))
        Spacer(
            Modifier
                .size(10.dp)
                .background(if (appLabel == null) SegmentScreenOn else SegmentDistract, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = appLabel ?: stringResource(R.string.distraction_screen_on),
            style = MaterialTheme.typography.bodyLarge,
            color = Pencil,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = sessionDurationText(durationSec.toLong()),
            style = MaterialTheme.typography.bodyLarge,
            color = Pencil,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun DistractionListItemPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 8.dp, bottom = 8.dp),
        ) {
            DistractionListItem(time = "19:24", appLabel = "인스타그램", durationSec = 3 * 60)
            DistractionListItem(time = "19:51", appLabel = null, durationSec = 20)
            DistractionListItem(time = "20:05", appLabel = "아주 긴 이름을 가진 동영상 스트리밍 앱", durationSec = 5 * 60 + 12)
        }
    }
}
