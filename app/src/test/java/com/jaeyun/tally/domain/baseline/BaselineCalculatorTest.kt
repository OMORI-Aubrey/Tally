package com.jaeyun.tally.domain.baseline

import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.SessionStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private val SEOUL = ZoneId.of("Asia/Seoul")
private const val DAY = 24 * 60 * 60_000L

/** 이번 주 월요일 00:00 (2026-10-26) */
private val THIS_WEEK = LocalDate.of(2026, 10, 26).atStartOfDay(SEOUL).toInstant().toEpochMilli()

/** 기준선 기간 첫 날 00:00 (4주 전 월요일, 2026-09-28) */
private val WINDOW_START = THIS_WEEK - 28 * DAY

private fun session(
    startAt: Long = THIS_WEEK - DAY,
    totalMin: Int = 60,
    focusMin: Int = 45,
    lfsMin: Int = 20,
    interruptions: Int = 3,
    isValidForStats: Boolean = true,
    hasTimeline: Boolean = true,
) = SessionStats(
    startAt = startAt,
    isValidForStats = isValidForStats,
    hasTimeline = hasTimeline,
    tTotalSec = totalMin * 60,
    tFocusSec = focusMin * 60,
    lfsSec = lfsMin * 60,
    interruptionCount = interruptions,
)

private fun List<SessionStats>.baseline(required: Int = BaselineCalculator.REQUIRED_SESSIONS): Baseline =
    BaselineCalculator.calculate(this, THIS_WEEK, SEOUL, required)

private fun Baseline.active(): Baseline.Active {
    assertTrue("기준선이 활성이어야 한다: $this", this is Baseline.Active)
    return this as Baseline.Active
}

class BaselineCalculatorTest {

    @Test
    fun `유효 세션 11개면 미활성이고 1개가 더 필요하다`() {
        assertEquals(Baseline.Inactive(neededSessions = 1), List(11) { session() }.baseline())
    }

    @Test
    fun `유효 세션 12개면 활성`() {
        assertEquals(12, List(12) { session() }.baseline().active().sessionCount)
    }

    @Test
    fun `세션이 없으면 12개가 필요하다`() {
        assertEquals(Baseline.Inactive(neededSessions = 12), emptyList<SessionStats>().baseline())
    }

    @Test
    fun `이번 주 세션은 기준선에 들어가지 않는다`() {
        val sessions = List(11) { session() } + session(startAt = THIS_WEEK) + session(startAt = THIS_WEEK + 3 * DAY)

        assertEquals(Baseline.Inactive(neededSessions = 1), sessions.baseline())
    }

    @Test
    fun `이번 주 시작 직전 세션은 지난주라 들어간다`() {
        val sessions = List(11) { session() } + session(startAt = THIS_WEEK - 1)

        assertEquals(12, sessions.baseline().active().sessionCount)
    }

    @Test
    fun `4주 전 월요일 00시부터 들어가고 그 전은 빠진다`() {
        val sessions = List(10) { session() } + session(startAt = WINDOW_START) + session(startAt = WINDOW_START - 1)

        assertEquals(Baseline.Inactive(neededSessions = 1), sessions.baseline())
    }

    @Test
    fun `통계 제외 세션과 타임라인 없는 세션은 빠지고 활성 조건에도 세지 않는다`() {
        val sessions = List(10) { session() } +
            session(isValidForStats = false) +
            session(hasTimeline = false)

        assertEquals(Baseline.Inactive(neededSessions = 2), sessions.baseline())
    }

    @Test
    fun `집중 밀도는 세션 평균이 아니라 기간 합으로 계산한다`() {
        // 2시간 50% 세션 1개 + 5분 100% 세션 11개. 세션 평균이면 95.8%, 기간 합이면 (60 + 55) / (120 + 55) = 65.7%
        val sessions = List(11) { session(totalMin = 5, focusMin = 5) } + session(totalMin = 120, focusMin = 60)

        assertEquals(115 * 100f / 175, sessions.baseline().active().densityPct, 0.001f)
    }

    @Test
    fun `세션당 이탈, 평균 LFS, 평균 세션 길이는 유효 세션 수로 나눈다`() {
        val sessions = List(6) { session(totalMin = 30, lfsMin = 10, interruptions = 1) } +
            List(6) { session(totalMin = 91, lfsMin = 31, interruptions = 4) }

        val baseline = sessions.baseline().active()

        assertEquals(2.5f, baseline.interruptionsPerSession, 0.0001f)
        assertEquals((10 + 31) * 60 / 2, baseline.lfsSec)
        assertEquals((30 + 91) * 60 / 2, baseline.sessionSec)
    }

    @Test
    fun `평균은 초에서 버린다`() {
        // LFS 65초 / 12 = 5.4초, 공부 시간 (11 × 3600 + 3665)초 / 12 = 3605.4초
        val sessions = List(11) { session(lfsMin = 0) } + session().copy(tTotalSec = 3665, lfsSec = 65)

        val baseline = sessions.baseline().active()

        assertEquals(5, baseline.lfsSec)
        assertEquals(3605, baseline.sessionSec)
    }

    @Test
    fun `주간 순공 총량은 4주 합을 4로 나눈다 — 세션이 한 주에 몰려 있어도 같다`() {
        val sessions = List(12) { session(startAt = THIS_WEEK - DAY, focusMin = 50) }

        assertEquals(12 * 50 * 60 / 4, sessions.baseline().active().weeklyFocusSec)
    }

    @Test
    fun `완화 플래그는 debug 빌드에서만 활성 조건을 6개로 낮춘다`() {
        assertEquals(6, BaselineCalculator.requiredSessions(relaxed = true, debugBuild = true))
        assertEquals(12, BaselineCalculator.requiredSessions(relaxed = true, debugBuild = false))
        assertEquals(12, BaselineCalculator.requiredSessions(relaxed = false, debugBuild = true))
        assertEquals(12, BaselineCalculator.requiredSessions(relaxed = false, debugBuild = false))
    }

    @Test
    fun `완화 조건에서는 유효 세션 5개 미활성, 6개 활성`() {
        val relaxed = BaselineCalculator.RELAXED_REQUIRED_SESSIONS

        assertEquals(Baseline.Inactive(neededSessions = 1), List(5) { session() }.baseline(relaxed))
        assertEquals(6, List(6) { session() }.baseline(relaxed).active().sessionCount)
    }

    @Test
    fun `일광 절약 시간이 바뀐 기간도 4주 전 월요일 00시가 경계다`() {
        // 뉴욕은 2026-11-01에 서머타임이 끝난다. 이번 주 월요일 2026-11-23, 4주 전 월요일 2026-10-26
        val zone = ZoneId.of("America/New_York")
        val thisWeek = LocalDate.of(2026, 11, 23).atStartOfDay(zone).toInstant().toEpochMilli()
        val windowStart = LocalDate.of(2026, 10, 26).atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = List(10) { session(startAt = thisWeek - DAY) } +
            session(startAt = windowStart) +
            session(startAt = windowStart - 1)

        assertEquals(
            Baseline.Inactive(neededSessions = 1),
            BaselineCalculator.calculate(sessions, thisWeek, zone, BaselineCalculator.REQUIRED_SESSIONS),
        )
    }
}
