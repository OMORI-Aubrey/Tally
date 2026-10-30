package com.jaeyun.tally.domain.model

/**
 * 한 주의 통계 세션(`countsInStats`)을 모은 값. 다음 목표(§7.3)가 기준선([Baseline.Active])과 비교한다.
 * 정의는 기준선과 같고, 순공 총량만 4로 나누지 않은 그 주의 합이다.
 *
 * @param interruptionsPerSession 세션당 이탈 `Σ N_int / 세션 수`
 * @param lfsSec 세션 `LFS`의 평균. 초에서 버린다
 * @param focusSec 주간 순공 총량 `Σ T_focus`
 * @param sessionSec 평균 세션 길이. 공부 시간(`T_total`)의 평균이며 초에서 버린다 (PLAN.md §4)
 */
data class WeekStats(
    val interruptionsPerSession: Float,
    val lfsSec: Int,
    val focusSec: Int,
    val sessionSec: Int,
)
