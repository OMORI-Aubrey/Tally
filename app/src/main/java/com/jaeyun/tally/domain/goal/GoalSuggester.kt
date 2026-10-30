package com.jaeyun.tally.domain.goal

import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.Goal
import com.jaeyun.tally.domain.model.GoalMetric
import com.jaeyun.tally.domain.model.WeekStats

/**
 * 다음 목표 하나를 고른다 (§7.3). 대부분 "기준선으로 복귀"이고, 전체 최고 기록은 입력으로 받지도 않는다(사후 배지로만 쓴다).
 * Android 의존성 없는 순수 함수다.
 *
 * 우선순위
 * 1. 세션당 이탈 ≥ 기준선 × [INTERRUPTION_PCT]% → 이탈 횟수, 기준선 복귀
 * 2. 평균 `LFS` < 기준선 × [LFS_PCT]% → `LFS`, 기준선 복귀
 * 3. 주간 순공 총량 < 기준선 주평균 × [FOCUS_PCT]% → 주간 순공 총량, 기준선 복귀
 * 4. 모두 정상 → 좋아지는 방향으로 맞춘 편차가 가장 작은(가장 덜 좋아진) 지표, 좋아지는 방향으로 [STRETCH_PCT]%
 *    (`LFS`·순공은 기준선 × 105%, 이탈은 × 95%). 편차가 같으면 이탈 → `LFS` → 순공 순서 (PLAN.md §4)
 *
 * 세션 길이 가드: 평균 세션 길이가 기준선의 ±[SESSION_LENGTH_TOLERANCE_PCT]% 밖이면 `LFS`를 그 주의 목표 대상에서
 * 뺀다(2순위와 4순위 모두). `LFS`는 세션 길이가 상한이라 세션 운영이 바뀐 주의 `LFS` 변화는 집중력 변화가 아니다.
 *
 * 비교는 퍼센트 정수로 곱해서 해 경계값(120%, 90%, ±20%)이 소수 오차로 어긋나지 않게 한다.
 * 계수는 모두 초기 설정값이며 §14.3 인터뷰로 조정한다.
 */
object GoalSuggester {
    const val INTERRUPTION_PCT = 120
    const val LFS_PCT = 90
    const val FOCUS_PCT = 90
    const val STRETCH_PCT = 5
    const val SESSION_LENGTH_TOLERANCE_PCT = 20

    /** 기준선이 미활성이면 목표가 없다(null). 화면은 "기준선 계산에 세션 N개가 더 필요합니다"를 쓴다 */
    fun suggest(week: WeekStats, baseline: Baseline): Goal? {
        if (baseline !is Baseline.Active) return null
        val lfsAllowed = isSessionLengthComparable(week.sessionSec, baseline.sessionSec)

        // 기준선 이탈이 0이면 이번 주도 0일 때 "0회로 복귀"가 되지 않도록 기준선보다 많을 때만 본다
        if (week.interruptionsPerSession * 100 >= baseline.interruptionsPerSession * INTERRUPTION_PCT &&
            week.interruptionsPerSession > baseline.interruptionsPerSession
        ) {
            return interruptionGoal(week, baseline, stretch = false)
        }
        if (lfsAllowed && week.lfsSec * 100L < baseline.lfsSec * LFS_PCT.toLong()) {
            return lfsGoal(week, baseline, stretch = false)
        }
        if (week.focusSec * 100L < baseline.weeklyFocusSec * FOCUS_PCT.toLong()) {
            return focusGoal(week, baseline, stretch = false)
        }

        val candidates = listOfNotNull(
            GoalMetric.INTERRUPTION to improvementPct(
                week.interruptionsPerSession, baseline.interruptionsPerSession, lowerIsBetter = true,
            ),
            if (lfsAllowed) GoalMetric.LFS to improvementPct(week.lfsSec.toFloat(), baseline.lfsSec.toFloat()) else null,
            GoalMetric.FOCUS_TOTAL to improvementPct(week.focusSec.toFloat(), baseline.weeklyFocusSec.toFloat()),
        )
        return when (candidates.minBy { it.second }.first) {
            GoalMetric.INTERRUPTION -> interruptionGoal(week, baseline, stretch = true)
            GoalMetric.LFS -> lfsGoal(week, baseline, stretch = true)
            GoalMetric.FOCUS_TOTAL -> focusGoal(week, baseline, stretch = true)
        }
    }

    /** [week]의 [metric] 값. 단위는 [GoalMetric]을 따른다(`LFS`·순공은 분, 이탈은 회) */
    fun valueOf(metric: GoalMetric, week: WeekStats): Float = when (metric) {
        GoalMetric.INTERRUPTION -> week.interruptionsPerSession
        GoalMetric.LFS -> week.lfsSec / 60f
        GoalMetric.FOCUS_TOTAL -> week.focusSec / 60f
    }

    /** 목표를 달성했는지. 이탈은 목표 이하, `LFS`·순공은 목표 이상이면 달성이다(목표값과 같아도 달성) */
    fun isAchieved(metric: GoalMetric, achievedValue: Float, targetValue: Float): Boolean = when (metric) {
        GoalMetric.INTERRUPTION -> achievedValue <= targetValue
        GoalMetric.LFS, GoalMetric.FOCUS_TOTAL -> achievedValue >= targetValue
    }

    /** 평균 세션 길이가 기준선의 ±20% 안이다. 경계값은 안에 넣는다 */
    internal fun isSessionLengthComparable(weekSessionSec: Int, baselineSessionSec: Int): Boolean {
        val week = weekSessionSec * 100L
        return week >= baselineSessionSec * (100L - SESSION_LENGTH_TOLERANCE_PCT) &&
            week <= baselineSessionSec * (100L + SESSION_LENGTH_TOLERANCE_PCT)
    }

    /**
     * 기준선 대비 편차(%). 좋아진 쪽이 양수가 되도록 부호를 맞춘다(이탈은 줄어야 좋다).
     * 기준선이 0이면 같을 때 0, 좋아졌으면 +∞, 나빠졌으면 −∞다.
     */
    internal fun improvementPct(week: Float, baseline: Float, lowerIsBetter: Boolean = false): Float {
        val diff = if (lowerIsBetter) baseline - week else week - baseline
        return when {
            baseline > 0f -> diff * 100f / baseline
            diff > 0f -> Float.POSITIVE_INFINITY
            diff < 0f -> Float.NEGATIVE_INFINITY
            else -> 0f
        }
    }

    private fun interruptionGoal(week: WeekStats, baseline: Baseline.Active, stretch: Boolean): Goal {
        val base = baseline.interruptionsPerSession
        return Goal(
            metric = GoalMetric.INTERRUPTION,
            currentValue = valueOf(GoalMetric.INTERRUPTION, week),
            targetValue = if (stretch) base * (100 - STRETCH_PCT) / 100 else base,
            baselineValue = base,
        )
    }

    private fun lfsGoal(week: WeekStats, baseline: Baseline.Active, stretch: Boolean): Goal =
        minutesGoal(GoalMetric.LFS, week, baseline.lfsSec, stretch)

    private fun focusGoal(week: WeekStats, baseline: Baseline.Active, stretch: Boolean): Goal =
        minutesGoal(GoalMetric.FOCUS_TOTAL, week, baseline.weeklyFocusSec, stretch)

    /** `LFS`·주간 순공 총량은 분 단위로 둔다([GoalMetric]) */
    private fun minutesGoal(metric: GoalMetric, week: WeekStats, baselineSec: Int, stretch: Boolean): Goal {
        val base = baselineSec / 60f
        return Goal(
            metric = metric,
            currentValue = valueOf(metric, week),
            targetValue = if (stretch) base * (100 + STRETCH_PCT) / 100 else base,
            baselineValue = base,
        )
    }
}
