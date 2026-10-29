package com.jaeyun.tally.domain.model

/** 개인 기준선 (§5.3). 저장하지 않고 조회할 때 계산한다(§9). 계산은 `BaselineCalculator`(#24)가 한다 */
sealed interface Baseline {

    /**
     * 직전 4주에 유효 세션이 활성 조건만큼 있다.
     *
     * @param densityPct 집중 밀도 `Σ T_focus / Σ T_total × 100`. 기간 합으로 계산해 긴 세션이 더 반영된다
     * @param interruptionsPerSession 세션당 이탈 `Σ N_int / 유효 세션 수`
     * @param lfsSec 세션 `LFS`의 평균. 초에서 버린다
     * @param weeklyFocusSec 주간 순공 총량 `Σ T_focus / 4`. 세션이 없는 주가 있어도 4로 나눈다. 초에서 버린다
     * @param sessionSec 평균 세션 길이. 공부 시간(`T_total`)의 평균이며 초에서 버린다. 화면에 보이지 않고 §7.3 세션 길이 가드만 쓴다
     * @param sessionCount 기준선에 쓴 유효 세션 수
     */
    data class Active(
        val densityPct: Float,
        val interruptionsPerSession: Float,
        val lfsSec: Int,
        val weeklyFocusSec: Int,
        val sessionSec: Int,
        val sessionCount: Int,
    ) : Baseline

    /** 유효 세션이 모자라 판정·목표 대신 "기준선 계산에 세션 [neededSessions]개가 더 필요합니다"를 쓴다 */
    data class Inactive(val neededSessions: Int) : Baseline
}
