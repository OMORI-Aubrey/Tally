package com.jaeyun.tally.domain.model

/**
 * 세션 단위 산출 지표 6개 (§5.2). `StudySession`에 비정규화 저장한다. 계산은 `MetricsCalculator`(#10)가 한다.
 *
 * @param tTotalSec 착석 시간 `T_total`
 * @param tFocusSec 순공 시간 `T_focus = T_total − T_dist`
 * @param tDistSec 딴짓 시간 `T_dist`. `DISTRACT` 구간 길이의 합
 * @param lfsSec 최장 무방해 구간 `LFS`. 연속된 비`DISTRACT` 구간 중 최대 길이
 * @param densityPct 집중 밀도 `D = T_focus / T_total × 100`
 * @param interruptionCount 이탈 횟수 `N_int`. `DISTRACT` 구간의 개수
 */
data class SessionMetrics(
    val tTotalSec: Int,
    val tFocusSec: Int,
    val tDistSec: Int,
    val lfsSec: Int,
    val densityPct: Float,
    val interruptionCount: Int,
)
