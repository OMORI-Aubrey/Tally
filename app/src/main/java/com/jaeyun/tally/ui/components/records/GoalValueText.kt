package com.jaeyun.tally.ui.components.records

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaeyun.tally.R
import com.jaeyun.tally.domain.model.GoalMetric
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.components.text.totalDurationText
import kotlin.math.roundToInt

/** 세션당 횟수처럼 소수가 있는 수: 소수 첫째 자리에서 반올림하고 `.0`은 쓰지 않는다(`4.4`, `3`) */
fun decimalText(value: Float): String {
    val tenths = (value * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}

/** 세션당 이탈: `4.4회` */
@Composable
fun perSessionCountText(value: Float): String = stringResource(R.string.count_times, decimalText(value))

/** 목표 지표의 이름: `세션당 이탈`, `최장 평균`, `주간 순공` */
@Composable
fun goalMetricLabel(metric: GoalMetric): String = when (metric) {
    GoalMetric.INTERRUPTION -> stringResource(R.string.goal_metric_interruption)
    GoalMetric.LFS -> stringResource(R.string.goal_metric_lfs)
    GoalMetric.FOCUS_TOTAL -> stringResource(R.string.goal_metric_focus)
}

/**
 * 목표 값. 단위는 [GoalMetric]을 따른다 — 이탈은 회(`3.4회`), `LFS`는 분(`40분`, 세션 단위 표기), 주간 순공은 분(`10h 30m`, 총량 표기)
 */
@Composable
fun goalValueText(metric: GoalMetric, value: Float): String = when (metric) {
    GoalMetric.INTERRUPTION -> perSessionCountText(value)
    GoalMetric.LFS -> sessionDurationText((value * 60).toLong())
    GoalMetric.FOCUS_TOTAL -> totalDurationText((value * 60).toLong())
}
