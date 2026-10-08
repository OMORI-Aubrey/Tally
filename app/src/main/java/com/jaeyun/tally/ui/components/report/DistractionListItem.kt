package com.jaeyun.tally.ui.components.report

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.TabularDigitsText
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.Paper
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
 *
 * 자동 분류는 틀리는 것이 기본이라 고치는 길을 짧게 둔다(§5.1). 길게 누르면(눌러도) 메뉴가 뜨고 `허용 앱으로 바꾸기`를
 * 고르면 [onAllow]. 두 번이면 바뀐다. 앱이 아닌 구간(화면 켜짐)은 [onAllow]가 null이라 메뉴가 없다.
 */
@Composable
fun DistractionListItem(
    time: String,
    appLabel: String?,
    durationSec: Int,
    modifier: Modifier = Modifier,
    onAllow: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val menuLabel = stringResource(R.string.distraction_allow)
    val pressable = if (onAllow == null) {
        Modifier.semantics(mergeDescendants = true) {}
    } else {
        Modifier.combinedClickable(
            onClick = { menuOpen = true },
            onLongClick = { menuOpen = true },
            onClickLabel = menuLabel,
            onLongClickLabel = menuLabel,
        )
    }
    Box(modifier) {
        DistractionRow(time, appLabel, durationSec, pressable)
        if (onAllow != null) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = Paper) {
                DropdownMenuItem(
                    text = { Text(menuLabel) },
                    onClick = {
                        menuOpen = false
                        onAllow()
                    },
                )
            }
        }
    }
}

@Composable
private fun DistractionRow(time: String, appLabel: String?, durationSec: Int, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
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
            DistractionListItem(time = "19:24", appLabel = "인스타그램", durationSec = 3 * 60, onAllow = {})
            DistractionListItem(time = "19:51", appLabel = null, durationSec = 20)
            DistractionListItem(time = "20:05", appLabel = "아주 긴 이름을 가진 동영상 스트리밍 앱", durationSec = 5 * 60 + 12, onAllow = {})
        }
    }
}
