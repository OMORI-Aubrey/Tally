package com.jaeyun.tally.ui.screens.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStepTest {

    @Test
    fun `처음 열었을 때 꺼져 있으면 순서 안내`() {
        assertEquals(PermissionStep.GUIDE, permissionStep(granted = false, requested = false))
    }

    @Test
    fun `설정에 다녀왔는데 꺼져 있으면 아직 꺼져 있다고 알린다`() {
        assertEquals(PermissionStep.STILL_OFF, permissionStep(granted = false, requested = true))
        assertFalse(finishesOnReturn(granted = false, requested = true))
    }

    @Test
    fun `처음부터 켜져 있으면 시작하기만 두고 저절로 넘어가지 않는다`() {
        assertEquals(PermissionStep.GRANTED, permissionStep(granted = true, requested = false))
        assertFalse(finishesOnReturn(granted = true, requested = false))
    }

    @Test
    fun `설정에 다녀와 켰으면 바로 끝낸다`() {
        assertTrue(finishesOnReturn(granted = true, requested = true))
    }

    @Test
    fun `고른 시간은 분으로 저장하고 고르지 않았으면 건너뛴다`() {
        assertEquals(180, selfReportedDailyMin(3))
        assertNull(selfReportedDailyMin(null))
    }
}
