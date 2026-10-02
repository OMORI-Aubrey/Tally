package com.jaeyun.tally.domain.model

/** 타임라인 복원에 쓰는 사용 기록 이벤트 4종 (§6.1). `ACTIVITY_PAUSED`는 쓰지 않는다 */
enum class RawEventType {
    /** 앱이 포그라운드로 들어왔다 */
    ACTIVITY_RESUMED,

    /** 화면이 켜졌다 */
    SCREEN_INTERACTIVE,

    /** 화면이 꺼졌다 */
    SCREEN_NON_INTERACTIVE,

    /** 잠금이 풀렸다. `__SCREEN_ON_UNKNOWN__` 구간을 끝내는 데만 쓴다 (§6.2.1) */
    KEYGUARD_HIDDEN,
}

/**
 * `UsageEvents.Event`에서 복원에 필요한 값만 옮긴 것. Android 타입에 의존하지 않도록 `data/usagestats`가 변환한다.
 *
 * @param timestamp epoch millis
 * @param packageName [RawEventType.ACTIVITY_RESUMED]에서만 쓴다. 나머지 타입에서는 무시한다
 */
data class RawEvent(
    val timestamp: Long,
    val type: RawEventType,
    val packageName: String? = null,
)
