package com.jaeyun.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.TabularDigitsText
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.SegmentAllowed
import com.jaeyun.tally.ui.theme.SegmentAway
import com.jaeyun.tally.ui.theme.SegmentDistract
import com.jaeyun.tally.ui.theme.SegmentScreenOff
import com.jaeyun.tally.ui.theme.SegmentScreenOn
import com.jaeyun.tally.ui.theme.TallyTheme

/** 스트립 한 칸의 종류. 색은 이탈 목록과 같은 규칙이다(와이어프레임 §0). 범례도 이 순서로 쓴다 */
enum class TimelineKind {
    ALLOWED,
    SCREEN_OFF,
    DISTRACT,

    /** 화면만 켜고 앱은 열지 않았다(`__SCREEN_ON_UNKNOWN__`). 딴짓 계열이다 */
    SCREEN_ON,

    /** 화면이 60분 넘게 꺼져 있었다. 공부 시간에서 빠진다 */
    AWAY,
}

/** 스트립 한 칸. [durationMillis]가 그대로 폭의 비율이 된다 */
data class TimelineSegment(val kind: TimelineKind, val durationMillis: Long)

/**
 * 타임라인 스트립 (§8.2 ②, §8.3): 세션 전체를 왼쪽에서 오른쪽으로 한 막대에 그린다. 구조 헤드라인의 "6조각"을 그림으로 확인시킨다.
 *
 * `Row` + `weight(구간 길이)`로만 그린다. Canvas를 쓰지 않는다. 비율을 고치지 않으므로 [segments]는 세션 전체를 빈틈 없이
 * 덮어야 한다 — 빈틈이 있으면 빈칸이 아니라 나머지 구간이 늘어나 조용히 왜곡된다(§6.2.2). 이 검사는 부르는 쪽이 한다.
 *
 * 딴짓 계열(딴짓·화면 켜짐)은 위아래를 꽉 채우고 나머지는 조금 얇게 그린다. 짧은 딴짓도 막대 밖으로 튀어나와 보인다.
 * 아래에 시작·종료 시각과, 이 세션에 나온 종류만 범례로 둔다.
 */
@Composable
fun TimelineStrip(segments: List<TimelineSegment>, startTime: String, endTime: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.timeline_description)
    val drawn = segments.filter { it.durationMillis > 0 }
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(StripHeight)
                .clip(RoundedCornerShape(8.dp))
                .semantics { contentDescription = description },
        ) {
            drawn.forEach { segment ->
                val loud = segment.kind == TimelineKind.DISTRACT || segment.kind == TimelineKind.SCREEN_ON
                Box(
                    Modifier
                        .weight(segment.durationMillis.toFloat())
                        .fillMaxHeight()
                        .padding(vertical = if (loud) 0.dp else 4.dp)
                        .background(segment.kind.color),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            TabularDigitsText(text = startTime, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
            Spacer(Modifier.weight(1f))
            TabularDigitsText(text = endTime, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val present = drawn.map { it.kind }.toSet()
            TimelineKind.entries.filter { it in present }.forEach { kind ->
                LegendItem(kind)
            }
        }
    }
}

@Composable
private fun LegendItem(kind: TimelineKind) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(10.dp)
                .background(kind.color, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(stringResource(kind.label), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
    }
}

private val StripHeight = 40.dp

private val TimelineKind.color: Color
    get() = when (this) {
        TimelineKind.ALLOWED -> SegmentAllowed
        TimelineKind.SCREEN_OFF -> SegmentScreenOff
        TimelineKind.DISTRACT -> SegmentDistract
        TimelineKind.SCREEN_ON -> SegmentScreenOn
        TimelineKind.AWAY -> SegmentAway
    }

private val TimelineKind.label: Int
    get() = when (this) {
        TimelineKind.ALLOWED -> R.string.timeline_allowed
        TimelineKind.SCREEN_OFF -> R.string.timeline_screen_off
        TimelineKind.DISTRACT -> R.string.timeline_distract
        TimelineKind.SCREEN_ON -> R.string.timeline_screen_on
        TimelineKind.AWAY -> R.string.timeline_away
    }

private const val MIN = 60_000L

@Preview(widthDp = 360)
@Composable
private fun TimelineStripPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 98분 세션: 6조각, 화면만 켠 20초 포함
            TimelineStrip(
                segments = listOf(
                    TimelineSegment(TimelineKind.ALLOWED, 10 * MIN),
                    TimelineSegment(TimelineKind.SCREEN_OFF, 14 * MIN),
                    TimelineSegment(TimelineKind.DISTRACT, 3 * MIN),
                    TimelineSegment(TimelineKind.SCREEN_OFF, 24 * MIN),
                    TimelineSegment(TimelineKind.SCREEN_ON, 20_000),
                    TimelineSegment(TimelineKind.ALLOWED, 13 * MIN + 40_000),
                    TimelineSegment(TimelineKind.DISTRACT, 5 * MIN),
                    TimelineSegment(TimelineKind.SCREEN_OFF, 24 * MIN),
                    TimelineSegment(TimelineKind.DISTRACT, 40_000),
                    TimelineSegment(TimelineKind.ALLOWED, 4 * MIN + 20_000),
                ),
                startTime = "19:00",
                endTime = "20:38",
            )
            // 이탈 없는 세션
            TimelineStrip(
                segments = listOf(TimelineSegment(TimelineKind.ALLOWED, 2 * MIN), TimelineSegment(TimelineKind.SCREEN_OFF, 50 * MIN)),
                startTime = "21:10",
                endTime = "22:02",
            )
            // 자리 비움이 낀 세션
            TimelineStrip(
                segments = listOf(
                    TimelineSegment(TimelineKind.SCREEN_OFF, 30 * MIN),
                    TimelineSegment(TimelineKind.AWAY, 70 * MIN),
                    TimelineSegment(TimelineKind.DISTRACT, 2 * MIN),
                    TimelineSegment(TimelineKind.SCREEN_OFF, 20 * MIN),
                ),
                startTime = "14:00",
                endTime = "16:02",
            )
        }
    }
}
