package com.jaeyun.tally.ui.screens.timer

import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.util.weekRangeOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

/** `nextInt(3)`이 늘 [value]를 낸다. 0이면 1/3 확률에 걸린 것이다 */
private class FixedRandom(private val value: Int) : Random() {
    override fun nextBits(bitCount: Int): Int = 0
    override fun nextInt(until: Int): Int = value
}

private val HIT = FixedRandom(0)
private val MISS = FixedRandom(1)

class PerceivedTriggerTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    /** 2026-09-28(월) ~ 10-05(월) 00:00 */
    private val week = weekRangeOf(at("2026-10-02T12:00"), seoul)

    private fun at(time: String) = LocalDateTime.parse(time).atZone(seoul).toInstant().toEpochMilli()

    private fun session(
        start: String,
        valid: Boolean = true,
        hasTimeline: Boolean = true,
        perceivedMin: Int? = null,
    ) = StudySession(
        subjectName = null,
        startAt = at(start),
        endAt = at(start) + 30 * 60_000L,
        perceivedFocusMin = perceivedMin,
        isValidForStats = valid,
        hasTimeline = hasTimeline,
        createdAt = at(start),
    )

    /** 이번 주 세션 [earlier] 뒤에 [current]를 막 끝냈다 */
    private fun ask(current: StudySession, earlier: List<StudySession> = emptyList(), lastAskedAt: Long? = null, random: Random) =
        shouldAskPerceived(current, earlier + current, lastAskedAt, week, random)

    @Test
    fun `1·2번째 세션은 3분의 1 확률에 걸릴 때만 묻는다`() {
        val first = session("2026-09-28T10:00")
        val second = session("2026-09-29T10:00")

        assertTrue(ask(first, random = HIT))
        assertFalse(ask(first, random = MISS))
        assertTrue(ask(second, listOf(first), random = HIT))
        assertFalse(ask(second, listOf(first), random = MISS))
    }

    @Test
    fun `3번째 세션까지 걸리지 않았으면 확정으로 묻는다`() {
        val earlier = listOf(session("2026-09-28T10:00"), session("2026-09-29T10:00"))

        assertTrue(ask(session("2026-09-30T10:00"), earlier, random = MISS))
    }

    @Test
    fun `답한 주는 다시 묻지 않는다`() {
        val earlier = listOf(session("2026-09-28T10:00", perceivedMin = 40), session("2026-09-29T10:00"))

        assertFalse(ask(session("2026-09-30T10:00"), earlier, random = HIT))
    }

    @Test
    fun `건너뛴 주도 다시 묻지 않는다`() {
        val skipped = session("2026-09-28T10:00")
        val earlier = listOf(skipped, session("2026-09-29T10:00"))

        assertFalse(ask(session("2026-09-30T10:00"), earlier, lastAskedAt = skipped.startAt, random = HIT))
    }

    @Test
    fun `지난주에 물었으면 이번 주는 다시 후보다`() {
        val lastWeek = at("2026-09-27T23:00")

        assertTrue(ask(session("2026-09-28T10:00"), lastAskedAt = lastWeek, random = HIT))
    }

    @Test
    fun `통계 제외 세션과 타임라인 없는 세션에서는 묻지 않는다`() {
        assertFalse(ask(session("2026-09-28T10:00", valid = false), random = HIT))
        assertFalse(ask(session("2026-09-28T10:00", hasTimeline = false), random = HIT))
    }

    @Test
    fun `3번째를 셀 때 통계 제외 세션과 타임라인 없는 세션은 빼고 센다`() {
        val earlier = listOf(
            session("2026-09-28T10:00"),
            session("2026-09-28T11:00", valid = false),
            session("2026-09-29T10:00", hasTimeline = false),
        )

        // 후보로는 2번째 세션이라 확률에 맡긴다
        assertFalse(ask(session("2026-09-30T10:00"), earlier, random = MISS))
    }
}
