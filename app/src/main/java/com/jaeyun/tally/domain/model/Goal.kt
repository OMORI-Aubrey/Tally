package com.jaeyun.tally.domain.model

/**
 * 다음 목표 하나 (§7.3). 계산은 `GoalSuggester`(#26)가 한다. 값의 단위는 [GoalMetric]을 따른다(분 또는 회).
 *
 * 표시 예: `세션당 이탈   4.4회 → 3.4회` / `지난 4주 평소값입니다`
 *
 * @param currentValue 목표를 정할 때 본 주의 값 (화살표 왼쪽)
 * @param targetValue 목표값. 1~3순위는 [baselineValue]와 같고(기준선 복귀), 4순위만 좋아지는 방향으로 5% 앞선다
 * @param baselineValue 기준선 값 (`WeeklyGoal`에 근거로 보존한다)
 */
data class Goal(
    val metric: GoalMetric,
    val currentValue: Float,
    val targetValue: Float,
    val baselineValue: Float,
)
