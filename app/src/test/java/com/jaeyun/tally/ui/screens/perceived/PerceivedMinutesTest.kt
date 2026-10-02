package com.jaeyun.tally.ui.screens.perceived

import com.jaeyun.tally.data.room.StudySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerceivedMinutesTest {

    @Test
    fun `빈칸이면 아직 답이 없다`() {
        assertEquals(PerceivedMinutes.Empty, parsePerceivedMinutes("", "", maxMinutes = 30))
        assertEquals(PerceivedMinutes.Empty, parsePerceivedMinutes("", "", maxMinutes = 150))
    }

    @Test
    fun `1시간 이상 세션부터 시간 칸을 둔다`() {
        assertFalse(hasHourBlank(59))
        assertTrue(hasHourBlank(60))
    }

    @Test
    fun `시간 칸에 세션보다 긴 시간을 적으면 분 칸으로 넘어가지 않는다`() {
        // 2시간 1분 세션
        assertTrue(hourFitsSession("0", maxMinutes = 121))
        assertTrue(hourFitsSession("2", maxMinutes = 121))
        assertFalse(hourFitsSession("3", maxMinutes = 121))
        assertFalse(hourFitsSession("9", maxMinutes = 121))
        assertFalse(hourFitsSession("", maxMinutes = 121))
    }

    @Test
    fun `분 칸만 있으면 세션 길이까지 받는다`() {
        assertEquals(PerceivedMinutes.Valid(30), parsePerceivedMinutes("", "30", maxMinutes = 30))
        assertEquals(PerceivedMinutes.Valid(7), parsePerceivedMinutes("", "07", maxMinutes = 30))
        assertEquals(PerceivedMinutes.TooLong, parsePerceivedMinutes("", "31", maxMinutes = 30))
    }

    @Test
    fun `0분도 받는다`() {
        assertEquals(PerceivedMinutes.Valid(0), parsePerceivedMinutes("", "0", maxMinutes = 30))
        assertEquals(PerceivedMinutes.Valid(0), parsePerceivedMinutes("0", "", maxMinutes = 150))
    }

    @Test
    fun `시간과 분을 합친다`() {
        assertEquals(PerceivedMinutes.Valid(150), parsePerceivedMinutes("2", "30", maxMinutes = 150))
        assertEquals(PerceivedMinutes.Valid(45), parsePerceivedMinutes("0", "45", maxMinutes = 150))
    }

    @Test
    fun `한 칸만 적으면 다른 칸은 0이다`() {
        assertEquals(PerceivedMinutes.Valid(120), parsePerceivedMinutes("2", "", maxMinutes = 150))
        assertEquals(PerceivedMinutes.Valid(45), parsePerceivedMinutes("", "45", maxMinutes = 150))
    }

    @Test
    fun `2시간 30분 세션에 2시간 41분은 너무 길다`() {
        assertEquals(PerceivedMinutes.TooLong, parsePerceivedMinutes("2", "41", maxMinutes = 150))
        assertEquals(PerceivedMinutes.TooLong, parsePerceivedMinutes("3", "", maxMinutes = 150))
    }

    @Test
    fun `시간 칸이 있으면 분은 59까지다`() {
        assertEquals(PerceivedMinutes.MinutesOver59, parsePerceivedMinutes("1", "60", maxMinutes = 150))
        assertEquals(PerceivedMinutes.MinutesOver59, parsePerceivedMinutes("", "90", maxMinutes = 150))
        assertEquals(PerceivedMinutes.Valid(119), parsePerceivedMinutes("1", "59", maxMinutes = 150))
    }

    @Test
    fun `최대는 타이머 길이를 분으로 올린 값이다`() {
        fun session(lengthMillis: Long, tTotalSec: Int) =
            StudySession(subjectName = null, startAt = 0, endAt = lengthMillis, tTotalSec = tTotalSec, createdAt = 0)

        assertEquals(48, maxPerceivedMinutes(session(lengthMillis = (47 * 60 + 30) * 1000L, tTotalSec = 47 * 60 + 30)))
        assertEquals(47, maxPerceivedMinutes(session(lengthMillis = 47 * 60_000L, tTotalSec = 47 * 60)))
        // 자리 비움으로 착석이 줄어도 타이머가 보여준 길이까지 받는다
        assertEquals(120, maxPerceivedMinutes(session(lengthMillis = 120 * 60_000L, tTotalSec = 40 * 60)))
    }
}
