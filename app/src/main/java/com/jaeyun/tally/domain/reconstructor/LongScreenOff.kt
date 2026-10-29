package com.jaeyun.tally.domain.reconstructor

import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.Segment

/** 화면 꺼짐 단일 구간이 이보다 길면 리포트 전에 확인 다이얼로그를 띄운다 (§6.3 #6, PLAN.md P0-3) */
const val LONG_SCREEN_OFF_MILLIS = 60 * 60_000L

/**
 * 타이머를 켠 채 잠들었을 수 있는 구간 (§6.3 #6). 화면 꺼짐 단일 구간이 [thresholdMillis]를 **넘는** 것만 고른다.
 * 사용자가 `[자리를 비웠어요]`를 고르면 그 구간이 `AWAY`가 된다 (#21).
 */
fun List<Segment>.longScreenOffSegments(thresholdMillis: Long = LONG_SCREEN_OFF_MILLIS): List<Segment> =
    filter { it.category == Category.SCREEN_OFF && it.durationMillis > thresholdMillis }

/**
 * 시작·끝이 [startAt]·[endAt]인 화면 꺼짐 구간을 자리 비움(`AWAY`)으로 바꾼다 (§6.3 #6). 경계는 그대로라 불변식이 유지되고,
 * 이웃 구간은 화면 꺼짐이 아니므로(같은 분류는 이미 병합됐다) 새로 합칠 구간도 없다.
 * 화면 꺼짐이 아니거나 경계가 다른 구간은 건드리지 않는다.
 */
fun List<Segment>.markAway(startAt: Long, endAt: Long): List<Segment> = map {
    if (it.category == Category.SCREEN_OFF && it.startAt == startAt && it.endAt == endAt) it.copy(category = Category.AWAY) else it
}
