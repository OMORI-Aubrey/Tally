package com.jaeyun.tally.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun session(
    totalMin: Int = 60,
    focusMin: Int = 45,
    lfsMin: Int = 20,
    interruptions: Int = 3,
    isValidForStats: Boolean = true,
    hasTimeline: Boolean = true,
) = SessionStats(
    startAt = 0L,
    isValidForStats = isValidForStats,
    hasTimeline = hasTimeline,
    tTotalSec = totalMin * 60,
    tFocusSec = focusMin * 60,
    lfsSec = lfsMin * 60,
    interruptionCount = interruptions,
)

class WeekStatsTest {

    @Test
    fun `세션당 이탈과 평균 LFS·세션 길이는 세션 수로 나누고 순공은 합이다`() {
        val week = WeekStats.of(
            listOf(
                session(totalMin = 30, focusMin = 20, lfsMin = 10, interruptions = 1),
                session(totalMin = 91, focusMin = 70, lfsMin = 31, interruptions = 4),
            ),
        )

        assertEquals(WeekStats(2.5f, (10 + 31) * 60 / 2, (20 + 70) * 60, (30 + 91) * 60 / 2), week)
    }

    @Test
    fun `통계 제외 세션과 타임라인 없는 세션은 빠진다`() {
        val week = WeekStats.of(
            listOf(
                session(interruptions = 2),
                session(interruptions = 10, isValidForStats = false),
                session(interruptions = 10, hasTimeline = false),
            ),
        )

        assertEquals(2f, week?.interruptionsPerSession)
        assertEquals(45 * 60, week?.focusSec)
    }

    @Test
    fun `통계에 셀 세션이 없으면 null`() {
        assertNull(WeekStats.of(emptyList()))
        assertNull(WeekStats.of(listOf(session(isValidForStats = false), session(hasTimeline = false))))
    }
}
