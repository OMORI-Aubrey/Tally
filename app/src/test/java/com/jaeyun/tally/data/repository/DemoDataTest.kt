package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.countsInStats
import com.jaeyun.tally.domain.metrics.MetricsCalculator
import com.jaeyun.tally.domain.model.PseudoPackage
import com.jaeyun.tally.util.weekRangeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DemoDataTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val self = "com.jaeyun.tally"

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, seoul).toInstant().toEpochMilli()

    // 2026-10-09 (금) 18:00. 이번 주는 10/5(월)부터
    private val now = at(2026, 10, 9, 18)
    private val thisWeek = weekRangeOf(now, seoul)
    private val fourWeeksAgo = at(2026, 9, 7, 0)

    private val demo = demoData(now, seoul, self)
    private val sessions = demo.sessions.map { it.session }

    @Test
    fun `세션은 모두 끝났고 이번 주와 앞 4주 안에 있다`() {
        assertTrue(sessions.isNotEmpty())
        sessions.forEach {
            assertTrue(it.startAt >= fourWeeksAgo)
            assertTrue(it.endAt!! <= now)
        }
        assertTrue(sessions.any { it.startAt in thisWeek })
    }

    @Test
    fun `구간이 빈틈과 겹침 없이 세션 전체를 덮는다`() {
        demo.sessions.forEach { (session, segments) ->
            assertEquals(session.startAt, segments.first().startAt)
            assertEquals(session.endAt, segments.last().endAt)
            segments.zipWithNext().forEach { (a, b) ->
                assertEquals(a.endAt, b.startAt)
                assertTrue(a.category != b.category)
            }
            segments.forEach { assertTrue(it.endAt > it.startAt) }
        }
    }

    @Test
    fun `앞 4주만으로 기준선 조건(12세션)을, 전체로 누적 섹션 조건(15세션)을 채운다`() {
        val previous = sessions.filter { it.startAt < thisWeek.startAt && it.countsInStats }
        assertTrue(previous.size >= 12)
        assertTrue(sessions.count { it.countsInStats } >= 15)
    }

    @Test
    fun `지표는 저장할 구간으로 계산한 값이다`() {
        demo.sessions.forEach { (session, segments) ->
            val metrics = MetricsCalculator.calculate(
                segments.map { it.toSegment() },
                session.startAt,
                session.endAt!!,
            )
            assertEquals(metrics.tTotalSec, session.tTotalSec)
            assertEquals(metrics.tFocusSec, session.tFocusSec)
            assertEquals(metrics.lfsSec, session.lfsSec)
            assertEquals(metrics.interruptionCount, session.interruptionCount)
            assertTrue(session.hasTimeline)
        }
    }

    @Test
    fun `3분이 안 되는 오조작 세션이 하나 있고 통계에서 빠진다`() {
        assertEquals(1, sessions.count { !it.isValidForStats })
        assertTrue(sessions.single { !it.isValidForStats }.tTotalSec < 180)
    }

    @Test
    fun `힘든 주는 이탈이 가장 잦고 이번 주는 가장 적다`() {
        val byWeek = sessions.filter { it.countsInStats }.groupBy { weekRangeOf(it.startAt, seoul).startAt }.toSortedMap()
            .values.map { week -> week.sumOf { it.interruptionCount }.toDouble() / week.size }
        assertEquals(5, byWeek.size)
        assertEquals(byWeek.max(), byWeek[2], 0.0)
        assertEquals(byWeek.min(), byWeek[4], 0.0)
    }

    @Test
    fun `체감은 앞 4주에 주마다 한 번이고 세션 길이를 넘지 않는다`() {
        val perceived = sessions.filter { it.perceivedFocusMin != null }
        assertEquals(4, perceived.size)
        assertEquals(4, perceived.map { weekRangeOf(it.startAt, seoul) }.distinct().size)
        perceived.forEach { assertTrue(it.perceivedFocusMin!! <= it.tTotalSec / 60) }
    }

    @Test
    fun `이번 주에 아직 끝나지 않은 세션은 만들지 않는다`() {
        // 10/5(월) 10:00 — 이번 주 첫 세션(09:30~11:00)이 진행 중이다
        val monday = at(2026, 10, 5, 10)
        val early = demoData(monday, seoul, self).sessions.map { it.session }

        assertTrue(early.none { it.startAt >= weekRangeOf(monday, seoul).startAt })
        assertTrue(early.count { it.countsInStats } >= 12)
    }

    @Test
    fun `같은 시각이면 같은 데이터가 나온다`() {
        assertEquals(demo, demoData(now, seoul, self))
    }

    @Test
    fun `구간에 나온 앱은 모두 분류가 있다`() {
        val apps = demo.sessions.flatMap { it.segments }.map { it.packageName }
            .filter { it != self && it != PseudoPackage.SCREEN_OFF }.toSet()
        assertEquals(apps, demo.classifications.map { it.packageName }.toSet())
    }
}
