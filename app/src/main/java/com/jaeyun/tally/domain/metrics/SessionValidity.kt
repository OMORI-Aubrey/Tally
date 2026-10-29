package com.jaeyun.tally.domain.metrics

/**
 * 통계에 넣을 세션인지 정한다 (§6.3 #3·#4, PLAN.md P0-3). `StudySession.isValidForStats`에 저장한다.
 *
 * 길이는 착석(`T_total`)으로 잰다. 자리 비움은 일시정지라 빠진다(PLAN.md §4). 자리 비움이 없으면 세션 길이와 같다.
 * 3분 미만은 오조작이라 기록만 남기고 통계에서 뺀다. 180분 초과는 종료를 잊었을 수 있어 일단 빼고, 리포트 전에
 * 종료를 잊었는지 묻는다([isOverLong], #21). 경계값은 포함한다.
 * 지표 정의가 아니므로 바꿔도 [MetricsVersion]을 올리지 않는다. 저장된 값만 `tTotalSec`로 다시 계산하면 된다.
 */
object SessionValidity {
    const val MIN_SEC = 3 * 60
    const val MAX_SEC = 180 * 60

    fun isValidForStats(totalSec: Int): Boolean = totalSec in MIN_SEC..MAX_SEC

    /** 종료를 잊었는지 물을 길이 (§6.3 #4). 180분 정각은 묻지 않는다 */
    fun isOverLong(totalSec: Int): Boolean = totalSec > MAX_SEC
}
