package com.jaeyun.tally.domain.model

/**
 * 타임라인 구간 분류 (§5.1, PLAN.md §4).
 * Room에는 이름(TEXT)으로 저장되므로 상수 이름을 바꾸면 저장된 데이터를 읽지 못한다.
 */
enum class Category {
    /** 딴짓 앱. 순공에서 차감한다 */
    DISTRACT,

    /** 허용 앱(사전·노트·강의·시스템·런처·본 앱). 순공에 포함한다 */
    ALLOWED,

    /** 화면 꺼짐. 폰을 만지지 않았으므로 순공에 포함한다 */
    SCREEN_OFF,

    /** 자리 비움. §6.3 #6 `[자리 비웠어요]`를 고른 구간. 착석·순공에서 모두 제외한다 */
    AWAY,
}
