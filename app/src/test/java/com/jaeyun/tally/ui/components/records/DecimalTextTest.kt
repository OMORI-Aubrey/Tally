package com.jaeyun.tally.ui.components.records

import org.junit.Assert.assertEquals
import org.junit.Test

class DecimalTextTest {

    @Test
    fun `소수 첫째 자리에서 반올림한다`() {
        assertEquals("4.4", decimalText(4.4f))
        assertEquals("2.4", decimalText(2.375f))
        assertEquals("3.1", decimalText(3.125f))
    }

    @Test
    fun `정수면 소수점을 쓰지 않는다`() {
        assertEquals("3", decimalText(3f))
        assertEquals("3", decimalText(2.96f))
        assertEquals("12", decimalText(12f))
    }
}
