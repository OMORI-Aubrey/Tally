package com.jaeyun.tally.probe

import android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
import android.app.usage.UsageEvents.Event.KEYGUARD_HIDDEN
import android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE
import android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenOnAnalyzerTest {

    private fun event(timestamp: Long, type: Int, pkg: String = "android") = ProbeEvent(timestamp, type, pkg)

    @Test
    fun `화면 켜짐 후 앱 진입은 잠금해제 소요 시간으로 잡힌다`() {
        val samples = ScreenOnAnalyzer.analyze(
            listOf(
                event(1_000, SCREEN_INTERACTIVE),
                event(2_500, KEYGUARD_HIDDEN),
                event(3_200, ACTIVITY_RESUMED, "com.jaeyun.tally"),
            ),
            windowEnd = 10_000,
        )

        val sample = samples.single()
        assertEquals(ScreenOnOutcome.APP_RESUMED, sample.outcome)
        assertEquals(2_200, sample.durationMs)
        assertEquals(1_500L, sample.keyguardHiddenMs)
        assertEquals("com.jaeyun.tally", sample.resumedPackage)
    }

    @Test
    fun `앱 없이 화면이 꺼지면 SCREEN_OFF 샘플이 된다`() {
        val sample = ScreenOnAnalyzer.analyze(
            listOf(
                event(1_000, SCREEN_INTERACTIVE),
                event(31_000, SCREEN_NON_INTERACTIVE),
            ),
            windowEnd = 60_000,
        ).single()

        assertEquals(ScreenOnOutcome.SCREEN_OFF, sample.outcome)
        assertEquals(30_000, sample.durationMs)
        assertNull(sample.keyguardHiddenMs)
    }

    @Test
    fun `다음 이벤트가 없으면 조회 끝까지를 길이로 한다`() {
        val sample = ScreenOnAnalyzer.analyze(
            listOf(event(1_000, SCREEN_INTERACTIVE)),
            windowEnd = 4_000,
        ).single()

        assertEquals(ScreenOnOutcome.NO_EVENT, sample.outcome)
        assertEquals(3_000, sample.durationMs)
    }

    @Test
    fun `정렬되지 않은 입력도 시각순으로 분석한다`() {
        val sample = ScreenOnAnalyzer.analyze(
            listOf(
                event(3_000, ACTIVITY_RESUMED, "app"),
                event(1_000, SCREEN_INTERACTIVE),
            ),
            windowEnd = 10_000,
        ).single()

        assertEquals(ScreenOnOutcome.APP_RESUMED, sample.outcome)
        assertEquals(2_000, sample.durationMs)
    }

    @Test
    fun `현행 규칙 통계는 앱 진입 샘플로만 계산하고 5초·10초 경계를 포함한다`() {
        val samples = ScreenOnAnalyzer.analyze(
            listOf(
                event(0, SCREEN_INTERACTIVE), event(2_000, ACTIVITY_RESUMED, "a"),
                event(10_000, SCREEN_INTERACTIVE), event(15_000, ACTIVITY_RESUMED, "a"),
                event(20_000, SCREEN_INTERACTIVE), event(30_000, ACTIVITY_RESUMED, "a"),
                event(40_000, SCREEN_INTERACTIVE), event(41_000, ACTIVITY_RESUMED, "a"),
                event(50_000, SCREEN_INTERACTIVE), event(90_000, SCREEN_NON_INTERACTIVE),
            ),
            windowEnd = 100_000,
        )

        val stats = ScreenOnAnalyzer.stats(samples).current
        assertEquals(4, stats.count)
        // 1s, 2s, 5s, 10s → 중앙값 (2s + 5s) / 2
        assertEquals(3_500L, stats.medianMs)
        assertEquals(10_000L, stats.maxMs)
        assertEquals(2, stats.atLeast5sCount)
        assertEquals(1, stats.atLeast10sCount)
        assertEquals(1, ScreenOnAnalyzer.stats(samples).screenOffCount)
    }

    /** 실기기(One UI) 10:27:06 기록. 런처가 화면 켜짐 직전에 RESUMED되고, 해제 후 홈에서 19초 머문 뒤 설정 진입 */
    @Test
    fun `런처가 화면 켜짐보다 먼저 RESUMED되면 현행 규칙은 홈 체류까지 구간에 넣는다`() {
        val sample = ScreenOnAnalyzer.analyze(
            listOf(
                event(0, SCREEN_NON_INTERACTIVE),
                event(36_522, ACTIVITY_RESUMED, "launcher"),
                event(36_548, SCREEN_INTERACTIVE),
                event(36_712, KEYGUARD_HIDDEN),
                event(55_848, ACTIVITY_RESUMED, "com.android.settings"),
            ),
            windowEnd = 60_000,
        ).single()

        assertEquals(19_300L, sample.durationMs)
        assertEquals(164L, sample.keyguardAwareMs)
        assertEquals(true, sample.isUnlock)
    }

    @Test
    fun `앱 RESUMED가 잠금 해제보다 먼저 오면 두 규칙의 길이가 같다`() {
        val sample = ScreenOnAnalyzer.analyze(
            listOf(
                event(0, SCREEN_INTERACTIVE),
                event(1_240, ACTIVITY_RESUMED, "com.jaeyun.tally"),
                event(1_301, KEYGUARD_HIDDEN),
            ),
            windowEnd = 10_000,
        ).single()

        assertNull(sample.keyguardHiddenMs)
        assertEquals(1_240L, sample.keyguardAwareMs)
    }

    @Test
    fun `알림만 보고 끄면 잠금 해제 통계에 들어가지 않는다`() {
        val stats = ScreenOnAnalyzer.stats(
            ScreenOnAnalyzer.analyze(
                listOf(event(0, SCREEN_INTERACTIVE), event(20_000, SCREEN_NON_INTERACTIVE)),
                windowEnd = 30_000,
            ),
        )

        assertEquals(0, stats.keyguardAware.count)
        assertEquals(1, stats.screenOffCount)
    }

    @Test
    fun `샘플이 없으면 중앙값과 최대값이 없다`() {
        val stats = ScreenOnAnalyzer.stats(emptyList())
        assertEquals(0, stats.current.count)
        assertNull(stats.current.medianMs)
        assertNull(stats.keyguardAware.maxMs)
    }
}
