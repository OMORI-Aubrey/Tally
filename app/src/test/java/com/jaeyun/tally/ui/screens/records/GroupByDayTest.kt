package com.jaeyun.tally.ui.screens.records

import com.jaeyun.tally.data.room.StudySession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class GroupByDayTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    private fun session(id: Long, start: String, subject: String? = null, sec: Int = 3600, valid: Boolean = true): StudySession {
        val startAt = LocalDateTime.parse(start).atZone(seoul).toInstant().toEpochMilli()
        return StudySession(
            id = id, subjectName = subject, startAt = startAt, endAt = startAt + sec * 1000L,
            tTotalSec = sec, isValidForStats = valid, createdAt = startAt,
        )
    }

    @Test
    fun `로컬 날짜로 묶고 최근 순서를 지킨다`() {
        val days = groupByDay(
            listOf(
                session(3, "2026-10-02T19:00", subject = "수학"),
                session(2, "2026-10-02T09:30"),
                session(1, "2026-10-01T21:00", subject = "영어"),
            ),
            seoul,
        )

        assertEquals(listOf(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)), days.map { it.date })
        assertEquals(listOf(3L, 2L), days[0].sessions.map { it.id })
        assertEquals(LocalTime.of(19, 0), days[0].sessions[0].startTime)
        assertEquals("수학", days[0].sessions[0].subjectName)
        assertEquals(null, days[0].sessions[1].subjectName)
    }

    @Test
    fun `자정을 넘긴 세션은 시작한 날에 들어간다`() {
        val days = groupByDay(listOf(session(1, "2026-10-01T23:30", sec = 2 * 3600)), seoul)

        assertEquals(LocalDate.of(2026, 10, 1), days.single().date)
    }

    @Test
    fun `통계 포함 여부와 길이를 그대로 옮긴다`() {
        val row = groupByDay(listOf(session(1, "2026-10-02T10:00", sec = 90, valid = false)), seoul).single().sessions.single()

        assertEquals(90, row.sessionSec)
        assertEquals(false, row.validForStats)
    }
}
