package com.jaeyun.tally.domain.metrics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionValidityTest {

    @Test
    fun `3분 미만은 통계에서 뺀다`() {
        assertFalse(SessionValidity.isValidForStats(0))
        assertFalse(SessionValidity.isValidForStats(3 * 60 - 1))
    }

    @Test
    fun `3분과 180분 경계는 포함한다`() {
        assertTrue(SessionValidity.isValidForStats(3 * 60))
        assertTrue(SessionValidity.isValidForStats(180 * 60))
    }

    @Test
    fun `180분 초과는 통계에서 뺀다`() {
        assertFalse(SessionValidity.isValidForStats(180 * 60 + 1))
    }
}
