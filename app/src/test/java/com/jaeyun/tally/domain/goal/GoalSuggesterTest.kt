package com.jaeyun.tally.domain.goal

import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.Goal
import com.jaeyun.tally.domain.model.GoalMetric.FOCUS_TOTAL
import com.jaeyun.tally.domain.model.GoalMetric.INTERRUPTION
import com.jaeyun.tally.domain.model.GoalMetric.LFS
import com.jaeyun.tally.domain.model.WeekStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val MIN = 60

/** 기준선: 세션당 이탈 2.5회, 평균 LFS 40분, 주간 순공 600분, 평균 세션 길이 60분 */
private val BASELINE = Baseline.Active(
    densityPct = 70f,
    interruptionsPerSession = 2.5f,
    lfsSec = 40 * MIN,
    weeklyFocusSec = 600 * MIN,
    sessionSec = 60 * MIN,
    sessionCount = 12,
)

/** 기준선과 똑같은 주. 각 테스트는 바꿀 값만 고친다 */
private val NORMAL = WeekStats(
    interruptionsPerSession = 2.5f,
    lfsSec = 40 * MIN,
    focusSec = 600 * MIN,
    sessionSec = 60 * MIN,
)

private fun WeekStats.goal(): Goal? = GoalSuggester.suggest(this, BASELINE)

class GoalSuggesterTest {

    // §14.2 목표 케이스

    @Test
    fun `기준선이 미활성이면 목표가 없다`() {
        assertNull(GoalSuggester.suggest(NORMAL, Baseline.Inactive(neededSessions = 1)))
    }

    @Test
    fun `이탈이 기준선보다 25% 많으면 이탈 횟수를 기준선 값으로 되돌린다`() {
        val goal = NORMAL.copy(interruptionsPerSession = 3.125f).goal()

        assertEquals(Goal(INTERRUPTION, currentValue = 3.125f, targetValue = 2.5f, baselineValue = 2.5f), goal)
    }

    @Test
    fun `최장 구간이 기준선의 85%면 LFS를 기준선 값으로 되돌린다`() {
        val goal = NORMAL.copy(lfsSec = 34 * MIN).goal()

        assertEquals(Goal(LFS, currentValue = 34f, targetValue = 40f, baselineValue = 40f), goal)
    }

    @Test
    fun `순공 총량이 기준선의 85%이고 나머지가 정상이면 주간 순공 총량 (3순위)`() {
        val goal = NORMAL.copy(focusSec = 510 * MIN).goal()

        assertEquals(Goal(FOCUS_TOTAL, currentValue = 510f, targetValue = 600f, baselineValue = 600f), goal)
    }

    @Test
    fun `세 지표가 모두 정상이면 가장 덜 좋아진 지표를 기준선보다 5% 앞에 둔다`() {
        // 좋아진 정도: 이탈 +10%(2.25회), LFS +25%(50분), 순공 +5%(630분) → 순공
        val goal = NORMAL.copy(interruptionsPerSession = 2.25f, lfsSec = 50 * MIN, focusSec = 630 * MIN).goal()

        assertEquals(Goal(FOCUS_TOTAL, currentValue = 630f, targetValue = 630f, baselineValue = 600f), goal)
    }

    @Test
    fun `4순위가 이탈이면 목표는 기준선보다 5% 적다`() {
        // 좋아진 정도: 이탈 −10%(2.75회, 1순위 120%에는 못 미침), LFS 0%, 순공 0% → 이탈
        val goal = NORMAL.copy(interruptionsPerSession = 2.75f).goal()

        assertEquals(INTERRUPTION, goal?.metric)
        assertEquals(2.5f * 0.95f, goal!!.targetValue, 0.0001f)
    }

    @Test
    fun `4순위가 LFS면 목표는 기준선보다 5% 길다`() {
        // 좋아진 정도: 이탈 +20%, LFS −5%(38분, 2순위 90%에는 못 미침), 순공 0% → LFS
        val goal = NORMAL.copy(interruptionsPerSession = 2f, lfsSec = 38 * MIN).goal()

        assertEquals(Goal(LFS, currentValue = 38f, targetValue = 42f, baselineValue = 40f), goal)
    }

    @Test
    fun `최고 기록을 갱신한 주에도 신기록이 목표가 되지 않는다`() {
        // LFS가 기준선의 3배(최고 기록 갱신). 이탈·순공이 0%로 같아 순서상 이탈을 기준선보다 5% 적게
        val goal = NORMAL.copy(lfsSec = 120 * MIN).goal()!!

        assertNotEquals(LFS, goal.metric)
        assertEquals(Goal(INTERRUPTION, currentValue = 2.5f, targetValue = 2.375f, baselineValue = 2.5f), goal)
    }

    @Test
    fun `평균 세션 길이가 기준선의 150%면 LFS 미달이어도 LFS를 빼고 3순위로 넘어간다`() {
        val goal = NORMAL.copy(lfsSec = 30 * MIN, focusSec = 510 * MIN, sessionSec = 90 * MIN).goal()

        assertEquals(FOCUS_TOTAL, goal?.metric)
    }

    @Test
    fun `평균 세션 길이가 기준선의 90%면 가드가 걸리지 않아 LFS를 고른다`() {
        val goal = NORMAL.copy(lfsSec = 30 * MIN, focusSec = 510 * MIN, sessionSec = 54 * MIN).goal()

        assertEquals(LFS, goal?.metric)
    }

    // 세션 길이 가드는 4순위에도 적용한다 (PLAN.md §4)

    @Test
    fun `세션 길이 가드가 걸린 주는 4순위에서도 LFS를 고르지 않는다`() {
        // LFS −5%가 가장 덜 좋아졌지만 세션 길이 150%라 빠지고, 다음으로 덜 좋아진 순공(0%)
        val goal = NORMAL.copy(interruptionsPerSession = 2f, lfsSec = 38 * MIN, sessionSec = 90 * MIN).goal()

        assertEquals(FOCUS_TOTAL, goal?.metric)
    }

    // 우선순위

    @Test
    fun `이탈과 LFS가 모두 나쁘면 1순위 이탈`() {
        val goal = NORMAL.copy(interruptionsPerSession = 4f, lfsSec = 20 * MIN, focusSec = 300 * MIN).goal()

        assertEquals(INTERRUPTION, goal?.metric)
    }

    @Test
    fun `LFS와 순공이 모두 나쁘면 2순위 LFS`() {
        val goal = NORMAL.copy(lfsSec = 20 * MIN, focusSec = 300 * MIN).goal()

        assertEquals(LFS, goal?.metric)
    }

    @Test
    fun `4순위에서 편차가 같으면 이탈 → LFS → 순공 순서`() {
        assertEquals(INTERRUPTION, NORMAL.goal()?.metric)
    }

    // 경계값

    @Test
    fun `이탈이 정확히 기준선의 120%면 1순위`() {
        assertEquals(Goal(INTERRUPTION, 3f, 2.5f, 2.5f), NORMAL.copy(interruptionsPerSession = 3f).goal())
    }

    @Test
    fun `이탈이 120%에 못 미치면 1순위가 아니다`() {
        assertEquals(2.5f * 0.95f, NORMAL.copy(interruptionsPerSession = 2.99f).goal()!!.targetValue, 0.0001f)
    }

    @Test
    fun `LFS가 정확히 기준선의 90%면 2순위가 아니고, 1초 모자라면 2순위`() {
        // 정확히 90%면 4순위에서 가장 덜 좋아진 LFS(−10%)를 기준선 + 5%로
        assertEquals(Goal(LFS, 36f, 42f, 40f), NORMAL.copy(lfsSec = 36 * MIN).goal())
        assertEquals(Goal(LFS, (36 * MIN - 1) / 60f, 40f, 40f), NORMAL.copy(lfsSec = 36 * MIN - 1).goal())
    }

    @Test
    fun `순공이 정확히 기준선의 90%면 3순위가 아니고, 1초 모자라면 3순위`() {
        // 정확히 90%면 4순위에서 가장 덜 좋아진 순공(−10%)을 기준선 + 5%로
        assertEquals(Goal(FOCUS_TOTAL, 540f, 630f, 600f), NORMAL.copy(focusSec = 540 * MIN).goal())
        assertEquals(Goal(FOCUS_TOTAL, (540 * MIN - 1) / 60f, 600f, 600f), NORMAL.copy(focusSec = 540 * MIN - 1).goal())
    }

    @Test
    fun `세션 길이 ±20% 경계값은 가드 안이다`() {
        assertEquals(true, GoalSuggester.isSessionLengthComparable(72 * MIN, 60 * MIN))
        assertEquals(true, GoalSuggester.isSessionLengthComparable(48 * MIN, 60 * MIN))
        assertEquals(false, GoalSuggester.isSessionLengthComparable(72 * MIN + 1, 60 * MIN))
        assertEquals(false, GoalSuggester.isSessionLengthComparable(48 * MIN - 1, 60 * MIN))
    }

    // 기준선 0

    @Test
    fun `기준선 이탈이 0이고 이번 주도 0이면 이탈 복귀 목표가 아니다`() {
        val baseline = BASELINE.copy(interruptionsPerSession = 0f)
        val goal = GoalSuggester.suggest(NORMAL.copy(interruptionsPerSession = 0f), baseline)

        // 이탈 0% · LFS 0% · 순공 0%로 같아 순서상 이탈이지만, 1순위(복귀)가 아니라 4순위다
        assertEquals(Goal(INTERRUPTION, 0f, 0f, 0f), goal)
    }

    @Test
    fun `기준선 이탈이 0인데 이번 주에 이탈이 있으면 0회로 되돌린다`() {
        val baseline = BASELINE.copy(interruptionsPerSession = 0f)

        assertEquals(Goal(INTERRUPTION, 1f, 0f, 0f), GoalSuggester.suggest(NORMAL.copy(interruptionsPerSession = 1f), baseline))
    }
}
