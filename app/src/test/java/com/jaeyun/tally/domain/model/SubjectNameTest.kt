package com.jaeyun.tally.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubjectNameTest {

    @Test
    fun `앞뒤 공백을 지우고 연속 공백을 하나로`() {
        assertEquals("한국사 문법", SubjectName.normalize("  한국사   문법 "))
        assertEquals("수학 과외", SubjectName.normalize("수학\t\n과외"))
    }

    @Test
    fun `비었거나 공백뿐이면 null`() {
        assertNull(SubjectName.normalize(""))
        assertNull(SubjectName.normalize("   "))
    }

    @Test
    fun `최대 길이에서 자르고 끝 공백을 지운다`() {
        val long = "가".repeat(19) + " 나다"
        assertEquals("가".repeat(19), SubjectName.normalize(long))
        assertEquals(SubjectName.MAX_LENGTH, SubjectName.normalize("가".repeat(30))!!.length)
    }
}
