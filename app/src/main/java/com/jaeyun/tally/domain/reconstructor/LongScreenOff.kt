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
