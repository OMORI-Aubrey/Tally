package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.StudySession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class SessionCsvTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    // 2026-10-05 (월) 10:00:00 KST
    private val start = 1_791_162_000_000L

    private fun session(subject: String?, perceived: Int? = null) = StudySession(
        id = 7,
        subjectName = subject,
        startAt = start,
        endAt = start + 90 * 60_000L,
        perceivedFocusMin = perceived,
        tTotalSec = 5400,
        tFocusSec = 4800,
        tDistSec = 600,
        lfsSec = 2400,
        densityPct = 88.888f,
        interruptionCount = 3,
        metricsVersion = 1,
        isValidForStats = true,
        hasTimeline = true,
        createdAt = start,
    )

    private fun lines(csv: String) = csv.split("\r\n")

    @Test
    fun `머리글 다음에 세션을 한 줄씩 쓰고 줄 끝은 CRLF다`() {
        val csv = sessionsCsv(listOf(session("수학", perceived = 85)), seoul)

        assertEquals(
            listOf(
                "id,subjectName,startLocal,endLocal,startAt,endAt,tTotalSec,tFocusSec,tDistSec,lfsSec,densityPct," +
                    "interruptionCount,perceivedFocusMin,isValidForStats,hasTimeline,metricsVersion",
                "7,수학,2026-10-05 10:00:00,2026-10-05 11:30:00,$start,${start + 5_400_000},5400,4800,600,2400,88.89," +
                    "3,85,true,true,1",
                "",
            ),
            lines(csv),
        )
    }

    @Test
    fun `값이 없으면 빈칸이다`() {
        val row = lines(sessionsCsv(listOf(session(null).copy(endAt = null)), seoul))[1].split(",")

        assertEquals("", row[1])
        assertEquals("", row[3])
        assertEquals("", row[5])
        assertEquals("", row[12])
    }

    @Test
    fun `쉼표나 따옴표가 든 과목 이름은 따옴표로 감싼다`() {
        assertEquals("\"국어, 문학\"", csvField("국어, 문학"))
        assertEquals("\"\"\"기출\"\" 풀이\"", csvField("\"기출\" 풀이"))
        assertEquals("수학", csvField("수학"))
    }

    @Test
    fun `세션이 없으면 머리글만 쓴다`() {
        assertEquals(2, lines(sessionsCsv(emptyList(), seoul)).size)
    }
}
