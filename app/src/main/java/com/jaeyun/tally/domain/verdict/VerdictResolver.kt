package com.jaeyun.tally.domain.verdict

import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.Verdict
import com.jaeyun.tally.domain.model.VerdictResult

/**
 * 주간 판정 (§7.2). 이번 주 집중 밀도를 기준선과 비교한다. 종합 점수·가중치를 쓰지 않는다(§7.1). Android 의존성 없는 순수 함수다.
 *
 * - 편차 = `(이번주_D − 기준선_D) / 기준선_D × 100`
 * - 5구간 경계: ±[STRONG_PCT], ±[MILD_PCT]. 경계값은 바깥(더 강한) 구간에 넣는다 (PLAN.md §4)
 * - 총량 가드: 이번 주 순공 총량이 기준선 주간 순공 총량 × [TOTAL_GUARD_RATIO]보다 적으면 판정을 [Verdict.STEADY] 이하로
 *   내린다. 단방향이라 총량이 늘어난 주에 판정을 올리지 않는다 (§7.2.1)
 *
 * 임계값은 모두 이론적 근거가 없는 초기 설정값이며 §14.3 인터뷰로 조정한다.
 */
object VerdictResolver {
    const val STRONG_PCT = 10f
    const val MILD_PCT = 3f
    const val TOTAL_GUARD_RATIO = 0.7f

    /**
     * 이번 주에 통계 세션이 있고 기준선이 활성일 때 부른다.
     *
     * @param thisWeekDensityPct 이번 주 집중 밀도. 기준선과 같이 기간 합 `Σ T_focus / Σ T_total × 100`
     * @param thisWeekFocusSec 이번 주 순공 총량 `Σ T_focus`
     */
    fun resolve(thisWeekDensityPct: Float, thisWeekFocusSec: Int, baseline: Baseline.Active): VerdictResult {
        val raw = verdictOf(deviationPct(thisWeekDensityPct, baseline.densityPct))
        val guarded = thisWeekFocusSec < baseline.weeklyFocusSec * TOTAL_GUARD_RATIO
        val verdict = if (guarded) minOf(raw, Verdict.STEADY) else raw
        return VerdictResult(verdict, clamped = verdict != raw)
    }

    /**
     * 기준선 대비 편차(%). 곱셈을 먼저 해 경계값(±3%, ±10%)이 소수 오차로 어긋나지 않게 한다.
     * 기준선 밀도가 0이면(직전 4주가 모두 딴짓) 이번 주가 0보다 크면 +∞, 같으면 0이다.
     */
    internal fun deviationPct(thisWeekDensityPct: Float, baselineDensityPct: Float): Float = when {
        baselineDensityPct > 0f -> (thisWeekDensityPct - baselineDensityPct) * 100f / baselineDensityPct
        thisWeekDensityPct > 0f -> Float.POSITIVE_INFINITY
        else -> 0f
    }

    internal fun verdictOf(deviationPct: Float): Verdict = when {
        deviationPct >= STRONG_PCT -> Verdict.BEST
        deviationPct >= MILD_PCT -> Verdict.BETTER
        deviationPct > -MILD_PCT -> Verdict.STEADY
        deviationPct > -STRONG_PCT -> Verdict.SLIPPED
        else -> Verdict.TOUGH
    }
}
