package com.jaeyun.tally.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 한 주의 `[startAt, endAt)` epoch millis. 주는 월요일 00:00(로컬 시간대)에 시작한다 (PLAN.md §2) */
data class WeekRange(val startAt: Long, val endAt: Long) {
    operator fun contains(at: Long): Boolean = at >= startAt && at < endAt
}

/**
 * [at]이 속한 주. 현재 시각도 인자로 받는다(§14.2 테스트 고정).
 * 일광 절약 시간이 바뀌는 주는 7일 ± 1시간이 될 수 있다. 자정을 날짜 기준으로 계산하므로 경계는 항상 00:00이다.
 */
fun weekRangeOf(at: Long, zone: ZoneId): WeekRange {
    val monday = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return WeekRange(
        startAt = monday.atStartOfDay(zone).toInstant().toEpochMilli(),
        endAt = monday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
