package com.jaeyun.tally.domain.model

/**
 * 주간 목표 대상 지표 (§7.3).
 * Room에는 이름(TEXT)으로 저장되므로 상수 이름을 바꾸면 저장된 데이터를 읽지 못한다.
 */
enum class GoalMetric {
    /** 최장 무방해 구간. 목표값 단위는 분 */
    LFS,

    /** 세션당 이탈 횟수. 목표값 단위는 회(소수) */
    INTERRUPTION,

    /** 주간 순공 총량. 목표값 단위는 분 */
    FOCUS_TOTAL,
}
