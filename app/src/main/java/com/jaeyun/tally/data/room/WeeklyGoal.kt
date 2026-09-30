package com.jaeyun.tally.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jaeyun.tally.domain.model.GoalMetric

/**
 * 주간 목표 (§7.3, §9). 규칙이 바뀌어도 과거 목표가 소급 변경되지 않도록 제시 당시 값을 보존한다.
 */
@Entity
data class WeeklyGoal(
    /** 월요일 00:00(로컬 시간대)의 epoch millis */
    @PrimaryKey val weekStartAt: Long,
    val metric: GoalMetric,
    /** 분 또는 회. 세션당 이탈은 소수(3.4회)이므로 Float */
    val targetValue: Float,
    /** 제시 당시의 기준선 값 (근거 보존) */
    val baselineValue: Float,
    /** 주 종료 후 채운다 */
    val achievedValue: Float? = null,
    val isAchieved: Boolean? = null,
)
