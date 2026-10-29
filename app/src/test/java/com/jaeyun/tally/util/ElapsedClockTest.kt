package com.jaeyun.tally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedClockTest {

    @Test
    fun `1시간 미만도 시를 00으로 채운다`() {
        assertEquals("00:00:00", formatElapsedClock(0))
        assertEquals("00:00:05", formatElapsedClock(5_999))
        assertEquals("00:59:59", formatElapsedClock(3_599_000))
    }

    @Test
    fun `시분초를 모두 두 자리로`() {
        assertEquals("01:00:00", formatElapsedClock(3_600_000))
        assertEquals("01:23:45", formatElapsedClock((3600 + 23 * 60 + 45) * 1000L))
        assertEquals("12:00:09", formatElapsedClock((12 * 3600 + 9) * 1000L))
    }

    @Test
    fun `음수는 0으로 본다`() {
        assertEquals("00:00:00", formatElapsedClock(-1_000))
    }
}
