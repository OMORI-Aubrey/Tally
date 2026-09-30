package com.jaeyun.tally.domain.metrics

/**
 * 세션 지표를 계산한 규칙의 버전. `StudySession.metricsVersion`에 함께 저장한다.
 *
 * 지표는 비정규화 저장이라(§9) 규칙이 바뀌면 이전 세션의 값이 새 규칙과 어긋난다.
 * 아래 중 하나라도 바뀌면 [CURRENT]를 올린다. 저장된 버전이 낮은 세션이 재계산 대상이다.
 * - §5.2 지표 정의 (`AWAY` 제외 규칙 포함)
 * - §6.2 복원 알고리즘 (입력 이벤트, 흡수 임계값, KEYGUARD_HIDDEN 규칙, 병합 순서)
 * - 구간 분류 의미 (`Category`)
 */
object MetricsVersion {
    /** 지표를 아직 계산하지 않은 세션(진행 중·미완료) */
    const val NOT_COMPUTED = 0

    /** PLAN.md P0-3 확정 지표·임계값 (2026-09-30) */
    const val CURRENT = 1
}
