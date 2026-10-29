package com.jaeyun.tally.domain.baseline

import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.SessionStats
import java.time.Instant
import java.time.ZoneId

/**
 * 개인 기준선 (§5.3). 판정(§7.2)과 다음 목표(§7.3)의 근거다. Android 의존성 없는 순수 함수다.
 *
 * - 대상: 이번 주를 **제외한** 직전 4주. 세션은 시작한 주에 속한다
 * - 통계에 셀 세션([SessionStats.countsInStats])만 쓴다. 3분 미만·180분 초과와 타임라인 없는 세션은 빠진다 (PLAN.md §4)
 * - 활성 조건: 유효 세션 [REQUIRED_SESSIONS]개 이상. 모자라면 [Baseline.Inactive]에 더 필요한 개수를 담는다
 */
object BaselineCalculator {
    const val WEEKS = 4
    const val REQUIRED_SESSIONS = 12

    /** 완화 플래그를 켠 debug 빌드의 활성 조건 (§5.3, §14.3) */
    const val RELAXED_REQUIRED_SESSIONS = 6

    /** 활성 조건. 완화 플래그는 debug 빌드에서만 효과가 있다 (§5.3) */
    fun requiredSessions(relaxed: Boolean, debugBuild: Boolean): Int =
        if (relaxed && debugBuild) RELAXED_REQUIRED_SESSIONS else REQUIRED_SESSIONS

    /**
     * @param thisWeekStartAt 이번 주가 시작하는 시각(월요일 00:00, 로컬 시간대). 기준선은 이 시각 직전 4주다
     * @param zone 4주 전 월요일 00:00을 날짜로 계산할 시간대. 일광 절약 시간이 바뀌어도 경계가 00:00에 맞는다
     * @param requiredSessions 활성 조건([requiredSessions])
     */
    fun calculate(
        sessions: List<SessionStats>,
        thisWeekStartAt: Long,
        zone: ZoneId,
        requiredSessions: Int,
    ): Baseline {
        val windowStartAt = Instant.ofEpochMilli(thisWeekStartAt).atZone(zone)
            .minusWeeks(WEEKS.toLong())
            .toInstant().toEpochMilli()
        val valid = sessions.filter { it.countsInStats && it.startAt >= windowStartAt && it.startAt < thisWeekStartAt }
        if (valid.size < requiredSessions) return Baseline.Inactive(neededSessions = requiredSessions - valid.size)

        val totalSec = valid.sumOf { it.tTotalSec.toLong() }
        val focusSec = valid.sumOf { it.tFocusSec.toLong() }
        return Baseline.Active(
            densityPct = if (totalSec == 0L) 0f else focusSec * 100f / totalSec,
            interruptionsPerSession = valid.sumOf { it.interruptionCount }.toFloat() / valid.size,
            lfsSec = (valid.sumOf { it.lfsSec.toLong() } / valid.size).toInt(),
            weeklyFocusSec = (focusSec / WEEKS).toInt(),
            sessionSec = (totalSec / valid.size).toInt(),
            sessionCount = valid.size,
        )
    }
}
