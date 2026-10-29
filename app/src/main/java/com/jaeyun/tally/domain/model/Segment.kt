package com.jaeyun.tally.domain.model

/**
 * 복원된 타임라인의 한 구간 `[startAt, endAt)`. 시각은 epoch millis.
 *
 * [packageName]은 앱 패키지이거나 의사 패키지([PseudoPackage])다. 같은 분류의 구간을 합친 경우에는
 * 그 안에서 가장 오래 머문 패키지를 대표로 남긴다(이탈 목록이 이 이름으로 보여준다, §8.2).
 */
data class Segment(
    val packageName: String,
    val category: Category,
    val startAt: Long,
    val endAt: Long,
) {
    val durationMillis: Long get() = endAt - startAt
}

/** 앱이 아닌 상태를 구간으로 나타내는 의사 패키지 (§6.2) */
object PseudoPackage {
    /** 화면 꺼짐 */
    const val SCREEN_OFF = "__SCREEN_OFF__"

    /** 화면은 켜졌지만 어떤 앱도 포그라운드로 오지 않았다(잠금화면·알림 셰이드). `DISTRACT`로 분류한다 (§6.2.1) */
    const val SCREEN_ON_UNKNOWN = "__SCREEN_ON_UNKNOWN__"
}
