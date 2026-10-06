package com.jaeyun.tally.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SketchSliderTest {

    private val hours = 1..12

    @Test
    fun `양 끝은 범위의 처음과 끝 값이다`() {
        assertEquals(1, sliderValueAt(0f, hours))
        assertEquals(12, sliderValueAt(1f, hours))
    }

    @Test
    fun `가장 가까운 눈금으로 맞춘다`() {
        // 눈금 11칸. 0.3 × 11 = 3.3 → 4번째 눈금(4시간), 0.35 × 11 = 3.85 → 5시간
        assertEquals(4, sliderValueAt(0.3f, hours))
        assertEquals(5, sliderValueAt(0.35f, hours))
    }

    @Test
    fun `트랙 밖을 눌러도 양 끝 값이다`() {
        assertEquals(1, sliderValueAt(-0.2f, hours))
        assertEquals(12, sliderValueAt(1.4f, hours))
    }
}
