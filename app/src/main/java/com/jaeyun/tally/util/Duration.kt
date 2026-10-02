package com.jaeyun.tally.util

/**
 * 시간 길이를 표기할 단위로 나눈 것 (PLAN.md §4 시간 표기 규칙). 문구는 화면이 문자열 리소스로 만든다.
 * - 세션 단위(리포트·세션 목록·요약 줄): 한글 — `40초`, `25분`, `2시간`, `1시간 28분`
 * - 주간·누적 총량: `h m` — `40s`, `25m`, `2h`, `21h 24m`
 */
sealed interface DurationParts {
    data class Seconds(val seconds: Int) : DurationParts
    data class Minutes(val minutes: Int) : DurationParts
    data class Hours(val hours: Int) : DurationParts
    data class HoursMinutes(val hours: Int, val minutes: Int) : DurationParts
}

/**
 * 1분 미만은 초, 1시간 미만은 분, 그 이상은 시간과 분(분이 0이면 시간만). 남는 초와 분은 버린다.
 * 버림이라 "순공 최대"가 실제보다 커지지 않는다. 0초는 0으로 본다 — 0을 화면에 보일지는 화면이 정한다(§8.5.1).
 */
fun durationParts(totalSeconds: Long): DurationParts {
    val sec = totalSeconds.coerceAtLeast(0)
    val hours = (sec / 3600).toInt()
    val minutes = ((sec % 3600) / 60).toInt()
    return when {
        sec < 60 -> DurationParts.Seconds(sec.toInt())
        hours == 0 -> DurationParts.Minutes(minutes)
        minutes == 0 -> DurationParts.Hours(hours)
        else -> DurationParts.HoursMinutes(hours, minutes)
    }
}
