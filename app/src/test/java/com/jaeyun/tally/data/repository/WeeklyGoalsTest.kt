package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.WeeklyGoal
import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.GoalMetric
import com.jaeyun.tally.domain.model.WeekStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val MIN = 60
private const val THIS_WEEK = 1_000_000L

/** 기준선: 세션당 이탈 2.5회, 평균 LFS 40분, 주간 순공 600분, 평균 세션 길이 60분 */
private val BASELINE = Baseline.Active(
    densityPct = 70f,
    interruptionsPerSession = 2.5f,
    lfsSec = 40 * MIN,
    weeklyFocusSec = 600 * MIN,
    sessionSec = 60 * MIN,
    sessionCount = 12,
)

private val NORMAL = WeekStats(interruptionsPerSession = 2.5f, lfsSec = 40 * MIN, focusSec = 600 * MIN, sessionSec = 60 * MIN)

/** 세션당 이탈 4.4회 → 3.4회 목표 (§7.3 예시) */
private val INTERRUPTION_GOAL = WeeklyGoal(weekStartAt = 0L, metric = GoalMetric.INTERRUPTION, targetValue = 3.4f, baselineValue = 3.4f)

private val LFS_GOAL = WeeklyGoal(weekStartAt = 0L, metric = GoalMetric.LFS, targetValue = 40f, baselineValue = 40f)

class WeeklyGoalsTest {

    // 이번 주 목표 만들기

    @Test
    fun `지난주 값과 기준선으로 이번 주 목표를 만든다`() {
        val goal = weeklyGoalFor(THIS_WEEK, NORMAL.copy(interruptionsPerSession = 3.125f), BASELINE)

        assertEquals(WeeklyGoal(THIS_WEEK, GoalMetric.INTERRUPTION, targetValue = 2.5f, baselineValue = 2.5f), goal)
    }

    @Test
    fun `지난주에 통계 세션이 없으면 목표를 만들지 않는다`() {
        assertNull(weeklyGoalFor(THIS_WEEK, lastWeek = null, baseline = BASELINE))
    }

    @Test
    fun `기준선이 미활성이면 목표를 만들지 않는다`() {
        assertNull(weeklyGoalFor(THIS_WEEK, NORMAL, Baseline.Inactive(neededSessions = 3)))
    }

    // 지난주 목표 판정

    @Test
    fun `이탈 목표는 목표 이하면 달성이다`() {
        val judged = INTERRUPTION_GOAL.judged(NORMAL.copy(interruptionsPerSession = 3.2f))

        assertEquals(3.2f, judged.achievedValue)
        assertEquals(true, judged.isAchieved)
    }

    @Test
    fun `이탈 목표는 목표보다 많으면 미달성이다`() {
        val judged = INTERRUPTION_GOAL.judged(NORMAL.copy(interruptionsPerSession = 4f))

        assertEquals(4f, judged.achievedValue)
        assertEquals(false, judged.isAchieved)
    }

    @Test
    fun `LFS 목표는 분 단위로 목표 이상이면 달성이다`() {
        assertEquals(true, LFS_GOAL.judged(NORMAL.copy(lfsSec = 40 * MIN)).isAchieved)
        assertEquals(false, LFS_GOAL.judged(NORMAL.copy(lfsSec = 40 * MIN - 1)).isAchieved)
        assertEquals(41f, LFS_GOAL.judged(NORMAL.copy(lfsSec = 41 * MIN)).achievedValue)
    }

    @Test
    fun `그 주에 통계 세션이 없었으면 견줄 값이 없어 미달성이다`() {
        val judged = INTERRUPTION_GOAL.judged(week = null)

        assertNull(judged.achievedValue)
        assertEquals(false, judged.isAchieved)
    }

    @Test
    fun `판정해도 제시 당시 목표값과 기준선 값은 그대로다`() {
        val judged = INTERRUPTION_GOAL.judged(NORMAL)

        assertEquals(INTERRUPTION_GOAL.targetValue, judged.targetValue)
        assertEquals(INTERRUPTION_GOAL.baselineValue, judged.baselineValue)
    }

    // §14.2 배지

    @Test
    fun `지난주 목표 달성이면 배지를 보여 준다`() {
        val achieved = INTERRUPTION_GOAL.judged(NORMAL.copy(interruptionsPerSession = 3.2f))

        assertEquals(achieved, achievedBadge(achieved))
    }

    @Test
    fun `지난주 목표 미달성이면 아무것도 보여 주지 않는다`() {
        assertNull(achievedBadge(INTERRUPTION_GOAL.judged(NORMAL.copy(interruptionsPerSession = 4f))))
        assertNull(achievedBadge(INTERRUPTION_GOAL.judged(week = null)))
    }

    @Test
    fun `지난주 목표가 없거나 판정 전이면 배지가 없다`() {
        assertNull(achievedBadge(null))
        assertNull(achievedBadge(INTERRUPTION_GOAL))
    }
}
