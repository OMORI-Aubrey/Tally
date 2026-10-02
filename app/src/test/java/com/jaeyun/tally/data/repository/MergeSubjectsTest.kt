package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.SubjectStat
import org.junit.Assert.assertEquals
import org.junit.Test

class MergeSubjectsTest {

    private val stats = listOf(
        SubjectStat("수학", sessionCount = 12, lastStartAt = 100),
        SubjectStat("영어", sessionCount = 8, lastStartAt = 300),
    )

    @Test
    fun `추가만 한 과목도 세션 0개로 들어간다`() {
        val merged = mergeSubjects(stats, added = mapOf("코틀린" to 200L), hidden = emptySet())

        assertEquals(
            listOf(SubjectSummary("수학", 12, false), SubjectSummary("영어", 8, false), SubjectSummary("코틀린", 0, false)),
            merged.forSheet(),
        )
    }

    @Test
    fun `칩은 마지막 세션과 추가 시각 중 늦은 쪽 순서`() {
        val merged = mergeSubjects(stats, added = mapOf("코틀린" to 200L, "수학" to 400L), hidden = emptySet())

        assertEquals(listOf("수학", "영어", "코틀린"), merged.recentVisible())
    }

    @Test
    fun `숨긴 과목은 칩에서만 빠진다`() {
        val merged = mergeSubjects(stats, added = emptyMap(), hidden = setOf("영어"))

        assertEquals(listOf("수학"), merged.recentVisible())
        assertEquals(SubjectSummary("영어", 8, hidden = true), merged.forSheet()[1])
    }

    @Test
    fun `시트는 세션 수가 같으면 최근 순`() {
        val merged = mergeSubjects(emptyList(), added = mapOf("가" to 1L, "나" to 2L), hidden = emptySet())

        assertEquals(listOf("나", "가"), merged.forSheet().map { it.name })
    }
}
