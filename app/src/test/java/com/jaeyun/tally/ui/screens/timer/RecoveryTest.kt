package com.jaeyun.tally.ui.screens.timer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN = 60_000L

class RecoveryTest {

    /** 19:00에 시작한 세션 */
    private val startAt = 1_000 * MIN

    @Test
    fun `세션을 시작한 뒤 재부팅했으면 묻는다`() {
        assertTrue(asksRecovery(startAt, bootAt = startAt + 30 * MIN, answeredAt = null))
    }

    @Test
    fun `재부팅이 세션보다 먼저면 묻지 않는다 (강제 종료·최근 앱에서 지움)`() {
        assertFalse(asksRecovery(startAt, bootAt = startAt - 5 * MIN, answeredAt = null))
    }

    @Test
    fun `이미 답한 재부팅은 다시 묻지 않고 그 뒤에 또 재부팅하면 묻는다`() {
        val bootAt = startAt + 30 * MIN
        assertFalse(asksRecovery(startAt, bootAt, answeredAt = bootAt + 2 * MIN))
        assertTrue(asksRecovery(startAt, bootAt = bootAt + 60 * MIN, answeredAt = bootAt + 2 * MIN))
    }

    @Test
    fun `예전 세션에 답한 시각은 새 세션의 재부팅을 막지 않는다`() {
        assertTrue(asksRecovery(startAt, bootAt = startAt + 30 * MIN, answeredAt = startAt - 60 * MIN))
    }
}
