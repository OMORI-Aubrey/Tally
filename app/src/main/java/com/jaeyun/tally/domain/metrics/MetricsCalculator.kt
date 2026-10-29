package com.jaeyun.tally.domain.metrics

import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.Segment
import com.jaeyun.tally.domain.model.SessionMetrics

/**
 * 복원된 구간 리스트를 한 번 순회해 세션 지표 6개를 낸다 (§5.2). Android 의존성 없는 순수 함수다.
 *
 * - `T_total` 착석 = 세션 길이 − `AWAY` 구간 (PLAN.md §4). 세션 길이는 구간 합이 아니라 시작·끝 시각으로 잰다(§6.2.2)
 * - `T_dist` 딴짓 = `DISTRACT` 구간 길이의 합
 * - `T_focus` 순공 = `T_total − T_dist`. `SCREEN_OFF`는 순공에 들어간다
 * - `LFS` = 연속된 비`DISTRACT` 구간 중 가장 긴 것. `AWAY`도 연속을 끊고 시간은 더하지 않는다 (PLAN.md §4)
 * - `N_int` 이탈 = `DISTRACT` 구간의 개수. 붙어 있는 `DISTRACT`는 하나로 센다
 * - `D` 집중 밀도 = `T_focus / T_total × 100`. 착석이 0이면 0
 *
 * 초 단위는 버림이다. 저장값에서 `T_focus + T_dist = T_total`이 항상 맞도록 `T_focus`는 버린 두 값의 차로 낸다.
 * 정의를 바꾸면 [MetricsVersion.CURRENT]를 올린다.
 */
object MetricsCalculator {

    fun calculate(segments: List<Segment>, sessionStart: Long, sessionEnd: Long): SessionMetrics {
        var awayMillis = 0L
        var distMillis = 0L
        var interruptions = 0
        var longestFocusMillis = 0L
        var runMillis = 0L
        var previous: Category? = null

        for (segment in segments) {
            when (segment.category) {
                Category.DISTRACT -> {
                    distMillis += segment.durationMillis
                    if (previous != Category.DISTRACT) interruptions++
                    runMillis = 0L
                }
                Category.AWAY -> {
                    awayMillis += segment.durationMillis
                    runMillis = 0L
                }
                Category.ALLOWED, Category.SCREEN_OFF -> {
                    runMillis += segment.durationMillis
                    longestFocusMillis = maxOf(longestFocusMillis, runMillis)
                }
            }
            previous = segment.category
        }

        val totalMillis = (sessionEnd - sessionStart - awayMillis).coerceAtLeast(0L)
        val tTotalSec = (totalMillis / 1000).toInt()
        val tDistSec = (distMillis / 1000).toInt().coerceAtMost(tTotalSec)
        val focusMillis = (totalMillis - distMillis).coerceAtLeast(0L)
        return SessionMetrics(
            tTotalSec = tTotalSec,
            tFocusSec = tTotalSec - tDistSec,
            tDistSec = tDistSec,
            lfsSec = (longestFocusMillis / 1000).toInt(),
            densityPct = if (totalMillis == 0L) 0f else focusMillis * 100f / totalMillis,
            interruptionCount = interruptions,
        )
    }

    /**
     * 인식 격차 `G = (체감분 − T_focus분) / T_focus분 × 100` (%). **양수가 과대평가**다. 모든 화면에서 이 부호를 쓴다(§5.2).
     * 저장하지 않고 조회할 때 계산한다. 체감 입력이 없거나 `T_focus`가 0이면 null이다(0 나눗셈 회피).
     */
    fun perceptionGapPct(perceivedFocusMin: Int?, tFocusSec: Int): Float? {
        if (perceivedFocusMin == null || tFocusSec <= 0) return null
        val focusMin = tFocusSec / 60f
        return (perceivedFocusMin - focusMin) / focusMin * 100f
    }
}
