package com.jaeyun.tally.util

import com.jaeyun.tally.util.DurationParts.Hours
import com.jaeyun.tally.util.DurationParts.HoursMinutes
import com.jaeyun.tally.util.DurationParts.Minutes
import com.jaeyun.tally.util.DurationParts.Seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTest {

    @Test
    fun `1분 미만은 초`() {
        assertEquals(Seconds(0), durationParts(0))
        assertEquals(Seconds(40), durationParts(40))
        assertEquals(Seconds(59), durationParts(59))
    }

    @Test
    fun `1시간 미만은 분, 남는 초는 버린다`() {
        assertEquals(Minutes(1), durationParts(60))
        assertEquals(Minutes(25), durationParts(25 * 60 + 59))
        assertEquals(Minutes(59), durationParts(3599))
    }

    @Test
    fun `1시간 이상은 시간과 분, 분이 0이면 시간만`() {
        assertEquals(Hours(1), durationParts(3600))
        assertEquals(Hours(2), durationParts(2 * 3600 + 59))
        assertEquals(HoursMinutes(1, 28), durationParts(3600 + 28 * 60))
        assertEquals(HoursMinutes(21, 24), durationParts(21 * 3600 + 24 * 60 + 30))
    }

    @Test
    fun `음수는 0초`() {
        assertEquals(Seconds(0), durationParts(-5))
    }
}
