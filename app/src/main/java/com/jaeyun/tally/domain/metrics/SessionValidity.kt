package com.jaeyun.tally.domain.metrics

/**
 * 통계에 넣을 세션인지 정한다 (§6.3 #3·#4, PLAN.md P0-3). `StudySession.isValidForStats`에 저장한다.
 *
 * 3분 미만은 오조작, 180분 초과는 종료를 잊었을 가능성이 커서 기록만 남기고 통계에서 뺀다. 경계값은 포함한다.
 * 지표 정의가 아니므로 바꿔도 [MetricsVersion]을 올리지 않는다. 저장된 값만 세션 길이로 다시 계산하면 된다.
 */
object SessionValidity {
    const val MIN_SEC = 3 * 60
    const val MAX_SEC = 180 * 60

    fun isValidForStats(sessionSec: Int): Boolean = sessionSec in MIN_SEC..MAX_SEC
}
