package com.jaeyun.tally.domain.model

/**
 * 기준선(§5.3)이 읽는 끝난 세션 하나의 값. `StudySession`에 저장된 값을 그대로 옮긴다.
 *
 * @param startAt 세션 시작 시각(epoch millis). 세션은 시작한 주에 속한다
 * @param isValidForStats 3분 이상·180분 이하인지 (§6.3 #3·#4)
 * @param hasTimeline 사용 기록으로 타임라인을 복원했는지. false면 착석 외 지표가 0이다 (PLAN.md §4)
 * @param tTotalSec 착석(공부) 시간 `T_total`
 * @param tFocusSec 순공 시간 `T_focus`
 * @param lfsSec 최장 무방해 구간 `LFS`
 * @param interruptionCount 이탈 횟수 `N_int`
 */
data class SessionStats(
    val startAt: Long,
    val isValidForStats: Boolean,
    val hasTimeline: Boolean,
    val tTotalSec: Int,
    val tFocusSec: Int,
    val lfsSec: Int,
    val interruptionCount: Int,
) {
    /** 통계에 셀 세션. `data/room`의 `StudySession.countsInStats`와 같은 기준이다 */
    val countsInStats: Boolean get() = isValidForStats && hasTimeline
}
