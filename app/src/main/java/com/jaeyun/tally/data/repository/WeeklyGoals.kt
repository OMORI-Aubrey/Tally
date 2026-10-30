package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.WeeklyGoal
import com.jaeyun.tally.domain.goal.GoalSuggester
import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.WeekStats

/**
 * 끝난 주의 목표에 그 주 값을 채운다 (§7.3). DB 조회는 [WeeklyGoalRepository]가 하고, 여기서는 계산만 한다.
 * 그 주에 통계 세션이 없었으면([week]가 null) 견줄 값이 없어 미달성이다. 미달성은 화면에 아무것도 보이지 않는다.
 */
internal fun WeeklyGoal.judged(week: WeekStats?): WeeklyGoal {
    if (week == null) return copy(achievedValue = null, isAchieved = false)
    val achieved = GoalSuggester.valueOf(metric, week)
    return copy(achievedValue = achieved, isAchieved = GoalSuggester.isAchieved(metric, achieved, targetValue))
}

/**
 * 이번 주([thisWeekStartAt]) 목표. 끝난 지난주 값([lastWeek])을 지난주를 뺀 직전 4주 기준선([baseline])과 비교한다(PLAN.md §4).
 * 지난주에 통계 세션이 없거나(비교할 사실이 없다) 기준선이 미활성이면 목표가 없다.
 */
internal fun weeklyGoalFor(thisWeekStartAt: Long, lastWeek: WeekStats?, baseline: Baseline): WeeklyGoal? {
    val goal = GoalSuggester.suggest(lastWeek ?: return null, baseline) ?: return null
    return WeeklyGoal(
        weekStartAt = thisWeekStartAt,
        metric = goal.metric,
        targetValue = goal.targetValue,
        baselineValue = goal.baselineValue,
    )
}

/** 지난주 목표를 달성했을 때만 배지로 보여 준다. 미달성·판정 전·목표 없음은 null이다(실패를 알리지 않는다, §7.3) */
internal fun achievedBadge(lastWeekGoal: WeeklyGoal?): WeeklyGoal? = lastWeekGoal?.takeIf { it.isAchieved == true }
