package com.jaeyun.tally.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaeyun.tally.R
import com.jaeyun.tally.util.DurationParts
import com.jaeyun.tally.util.durationParts

/** 세션 단위 길이: `40초`, `25분`, `2시간`, `1시간 28분` (PLAN.md §4 시간 표기 규칙) */
@Composable
fun sessionDurationText(seconds: Long): String = when (val parts = durationParts(seconds)) {
    is DurationParts.Seconds -> stringResource(R.string.duration_seconds, parts.seconds)
    is DurationParts.Minutes -> stringResource(R.string.duration_minutes, parts.minutes)
    is DurationParts.Hours -> stringResource(R.string.duration_hours, parts.hours)
    is DurationParts.HoursMinutes -> stringResource(R.string.duration_hours_minutes, parts.hours, parts.minutes)
}

/** 주간·누적 총량: `40s`, `25m`, `2h`, `21h 24m` (PLAN.md §4 시간 표기 규칙) */
@Composable
fun totalDurationText(seconds: Long): String = when (val parts = durationParts(seconds)) {
    is DurationParts.Seconds -> stringResource(R.string.total_seconds, parts.seconds)
    is DurationParts.Minutes -> stringResource(R.string.total_minutes, parts.minutes)
    is DurationParts.Hours -> stringResource(R.string.total_hours, parts.hours)
    is DurationParts.HoursMinutes -> stringResource(R.string.total_hours_minutes, parts.hours, parts.minutes)
}
