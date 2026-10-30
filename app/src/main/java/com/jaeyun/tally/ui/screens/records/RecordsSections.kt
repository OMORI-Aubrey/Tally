package com.jaeyun.tally.ui.screens.records

import com.jaeyun.tally.data.repository.WeeklyGoals
import com.jaeyun.tally.data.room.AppSegment
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.countsInStats
import com.jaeyun.tally.data.room.toSessionStats
import com.jaeyun.tally.domain.baseline.BaselineCalculator
import com.jaeyun.tally.domain.goal.GoalSuggester
import com.jaeyun.tally.domain.model.Baseline
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.GoalMetric
import com.jaeyun.tally.domain.model.PseudoPackage
import com.jaeyun.tally.domain.model.Verdict
import com.jaeyun.tally.domain.model.WeekStats
import com.jaeyun.tally.domain.verdict.VerdictResolver
import com.jaeyun.tally.ui.components.records.TopApp
import com.jaeyun.tally.ui.components.records.Trend
import com.jaeyun.tally.util.WeekRange
import com.jaeyun.tally.util.weekRangeOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** 판정 섹션 (§8.4 (1)). 늘 이번 주를 본다. 주 선택과 관계없다 */
sealed interface VerdictSection {
    /** 기준선 미활성: 섹션 전체를 "기준선 계산에 세션 N개가 더 필요합니다"로 바꾼다 (§5.3) */
    data class Inactive(val neededSessions: Int) : VerdictSection

    /**
     * @param badge 지난주 목표를 달성했을 때만. 미달성은 표시하지 않는다 (§7.3)
     * @param verdict 이번 주 판정. 이번 주에 통계 세션이 아직 없으면 null이고 판정 대신 안내 한 줄을 쓴다
     * @param goal 이번 주 목표. 목표를 만들 때 기준선이 미활성이었거나 지난주 통계 세션이 없었으면 null (PLAN.md §4)
     */
    data class Active(val badge: GoalBadge?, val verdict: VerdictLine?, val goal: GoalLine?) : VerdictSection
}

/**
 * 이번 주 판정과 그 근거 (§7.2.2)
 *
 * @param longestSec 이번 주 세션 `LFS`의 최댓값. 부정적인 판정 문구가 먼저 말하는 성취다(§5.6)
 * @param averageLfsSec 이번 주 세션 `LFS`의 평균. 기준선(세션 평균)과 견준다
 */
data class VerdictLine(
    val verdict: Verdict,
    val clamped: Boolean,
    val longestSec: Int,
    val averageLfsSec: Int,
    val baselineLfsSec: Int,
    val densityPct: Int,
    val baselineDensityPct: Int,
    val focusSec: Int,
    val baselineWeeklyFocusSec: Int,
)

/**
 * 이번 주 목표 (§7.3). 단위는 [GoalMetric]을 따른다
 *
 * @param fromValue 목표를 만들 때 본 지난주 값. 지난주 통계 세션이 이제 없으면 null
 * @param stretch 4순위라 기준선보다 5% 앞선다. 아니면 기준선 복귀
 */
data class GoalLine(val metric: GoalMetric, val fromValue: Float?, val targetValue: Float, val stretch: Boolean)

/**
 * 지난주 목표 달성 배지 (§7.3)
 *
 * @param fromValue 그 목표를 만들 때 본 지지난주 값. 지지난주 통계 세션이 이제 없으면 null
 */
data class GoalBadge(val metric: GoalMetric, val fromValue: Float?, val targetValue: Float, val achievedValue: Float)

/**
 * 이번 주 섹션 (§8.4 (2)). 주 선택으로 지난 주를 볼 수 있다
 *
 * @param monday 보고 있는 주의 월요일
 * @param firstWeekGap 1주차 격차. 첫 기록의 주가 이번 주이고 그 주를 볼 때만 (§5.8)
 * @param numbers 통계 세션으로 낸 숫자. 그 주에 통계 세션이 없으면 null
 * @param days 그 주의 세션 목록(통계 제외 세션 포함). 비어 있으면 섹션 자리에 안내 한 줄만 둔다
 */
data class WeekSection(
    val monday: LocalDate,
    val isThisWeek: Boolean,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
    val firstWeekGap: FirstWeekGap?,
    val numbers: WeekNumbers?,
    val topApps: List<TopApp>,
    val days: List<SessionDay>,
)

/** 1주차 격차 (§5.8). 하루 평균은 순공 합을 오늘까지 지난 날 수로 나눈다 */
data class FirstWeekGap(val selfReportedDailyMin: Int, val dailyFocusSec: Int)

/**
 * 지난주(보고 있는 주의 앞 주)와 견준 값. [previous]가 null이면 비교값을 감춘다 — 앞 주에 통계 세션이 없거나 그 값이 0이다
 * (와이어프레임 §5, §8.5.1)
 */
data class Compared<T>(val value: T, val previous: T? = null, val trend: Trend = Trend.SAME)

/**
 * 이번 주 섹션의 숫자. 모두 통계에 셀 세션(`countsInStats`)만 센다
 *
 * @param longestSec 세션 `LFS`의 최댓값 (주간 요약 줄의 최장과 같은 정의)
 * @param sittingSec 공부(착석) 시간 합
 * @param averageSessionSec 공부 시간의 평균 (기준선 평균 세션 길이와 같은 정의, PLAN.md §4)
 * @param dailyFocusSec 월요일부터 일요일까지 그날 시작한 세션의 순공 합
 */
data class WeekNumbers(
    val longestSec: Compared<Int>,
    val interruptionsPerSession: Compared<Float>,
    val densityPct: Compared<Int>,
    val focusSec: Int,
    val sittingSec: Int,
    val distractSec: Int,
    val sessionCount: Int,
    val averageSessionSec: Int,
    val dailyFocusSec: List<Int>,
)

/** [week]에 시작한 세션 */
internal fun List<StudySession>.startedIn(week: WeekRange): List<StudySession> = filter { it.startAt in week }

/** [week]에 시작한 통계 세션으로 [WeekStats]. 셀 세션이 없으면 null */
private fun List<StudySession>.weekStats(week: WeekRange): WeekStats? =
    WeekStats.of(startedIn(week).map { it.toSessionStats() })

/**
 * 보고 있는 주. [selectedWeekStart]가 null이면 이번 주이고, 첫 기록의 주와 이번 주 사이로 맞춘다
 * (그사이 기록을 지웠거나 날짜가 바뀐 경우).
 */
internal fun selectedWeek(sessions: List<StudySession>, selectedWeekStart: Long?, now: Long, zone: ZoneId): WeekRange {
    val thisWeek = weekRangeOf(now, zone)
    val start = selectedWeekStart ?: return thisWeek
    val firstWeek = sessions.minOfOrNull { it.startAt }?.let { weekRangeOf(it, zone) } ?: return thisWeek
    return when {
        start >= thisWeek.startAt -> thisWeek
        start < firstWeek.startAt -> firstWeek
        else -> weekRangeOf(start, zone)
    }
}

/**
 * 판정 섹션. 기준선은 이번 주를 뺀 직전 4주로 계산한다. 목표와 배지는 기록 탭에 들어올 때 갱신한 [goals]에서 이번 주·지난주 것만 쓴다
 * (주가 바뀌기 전에 읽은 값이면 버린다).
 */
internal fun buildVerdictSection(
    sessions: List<StudySession>,
    goals: WeeklyGoals?,
    now: Long,
    zone: ZoneId,
    requiredSessions: Int,
): VerdictSection {
    val thisWeek = weekRangeOf(now, zone)
    val lastWeek = weekRangeOf(thisWeek.startAt - 1, zone)
    val weekBeforeLast = weekRangeOf(lastWeek.startAt - 1, zone)

    val baseline = when (val b = BaselineCalculator.calculate(sessions.map { it.toSessionStats() }, thisWeek.startAt, zone, requiredSessions)) {
        is Baseline.Inactive -> return VerdictSection.Inactive(b.neededSessions)
        is Baseline.Active -> b
    }

    val thisWeekSessions = sessions.startedIn(thisWeek).filter { it.countsInStats }
    val verdict = WeekStats.of(thisWeekSessions.map { it.toSessionStats() })?.let { stats ->
        val sittingSec = thisWeekSessions.sumOf { it.tTotalSec.toLong() }.toInt()
        // 판정은 기준선과 같은 기간 합 밀도로 하고, 화면에는 버린 정수로 쓴다
        val density = if (sittingSec == 0) 0f else stats.focusSec * 100f / sittingSec
        val result = VerdictResolver.resolve(density, stats.focusSec, baseline)
        VerdictLine(
            verdict = result.verdict,
            clamped = result.clamped,
            longestSec = thisWeekSessions.maxOf { it.lfsSec },
            averageLfsSec = stats.lfsSec,
            baselineLfsSec = baseline.lfsSec,
            densityPct = densityPct(stats.focusSec, sittingSec),
            baselineDensityPct = baseline.densityPct.toInt(),
            focusSec = stats.focusSec,
            baselineWeeklyFocusSec = baseline.weeklyFocusSec,
        )
    }

    val goal = goals?.current?.takeIf { it.weekStartAt == thisWeek.startAt }?.let { current ->
        GoalLine(
            metric = current.metric,
            fromValue = sessions.weekStats(lastWeek)?.let { GoalSuggester.valueOf(current.metric, it) },
            targetValue = current.targetValue,
            stretch = current.targetValue != current.baselineValue,
        )
    }
    val badge = goals?.achievedLastWeek?.takeIf { it.weekStartAt == lastWeek.startAt }?.let { achieved ->
        val achievedValue = achieved.achievedValue ?: return@let null
        GoalBadge(
            metric = achieved.metric,
            fromValue = sessions.weekStats(weekBeforeLast)?.let { GoalSuggester.valueOf(achieved.metric, it) },
            targetValue = achieved.targetValue,
            achievedValue = achievedValue,
        )
    }
    return VerdictSection.Active(badge = badge, verdict = verdict, goal = goal)
}

/**
 * 이번 주 섹션. [week]는 [selectedWeek]로 정한 보고 있는 주다.
 *
 * - 비교값: 앞 주에 통계 세션이 없거나 그 값이 0이면 감춘다
 * - 1주차 격차: 첫 기록의 주가 이번 주이고 그 주를 보고 있고, 온보딩 값과 순공이 있을 때만(§5.8). 그 주가 지나면 다시 보이지 않는다
 */
internal fun buildWeekSection(
    sessions: List<StudySession>,
    week: WeekRange,
    selfReportedDailyMin: Int?,
    topApps: List<TopApp>,
    now: Long,
    zone: ZoneId,
): WeekSection {
    val thisWeek = weekRangeOf(now, zone)
    val firstWeek = sessions.minOfOrNull { it.startAt }?.let { weekRangeOf(it, zone) } ?: thisWeek
    val monday = Instant.ofEpochMilli(week.startAt).atZone(zone).toLocalDate()
    val isThisWeek = week.startAt == thisWeek.startAt

    val weekSessions = sessions.startedIn(week)
    val numbers = weekNumbers(
        sessions = weekSessions.filter { it.countsInStats },
        previous = sessions.startedIn(weekRangeOf(week.startAt - 1, zone)).filter { it.countsInStats },
        monday = monday,
        zone = zone,
    )

    val firstWeekGap = if (isThisWeek && firstWeek.startAt == thisWeek.startAt && selfReportedDailyMin != null) {
        numbers?.focusSec?.takeIf { it > 0 }?.let { focusSec ->
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val daysSoFar = ChronoUnit.DAYS.between(monday, today).toInt() + 1
            FirstWeekGap(selfReportedDailyMin, focusSec / daysSoFar)
        }
    } else {
        null
    }

    return WeekSection(
        monday = monday,
        isThisWeek = isThisWeek,
        canGoPrevious = week.startAt > firstWeek.startAt,
        canGoNext = week.startAt < thisWeek.startAt,
        firstWeekGap = firstWeekGap,
        numbers = numbers,
        topApps = topApps,
        days = groupByDay(weekSessions, zone),
    )
}

/** 통계 세션이 없으면 null */
private fun weekNumbers(sessions: List<StudySession>, previous: List<StudySession>, monday: LocalDate, zone: ZoneId): WeekNumbers? {
    val stats = WeekStats.of(sessions.map { it.toSessionStats() }) ?: return null
    val previousStats = WeekStats.of(previous.map { it.toSessionStats() })
    val sittingSec = sessions.sumOf { it.tTotalSec.toLong() }.toInt()
    val previousSittingSec = previous.sumOf { it.tTotalSec.toLong() }.toInt()

    val longest = sessions.maxOf { it.lfsSec }
    val previousLongest = previous.maxOfOrNull { it.lfsSec }?.takeIf { it > 0 }
    val previousInterruptions = previousStats?.interruptionsPerSession?.takeIf { it > 0f }
    val density = densityPct(stats.focusSec, sittingSec)
    val previousDensity = previousStats?.let { densityPct(it.focusSec, previousSittingSec) }?.takeIf { it > 0 }

    val dailyFocusSec = MutableList(7) { 0 }
    sessions.forEach { session ->
        val day = Instant.ofEpochMilli(session.startAt).atZone(zone).toLocalDate()
        dailyFocusSec[ChronoUnit.DAYS.between(monday, day).toInt()] += session.tFocusSec
    }

    return WeekNumbers(
        longestSec = Compared(longest, previousLongest, previousLongest?.let { trend(shownSeconds(longest), shownSeconds(it)) } ?: Trend.SAME),
        interruptionsPerSession = Compared(
            stats.interruptionsPerSession,
            previousInterruptions,
            previousInterruptions?.let { trend(tenths(stats.interruptionsPerSession), tenths(it)) } ?: Trend.SAME,
        ),
        densityPct = Compared(density, previousDensity, previousDensity?.let { trend(density, it) } ?: Trend.SAME),
        focusSec = stats.focusSec,
        sittingSec = sittingSec,
        distractSec = sessions.sumOf { it.tDistSec },
        sessionCount = sessions.size,
        averageSessionSec = stats.sessionSec,
        dailyFocusSec = dailyFocusSec,
    )
}

/** 집중 밀도(%). 기간 합으로 계산하고 정수로 버린다 */
private fun densityPct(focusSec: Int, sittingSec: Int): Int = if (sittingSec == 0) 0 else (focusSec * 100L / sittingSec).toInt()

/** 화면에 보이는 자릿수의 초. 1분 이상은 분까지만 보인다(PLAN.md §4 시간 표기) */
internal fun shownSeconds(seconds: Int): Int = if (seconds < 60) seconds else seconds - seconds % 60

/** 소수 첫째 자리까지 보이는 수를 10배 한 정수 */
internal fun tenths(value: Float): Int = (value * 10).roundToInt()

internal fun trend(value: Int, previous: Int): Trend = when {
    value > previous -> Trend.UP
    value < previous -> Trend.DOWN
    else -> Trend.SAME
}

/** 공부 중 켠 앱 상위 몇 개까지 (§8.4) */
internal const val MAX_TOP_APPS = 5

/**
 * 공부 중 켠 앱 (§8.4 (2)). 딴짓 구간만 앱별로 묶어 횟수(구간 수)와 시간을 센다. 화면만 켠 구간은 앱이 아니라 뺀다.
 * 시간이 긴 순서, 같으면 횟수가 많은 순서로 [MAX_TOP_APPS]개까지. 이름을 모르는 앱은 패키지 이름을 쓴다.
 *
 * 같은 분류의 구간을 합칠 때 가장 오래 머문 앱을 대표로 남기므로(§6.2) 앱별 값은 근삿값이다.
 */
internal fun topApps(segments: List<AppSegment>, appLabels: Map<String, String>): List<TopApp> =
    segments
        .filter { it.category == Category.DISTRACT && it.packageName != PseudoPackage.SCREEN_ON_UNKNOWN }
        .groupBy { it.packageName }
        .map { (pkg, appSegments) ->
            TopApp(
                appLabel = appLabels[pkg] ?: pkg,
                count = appSegments.size,
                durationSec = (appSegments.sumOf { it.endAt - it.startAt } / 1000).toInt(),
            )
        }
        .sortedWith(compareByDescending<TopApp> { it.durationSec }.thenByDescending { it.count })
        .take(MAX_TOP_APPS)
