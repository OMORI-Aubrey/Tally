package com.jaeyun.tally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class JosaTest {

    @Test
    fun `을를은 받침으로 고른다`() {
        assertEquals("을", Josa.objectOf("수햑"))
        assertEquals("를", Josa.objectOf("영어"))
    }

    @Test
    fun `으로로는 ㄹ 받침 뒤에 로를 쓴다`() {
        assertEquals("으로", Josa.directionOf("수학"))
        assertEquals("로", Josa.directionOf("국어"))
        assertEquals("으로", Josa.directionOf("한국사 문법"))
        assertEquals("로", Josa.directionOf("물리"))
        assertEquals("로", Josa.directionOf("철"))
    }

    @Test
    fun `한글이 아니면 둘을 함께 쓴다`() {
        assertEquals("을(를)", Josa.objectOf("Java"))
        assertEquals("(으)로", Josa.directionOf("C++"))
        assertEquals("을(를)", Josa.objectOf(""))
    }
}
