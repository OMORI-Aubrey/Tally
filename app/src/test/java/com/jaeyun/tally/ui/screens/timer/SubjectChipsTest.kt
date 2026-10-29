package com.jaeyun.tally.ui.screens.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectChipsTest {

    private val recent = listOf("수학", "영어", "국어", "한국사", "물리", "화학")

    @Test
    fun `최근 과목 5개까지`() {
        assertEquals(listOf("수학", "영어", "국어", "한국사", "물리"), subjectChips(recent, selected = null))
    }

    @Test
    fun `고른 과목이 5개 안에 있으면 순서 그대로`() {
        assertEquals(listOf("수학", "영어", "국어", "한국사", "물리"), subjectChips(recent, selected = "국어"))
    }

    @Test
    fun `고른 과목이 5개 밖이면 맨 앞에 넣고 4개만 더 보인다`() {
        assertEquals(listOf("화학", "수학", "영어", "국어", "한국사"), subjectChips(recent, selected = "화학"))
        assertEquals(listOf("새 과목", "수학", "영어", "국어", "한국사"), subjectChips(recent, selected = "새 과목"))
    }

    @Test
    fun `과목이 없으면 고른 과목만`() {
        assertEquals(emptyList<String>(), subjectChips(emptyList(), selected = null))
        assertEquals(listOf("수학"), subjectChips(emptyList(), selected = "수학"))
    }
}
