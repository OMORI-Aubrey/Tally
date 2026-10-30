package com.jaeyun.tally.domain.verdict

import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.Verdict
import com.jaeyun.tally.domain.model.Verdict.BEST
import com.jaeyun.tally.domain.model.Verdict.BETTER
import com.jaeyun.tally.domain.model.Verdict.SLIPPED
import com.jaeyun.tally.domain.model.Verdict.STEADY
import com.jaeyun.tally.domain.model.Verdict.TOUGH
import com.jaeyun.tally.domain.model.VerdictResult
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

private const val HOUR = 3600

/** 기준선 집중 밀도 50%, 주간 순공 10시간 */
private val BASELINE = Baseline.Active(
    densityPct = 50f,
    interruptionsPerSession = 3f,
    lfsSec = 30 * 60,
    weeklyFocusSec = 10 * HOUR,
    sessionSec = 60 * 60,
    sessionCount = 12,
)

/** 기준선 대비 편차 [deviationPct]%인 이번 주. 순공 총량은 기준선의 [focusRatio]배 */
private fun resolve(deviationPct: Float, focusRatio: Float = 1f): VerdictResult = VerdictResolver.resolve(
    thisWeekDensityPct = BASELINE.densityPct * (100f + deviationPct) / 100f,
    thisWeekFocusSec = (BASELINE.weeklyFocusSec * focusRatio).roundToInt(),
    baseline = BASELINE,
)

private fun verdictAt(deviationPct: Float): Verdict = resolve(deviationPct).verdict

class VerdictResolverTest {

    // §14.2 판정 케이스

    @Test
    fun `편차 +12%는 가장 잘한 주`() {
        assertEquals(VerdictResult(BEST, clamped = false), resolve(12f))
    }

    @Test
    fun `편차 −2%는 평소 페이스 유지`() {
        assertEquals(VerdictResult(STEADY, clamped = false), resolve(-2f))
    }

    @Test
    fun `편차 +12%, 순공 총량 60%면 총량 가드가 유지로 내린다`() {
        assertEquals(VerdictResult(STEADY, clamped = true), resolve(12f, focusRatio = 0.6f))
    }

    @Test
    fun `편차 +12%, 순공 총량 85%면 가드가 걸리지 않는다`() {
        assertEquals(VerdictResult(BEST, clamped = false), resolve(12f, focusRatio = 0.85f))
    }

    // 5구간 경계값: 경계는 바깥 구간에 넣는다 (PLAN.md §4)

    @Test
    fun `+10%는 가장 잘한 주, 바로 아래는 긍정`() {
        assertEquals(BEST, verdictAt(10f))
        assertEquals(BETTER, verdictAt(9.9f))
    }

    @Test
    fun `+3%는 긍정, 바로 아래는 유지`() {
        assertEquals(BETTER, verdictAt(3f))
        assertEquals(STEADY, verdictAt(2.9f))
    }

    @Test
    fun `−3%는 딴짓이 조금 늘어난 주, 바로 위는 유지`() {
        assertEquals(SLIPPED, verdictAt(-3f))
        assertEquals(STEADY, verdictAt(-2.9f))
    }

    @Test
    fun `−10%는 힘든 주, 바로 위는 딴짓이 조금 늘어난 주`() {
        assertEquals(TOUGH, verdictAt(-10f))
        assertEquals(SLIPPED, verdictAt(-9.9f))
    }

    @Test
    fun `편차는 곱셈을 먼저 해 경계값에서 소수 오차가 없다`() {
        assertEquals(3f, VerdictResolver.deviationPct(51.5f, 50f))
        assertEquals(-10f, VerdictResolver.deviationPct(63f, 70f))
    }

    // 총량 가드: 0.7 경계, 단방향

    @Test
    fun `순공 총량이 정확히 70%면 가드가 걸리지 않는다`() {
        assertEquals(VerdictResult(BEST, clamped = false), resolve(12f, focusRatio = 0.7f))
    }

    @Test
    fun `순공 총량이 70%에서 1초 모자라면 가드가 걸린다`() {
        val result = VerdictResolver.resolve(
            thisWeekDensityPct = 56f,
            thisWeekFocusSec = 7 * HOUR - 1,
            baseline = BASELINE,
        )

        assertEquals(VerdictResult(STEADY, clamped = true), result)
    }

    @Test
    fun `긍정 판정도 가드가 걸리면 유지로 내린다`() {
        assertEquals(VerdictResult(STEADY, clamped = true), resolve(5f, focusRatio = 0.5f))
    }

    @Test
    fun `원래 유지 이하인 주는 가드가 걸려도 판정이 그대로이고 클램프가 아니다`() {
        assertEquals(VerdictResult(STEADY, clamped = false), resolve(0f, focusRatio = 0.5f))
        assertEquals(VerdictResult(SLIPPED, clamped = false), resolve(-5f, focusRatio = 0.5f))
        assertEquals(VerdictResult(TOUGH, clamped = false), resolve(-20f, focusRatio = 0.5f))
    }

    @Test
    fun `총량이 늘어난 주에도 판정을 올리지 않는다`() {
        assertEquals(VerdictResult(STEADY, clamped = false), resolve(0f, focusRatio = 3f))
        assertEquals(VerdictResult(TOUGH, clamped = false), resolve(-20f, focusRatio = 3f))
    }

    // 기준선 밀도 0

    @Test
    fun `기준선 밀도가 0이면 이번 주가 0보다 클 때 가장 잘한 주, 0이면 유지`() {
        val zero = BASELINE.copy(densityPct = 0f)

        assertEquals(BEST, VerdictResolver.resolve(10f, 10 * HOUR, zero).verdict)
        assertEquals(STEADY, VerdictResolver.resolve(0f, 10 * HOUR, zero).verdict)
    }
}
