package com.jaeyun.tally.ui.screens.timer

import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.ui.components.WeeklySummary
import com.jaeyun.tally.util.weekRangeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WeeklySummaryTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    /** 2026-09-28(월) ~ 10-05(월) 00:00 */
    private val week = weekRangeOf(at("2026-10-02T12:00"), seoul)

    private fun at(time: String) = LocalDateTime.parse(time).atZone(seoul).toInstant().toEpochMilli()

    private fun session(
        start: String,
        lfsMin: Int,
        valid: Boolean = true,
        hasTimeline: Boolean = true,
    ) = StudySession(
        subjectName = null,
        startAt = at(start),
        endAt = at(start) + 60 * 60_000L,
        lfsSec = lfsMin * 60,
        isValidForStats = valid,
        hasTimeline = hasTimeline,
        createdAt = at(start),
    )

    @Test
    fun `이번 주 세션의 최장 구간 최댓값과 평균`() {
        val summary = weeklySummary(
            listOf(session("2026-10-02T19:00", lfsMin = 47), session("2026-09-29T09:00", lfsMin = 20), session("2026-09-28T00:00", lfsMin = 17)),
            week,
        )

        assertEquals(WeeklySummary(longestSec = 47 * 60, averageSec = 28 * 60), summary)
    }

    @Test
    fun `평균은 초 단위에서 버린다`() {
        val summary = weeklySummary(listOf(session("2026-10-01T10:00", lfsMin = 10), session("2026-10-02T10:00", lfsMin = 0)), week)

        // (600 + 0) / 2 = 300초
        assertEquals(5 * 60, summary?.averageSec)
        val odd = weeklySummary(
            listOf(session("2026-10-01T10:00", lfsMin = 1), session("2026-10-01T11:00", lfsMin = 1), session("2026-10-02T10:00", lfsMin = 0)),
            week,
        )
        // 120 / 3 = 40초
        assertEquals(40, odd?.averageSec)
    }

    @Test
    fun `월요일 0시에 시작한 세션은 이번 주, 그 직전은 지난주`() {
        val summary = weeklySummary(
            listOf(session("2026-09-28T00:00", lfsMin = 10), session("2026-09-27T23:59", lfsMin = 90), session("2026-10-05T00:00", lfsMin = 80)),
            week,
        )

        assertEquals(WeeklySummary(longestSec = 10 * 60, averageSec = 10 * 60), summary)
    }

    @Test
    fun `통계 제외 세션과 타임라인 없는 세션은 세지 않는다`() {
        val summary = weeklySummary(
            listOf(
                session("2026-10-01T10:00", lfsMin = 30),
                session("2026-10-01T11:00", lfsMin = 2, valid = false),
                session("2026-10-01T12:00", lfsMin = 0, hasTimeline = false),
            ),
            week,
        )

        assertEquals(WeeklySummary(longestSec = 30 * 60, averageSec = 30 * 60), summary)
    }

    @Test
    fun `이번 주 세션이 없으면 null`() {
        assertNull(weeklySummary(emptyList(), week))
        assertNull(weeklySummary(listOf(session("2026-09-27T10:00", lfsMin = 30)), week))
    }

    @Test
    fun `셀 세션이 없으면 null`() {
        val summary = weeklySummary(
            listOf(session("2026-10-01T11:00", lfsMin = 2, valid = false), session("2026-10-01T12:00", lfsMin = 0, hasTimeline = false)),
            week,
        )

        assertNull(summary)
    }

    @Test
    fun `최장 구간이 모두 0초면 0을 보이지 않도록 null`() {
        assertNull(weeklySummary(listOf(session("2026-10-01T10:00", lfsMin = 0)), week))
    }
}
