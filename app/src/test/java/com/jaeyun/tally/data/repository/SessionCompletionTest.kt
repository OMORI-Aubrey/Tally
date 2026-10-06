package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.domain.metrics.MetricsVersion
import com.jaeyun.tally.domain.model.Category.ALLOWED
import com.jaeyun.tally.domain.model.Category.AWAY
import com.jaeyun.tally.domain.model.Category.DISTRACT
import com.jaeyun.tally.domain.model.Segment
import com.jaeyun.tally.domain.reconstructor.ReconstructedTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN = 60_000L

class SessionCompletionTest {

    private val session = StudySession(id = 7, subjectName = "수학", startAt = 0, createdAt = 0)

    private fun timeline(vararg segments: Segment, violation: String? = null) =
        ReconstructedTimeline(segments.toList(), violation)

    @Test
    fun `권한이 없으면 착석만 저장하고 지표는 계산하지 않은 상태로 둔다`() {
        val done = completeSession(session, now = 30 * MIN, timeline = null, strictInvariant = true) {}

        assertEquals(30 * MIN, done.session.endAt)
        assertEquals(30 * 60, done.session.tTotalSec)
        assertEquals(0, done.session.tFocusSec)
        assertFalse(done.session.hasTimeline)
        assertEquals(MetricsVersion.NOT_COMPUTED, done.session.metricsVersion)
        assertTrue(done.session.isValidForStats)
        assertTrue(done.segments.isEmpty())
    }

    @Test
    fun `타임라인이 있으면 지표와 구간을 함께 저장한다`() {
        val done = completeSession(
            session,
            now = 30 * MIN,
            timeline = timeline(
                Segment("self", ALLOWED, 0, 10 * MIN),
                Segment("yt", DISTRACT, 10 * MIN, 15 * MIN),
                Segment("self", ALLOWED, 15 * MIN, 30 * MIN),
            ),
            strictInvariant = true,
        ) {}

        with(done.session) {
            assertEquals(30 * 60, tTotalSec)
            assertEquals(25 * 60, tFocusSec)
            assertEquals(5 * 60, tDistSec)
            assertEquals(15 * 60, lfsSec)
            assertEquals(1, interruptionCount)
            assertEquals(MetricsVersion.CURRENT, metricsVersion)
            assertTrue(hasTimeline)
            assertTrue(isValidForStats)
        }
        assertEquals(listOf(7L, 7L, 7L), done.segments.map { it.sessionId })
        assertEquals(listOf("self", "yt", "self"), done.segments.map { it.packageName })
        assertEquals(30 * MIN, done.segments.sumOf { it.endAt - it.startAt })
    }

    @Test
    fun `3분 미만 세션은 지표를 저장하되 통계에서 뺀다`() {
        val done = completeSession(
            session, now = 2 * MIN, timeline = timeline(Segment("self", ALLOWED, 0, 2 * MIN)), strictInvariant = true,
        ) {}

        assertEquals(MetricsVersion.CURRENT, done.session.metricsVersion)
        assertFalse(done.session.isValidForStats)
    }

    @Test
    fun `통계 포함 여부는 자리 비움을 뺀 착석으로 정한다`() {
        // 4시간 세션 중 2시간 30분을 자리 비움으로 바꿨다. 착석 1시간 30분이라 통계에 들어간다
        val done = completeSession(
            session,
            now = 240 * MIN,
            timeline = timeline(
                Segment("self", ALLOWED, 0, 60 * MIN),
                Segment("off", AWAY, 60 * MIN, 210 * MIN),
                Segment("self", ALLOWED, 210 * MIN, 240 * MIN),
            ),
            strictInvariant = true,
        ) {}

        assertEquals(90 * 60, done.session.tTotalSec)
        assertTrue(done.session.isValidForStats)
    }

    @Test
    fun `다시 복원한 타임라인이 저장된 구간과 같은지 본다`() {
        val stored = completeSession(
            session,
            now = 30 * MIN,
            timeline = timeline(Segment("self", ALLOWED, 0, 10 * MIN), Segment("yt", DISTRACT, 10 * MIN, 30 * MIN)),
            strictInvariant = true,
        ) {}.segments

        assertTrue(sameTimeline(timeline(Segment("self", ALLOWED, 0, 10 * MIN), Segment("yt", DISTRACT, 10 * MIN, 30 * MIN)), stored))
        // 사용 기록이 지워져 본 앱 구간 하나만 남았다
        assertFalse(sameTimeline(timeline(Segment("self", ALLOWED, 0, 30 * MIN)), stored))
        // 분류가 다르다
        assertFalse(sameTimeline(timeline(Segment("self", ALLOWED, 0, 10 * MIN), Segment("yt", ALLOWED, 10 * MIN, 30 * MIN)), stored))
    }

    @Test(expected = IllegalStateException::class)
    fun `debug 빌드는 불변식이 깨지면 예외`() {
        completeSession(
            session, now = 30 * MIN, timeline = timeline(Segment("self", ALLOWED, 0, 20 * MIN), violation = "빈틈"),
            strictInvariant = true,
        ) {}
    }

    @Test
    fun `release 빌드는 불변식이 깨지면 알리고 통계에서 뺀다`() {
        val messages = mutableListOf<String>()
        val done = completeSession(
            session, now = 30 * MIN, timeline = timeline(Segment("self", ALLOWED, 0, 20 * MIN), violation = "빈틈"),
            strictInvariant = false,
        ) { messages += it }

        assertEquals(1, messages.size)
        assertTrue(messages.single().contains("빈틈"))
        assertFalse(done.session.isValidForStats)
        assertTrue(done.session.hasTimeline)
    }
}
