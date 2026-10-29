package com.jaeyun.tally.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WeekRangeTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    private fun at(text: String, zone: ZoneId = seoul): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    // 2026-09-28은 월요일, 2026-10-04는 일요일
    private val week = WeekRange(at("2026-09-28T00:00"), at("2026-10-05T00:00"))

    @Test
    fun `월요일 00시 정각은 그 주의 시작`() {
        assertEquals(week, weekRangeOf(at("2026-09-28T00:00"), seoul))
    }

    @Test
    fun `일요일 23시 59분 59초는 아직 같은 주`() {
        val lastMoment = at("2026-10-04T23:59:59.999")
        assertEquals(week, weekRangeOf(lastMoment, seoul))
        assertTrue(lastMoment in week)
    }

    @Test
    fun `다음 월요일 00시는 다음 주이고 끝 경계는 포함하지 않는다`() {
        val nextMonday = at("2026-10-05T00:00")
        assertEquals(WeekRange(at("2026-10-05T00:00"), at("2026-10-12T00:00")), weekRangeOf(nextMonday, seoul))
        assertFalse(nextMonday in week)
    }

    @Test
    fun `주 중간의 자정도 같은 주`() {
        assertEquals(week, weekRangeOf(at("2026-10-01T00:00"), seoul))
        assertEquals(week, weekRangeOf(at("2026-09-30T23:59:59"), seoul))
    }

    @Test
    fun `같은 순간도 시간대에 따라 다른 주가 된다`() {
        // 서울 월요일 08:00 = UTC 일요일 23:00
        val instant = at("2026-09-28T08:00")
        assertEquals(week, weekRangeOf(instant, seoul))
        assertEquals(at("2026-09-21T00:00", ZoneId.of("UTC")), weekRangeOf(instant, ZoneId.of("UTC")).startAt)
    }

    @Test
    fun `일광 절약 시간이 끝나는 주는 1시간 길다`() {
        val newYork = ZoneId.of("America/New_York")
        // 2026-11-01(일) 서머타임 종료
        val range = weekRangeOf(at("2026-10-29T12:00", newYork), newYork)
        assertEquals(at("2026-10-26T00:00", newYork), range.startAt)
        assertEquals((7 * 24 + 1) * 3_600_000L, range.endAt - range.startAt)
    }
}
