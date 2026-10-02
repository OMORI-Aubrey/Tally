package com.jaeyun.tally.domain.metrics

import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.Category.ALLOWED
import com.jaeyun.tally.domain.model.Category.AWAY
import com.jaeyun.tally.domain.model.Category.DISTRACT
import com.jaeyun.tally.domain.model.Category.SCREEN_OFF
import com.jaeyun.tally.domain.model.Segment
import com.jaeyun.tally.domain.model.SessionMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val MIN = 60_000L

/** 분 단위 길이로 이어 붙인 구간. 시작은 0 */
private fun timeline(vararg parts: Pair<Category, Long>): List<Segment> {
    var at = 0L
    return parts.map { (category, millis) ->
        Segment("pkg.$category", category, at, at + millis).also { at += millis }
    }
}

private fun List<Segment>.metrics(): SessionMetrics = MetricsCalculator.calculate(this, first().startAt, last().endAt)

class MetricsCalculatorTest {

    @Test
    fun `딴짓 없는 단일 구간은 순공 = 착석, 밀도 100%, 이탈 0회`() {
        val m = timeline(ALLOWED to 30 * MIN).metrics()

        assertEquals(SessionMetrics(1800, 1800, 0, 1800, 100f, 0), m)
    }

    @Test
    fun `화면 꺼짐은 순공에 들어가고 연속도 끊지 않는다`() {
        val m = timeline(ALLOWED to 5 * MIN, SCREEN_OFF to 20 * MIN, ALLOWED to 5 * MIN).metrics()

        assertEquals(30 * 60, m.tFocusSec)
        assertEquals(30 * 60, m.lfsSec)
        assertEquals(100f, m.densityPct)
        assertEquals(0, m.interruptionCount)
    }

    @Test
    fun `딴짓 1회는 그 시간만 빼고 연속을 끊는다`() {
        val m = timeline(ALLOWED to 10 * MIN, DISTRACT to 5 * MIN, ALLOWED to 15 * MIN).metrics()

        assertEquals(SessionMetrics(1800, 1500, 300, 900, 1500 * 100f / 1800, 1), m)
    }

    @Test
    fun `LFS는 끊긴 조각 중 가장 긴 것`() {
        val m = timeline(
            ALLOWED to 10 * MIN, DISTRACT to 1 * MIN, ALLOWED to 20 * MIN, DISTRACT to 2 * MIN, SCREEN_OFF to 5 * MIN,
        ).metrics()

        assertEquals(20 * 60, m.lfsSec)
        assertEquals(2, m.interruptionCount)
        assertEquals(3 * 60, m.tDistSec)
    }

    @Test
    fun `자리 비움은 착석과 순공에서 빠지고 LFS 연속을 끊는다`() {
        val m = timeline(ALLOWED to 30 * MIN, AWAY to 120 * MIN, ALLOWED to 20 * MIN).metrics()

        assertEquals(50 * 60, m.tTotalSec)
        assertEquals(50 * 60, m.tFocusSec)
        assertEquals(30 * 60, m.lfsSec)
        assertEquals(100f, m.densityPct)
        assertEquals(0, m.interruptionCount)
    }

    @Test
    fun `자리 비움을 사이에 둔 딴짓은 따로 센다`() {
        val m = timeline(DISTRACT to 1 * MIN, AWAY to 10 * MIN, DISTRACT to 1 * MIN).metrics()

        assertEquals(2, m.interruptionCount)
    }

    @Test
    fun `붙어 있는 딴짓 구간은 이탈 1회로 센다`() {
        val m = timeline(ALLOWED to 10 * MIN, DISTRACT to 1 * MIN, DISTRACT to 3 * MIN, ALLOWED to 10 * MIN).metrics()

        assertEquals(1, m.interruptionCount)
        assertEquals(4 * 60, m.tDistSec)
    }

    @Test
    fun `초 단위는 버리고 순공과 딴짓의 합이 착석과 맞는다`() {
        val segments = listOf(Segment("a", ALLOWED, 0, 8_000), Segment("d", DISTRACT, 8_000, 10_900))

        val m = MetricsCalculator.calculate(segments, 0, 10_900)

        assertEquals(10, m.tTotalSec)
        assertEquals(2, m.tDistSec)
        assertEquals(8, m.tFocusSec)
        assertEquals(m.tTotalSec, m.tFocusSec + m.tDistSec)
        assertEquals(8, m.lfsSec)
    }

    @Test
    fun `착석이 0이면 밀도는 0`() {
        assertEquals(SessionMetrics(0, 0, 0, 0, 0f, 0), MetricsCalculator.calculate(listOf(Segment("a", ALLOWED, 5, 5)), 5, 5))
        assertEquals(0f, timeline(AWAY to 10 * MIN).metrics().densityPct)
    }

    // ── 인식 격차 G ──

    @Test
    fun `G는 양수가 과대평가`() {
        // 와이어프레임 §4: 체감 100분, 실측 최대 68분 → +47%
        assertEquals(47.06f, MetricsCalculator.perceptionGapPct(perceivedFocusMin = 100, tFocusSec = 68 * 60)!!, 0.01f)
        assertEquals(-25f, MetricsCalculator.perceptionGapPct(perceivedFocusMin = 45, tFocusSec = 60 * 60)!!, 0.01f)
        assertEquals(0f, MetricsCalculator.perceptionGapPct(perceivedFocusMin = 60, tFocusSec = 60 * 60)!!, 0.01f)
    }

    @Test
    fun `체감 입력이 없거나 순공이 0이면 G는 null`() {
        assertNull(MetricsCalculator.perceptionGapPct(perceivedFocusMin = null, tFocusSec = 3600))
        assertNull(MetricsCalculator.perceptionGapPct(perceivedFocusMin = 30, tFocusSec = 0))
    }
}
