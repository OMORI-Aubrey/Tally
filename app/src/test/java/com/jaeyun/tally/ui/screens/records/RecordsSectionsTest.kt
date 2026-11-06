package com.jaeyun.tally.ui.screens.records

import com.jaeyun.tally.data.repository.WeeklyGoals
import com.jaeyun.tally.data.room.AppSegment
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.WeeklyGoal
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.DistractApp
import com.jaeyun.tally.domain.model.GoalMetric
import com.jaeyun.tally.domain.model.PseudoPackage
import com.jaeyun.tally.domain.model.Verdict
import com.jaeyun.tally.domain.model.WeeklyReportInput
import com.jaeyun.tally.ui.components.records.TopApp
import com.jaeyun.tally.ui.components.records.Trend
import com.jaeyun.tally.util.weekRangeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val SEOUL = ZoneId.of("Asia/Seoul")

private fun at(date: String, time: String = "10:00"): Long =
    LocalDateTime.parse("${date}T$time").atZone(SEOUL).toInstant().toEpochMilli()

/** 2026-10-29 (목) 12:00. 이번 주는 10-26(월)부터 */
private val NOW = at("2026-10-29", "12:00")
private val THIS_WEEK = weekRangeOf(NOW, SEOUL)
private val LAST_WEEK = weekRangeOf(THIS_WEEK.startAt - 1, SEOUL)

private var nextId = 1L

/** 통계에 셀 세션. 기본은 1시간에 순공 45분(밀도 75%), LFS 20분, 이탈 3회 */
private fun session(
    startAt: Long,
    totalMin: Int = 60,
    focusMin: Int = 45,
    lfsSec: Int = 20 * 60,
    interruptions: Int = 3,
    valid: Boolean = true,
    timeline: Boolean = true,
    perceivedMin: Int? = null,
) = StudySession(
    id = nextId++,
    subjectName = null,
    startAt = startAt,
    endAt = startAt + totalMin * 60_000L,
    perceivedFocusMin = perceivedMin,
    tTotalSec = totalMin * 60,
    tFocusSec = focusMin * 60,
    tDistSec = (totalMin - focusMin) * 60,
    lfsSec = lfsSec,
    densityPct = focusMin * 100f / totalMin,
    interruptionCount = interruptions,
    isValidForStats = valid,
    hasTimeline = timeline,
    createdAt = startAt,
)

/** 이번 주를 뺀 직전 4주(9-28 ~ 10-25)에 이틀마다 기본 세션 12개: 기준선 밀도 75%, LFS 20분, 주간 순공 135분 */
private fun baselineSessions(): List<StudySession> =
    List(12) { session(at(LocalDate.of(2026, 10, 25).minusDays(it * 2L).toString())) }

private fun verdictSection(sessions: List<StudySession>, goals: WeeklyGoals? = null, required: Int = 12) =
    buildVerdictSection(sessions, goals, NOW, SEOUL, required)

private fun weekSection(
    sessions: List<StudySession>,
    weekStart: Long = THIS_WEEK.startAt,
    selfReportedDailyMin: Int? = null,
) = buildWeekSection(sessions, weekRangeOf(weekStart, SEOUL), selfReportedDailyMin, emptyList(), emptyList(), NOW, SEOUL)

private fun segment(pkg: String, millis: Long, category: Category = Category.DISTRACT) =
    AppSegment(sessionId = 1, packageName = pkg, category = category, startAt = 0, endAt = millis)

private fun cumulativeSection(sessions: List<StudySession>) = buildCumulativeSection(sessions, NOW, SEOUL)

/** [first]부터 하루씩 [count]개 날짜(`2026-10-01` 꼴) */
private fun days(first: String, count: Int): List<String> = List(count) { LocalDate.parse(first).plusDays(it.toLong()).toString() }

/** 10월 1일부터 하루 하나씩 10:00에 시작한 기본 세션 [count]개 */
private fun octoberSessions(count: Int): List<StudySession> = days("2026-10-01", count).map { session(at(it)) }

class RecordsSectionsTest {

    // 보고 있는 주

    @Test
    fun `고른 주가 없으면 이번 주`() {
        assertEquals(THIS_WEEK, selectedWeek(baselineSessions(), null, NOW, SEOUL))
    }

    @Test
    fun `고른 주는 첫 기록의 주와 이번 주 사이로 맞춘다`() {
        val sessions = listOf(session(at("2026-10-14")))

        assertEquals(LAST_WEEK, selectedWeek(sessions, LAST_WEEK.startAt, NOW, SEOUL))
        assertEquals(weekRangeOf(at("2026-10-14"), SEOUL), selectedWeek(sessions, at("2026-09-01"), NOW, SEOUL))
        assertEquals(THIS_WEEK, selectedWeek(sessions, THIS_WEEK.endAt, NOW, SEOUL))
    }

    // 판정 섹션

    @Test
    fun `기준선이 미활성이면 섹션 전체가 필요한 세션 수 안내다`() {
        assertEquals(VerdictSection.Inactive(neededSessions = 1), verdictSection(baselineSessions().drop(1)))
    }

    @Test
    fun `이번 주 밀도를 기준선과 견줘 판정하고 근거를 함께 낸다`() {
        // 이번 주 밀도 90%: 기준선 75%보다 +20% → 가장 잘한 주. 순공 108분은 기준선 135분의 0.7 이상이라 가드 없음
        val sessions = baselineSessions() +
            session(at("2026-10-26"), focusMin = 54, lfsSec = 47 * 60) +
            session(at("2026-10-27"), focusMin = 54, lfsSec = 33 * 60)

        val section = verdictSection(sessions) as VerdictSection.Active

        assertEquals(
            VerdictLine(
                verdict = Verdict.BEST,
                clamped = false,
                longestSec = 47 * 60,
                averageLfsSec = 40 * 60,
                baselineLfsSec = 20 * 60,
                densityPct = 90,
                baselineDensityPct = 75,
                focusSec = 108 * 60,
                baselineWeeklyFocusSec = 135 * 60,
            ),
            section.verdict,
        )
    }

    @Test
    fun `순공 총량이 기준선의 70%보다 적으면 판정을 내리고 클램프로 표시한다`() {
        val section = verdictSection(baselineSessions() + session(at("2026-10-26"), focusMin = 54)) as VerdictSection.Active

        assertEquals(Verdict.STEADY, section.verdict?.verdict)
        assertEquals(true, section.verdict?.clamped)
    }

    @Test
    fun `이번 주 통계 세션이 아직 없으면 판정 없이 안내한다`() {
        val sessions = baselineSessions() + session(at("2026-10-26"), totalMin = 2, focusMin = 2, valid = false)

        val section = verdictSection(sessions) as VerdictSection.Active

        assertNull(section.verdict)
    }

    @Test
    fun `완화 조건이면 세션 6개로 판정한다`() {
        val section = verdictSection(baselineSessions().take(6) + session(at("2026-10-26")), required = 6)

        assertTrue(section is VerdictSection.Active)
    }

    @Test
    fun `이번 주 목표는 지난주 값에서 출발하고 4순위면 stretch다`() {
        val goals = WeeklyGoals(
            current = WeeklyGoal(THIS_WEEK.startAt, GoalMetric.INTERRUPTION, targetValue = 2.85f, baselineValue = 3f),
            achievedLastWeek = null,
        )

        val section = verdictSection(baselineSessions(), goals) as VerdictSection.Active

        assertEquals(GoalLine(GoalMetric.INTERRUPTION, fromValue = 3f, targetValue = 2.85f, stretch = true), section.goal)
        assertNull(section.badge)
    }

    @Test
    fun `기준선 복귀 목표는 stretch가 아니다`() {
        val goals = WeeklyGoals(WeeklyGoal(THIS_WEEK.startAt, GoalMetric.LFS, targetValue = 20f, baselineValue = 20f), null)

        val goal = (verdictSection(baselineSessions(), goals) as VerdictSection.Active).goal

        assertEquals(GoalLine(GoalMetric.LFS, fromValue = 20f, targetValue = 20f, stretch = false), goal)
    }

    @Test
    fun `지난주 목표를 달성했으면 지지난주 값에서 실제 값으로 배지를 낸다`() {
        val goals = WeeklyGoals(
            current = null,
            achievedLastWeek = WeeklyGoal(
                LAST_WEEK.startAt, GoalMetric.LFS, targetValue = 20f, baselineValue = 20f, achievedValue = 25f, isAchieved = true,
            ),
        )

        val badge = (verdictSection(baselineSessions(), goals) as VerdictSection.Active).badge

        assertEquals(GoalBadge(GoalMetric.LFS, fromValue = 20f, targetValue = 20f, achievedValue = 25f), badge)
    }

    @Test
    fun `지지난주 통계 세션이 없으면 배지의 출발 값을 모른다`() {
        // 지지난주(10-12 ~ 10-18) 세션을 빼도 기준선 12개는 유지한다
        val sessions = baselineSessions().filterNot { it.startAt in weekRangeOf(at("2026-10-14"), SEOUL) } +
            List(4) { session(at("2026-10-01")) }
        val goals = WeeklyGoals(
            null,
            WeeklyGoal(LAST_WEEK.startAt, GoalMetric.LFS, 20f, 20f, achievedValue = 25f, isAchieved = true),
        )

        val badge = (verdictSection(sessions, goals) as VerdictSection.Active).badge

        assertNotNull(badge)
        assertNull(badge?.fromValue)
    }

    @Test
    fun `주가 바뀌기 전에 읽은 목표와 배지는 쓰지 않는다`() {
        val goals = WeeklyGoals(
            current = WeeklyGoal(LAST_WEEK.startAt, GoalMetric.LFS, 20f, 20f),
            achievedLastWeek = WeeklyGoal(THIS_WEEK.startAt - 14 * 86_400_000L, GoalMetric.LFS, 20f, 20f, 25f, true),
        )

        val section = verdictSection(baselineSessions(), goals) as VerdictSection.Active

        assertNull(section.goal)
        assertNull(section.badge)
    }

    // 이번 주 섹션

    @Test
    fun `이번 주 숫자는 통계 세션으로 내고 지난주와 견준다`() {
        val sessions = listOf(
            session(at("2026-10-20"), lfsSec = 38 * 60, interruptions = 3),
            session(at("2026-10-26"), focusMin = 50, lfsSec = 47 * 60, interruptions = 4),
            session(at("2026-10-28"), totalMin = 90, focusMin = 60, lfsSec = 30 * 60, interruptions = 5),
            session(at("2026-10-28", "20:00"), totalMin = 2, focusMin = 2, valid = false),
        )

        val numbers = weekSection(sessions).numbers!!

        assertEquals(Compared(47 * 60, previous = 38 * 60, trend = Trend.UP), numbers.longestSec)
        assertEquals(Compared(4.5f, previous = 3f, trend = Trend.UP), numbers.interruptionsPerSession)
        // 순공 110분 / 공부 150분 = 73%, 지난주 75%
        assertEquals(Compared(73, previous = 75, trend = Trend.DOWN), numbers.densityPct)
        assertEquals(110 * 60, numbers.focusSec)
        assertEquals(150 * 60, numbers.sittingSec)
        assertEquals(40 * 60, numbers.distractSec)
        assertEquals(2, numbers.sessionCount)
        assertEquals(75 * 60, numbers.averageSessionSec)
        assertEquals(listOf(50 * 60, 0, 60 * 60, 0, 0, 0, 0), numbers.dailyFocusSec)
    }

    @Test
    fun `지난주 통계 세션이 없으면 비교값을 감춘다`() {
        val numbers = weekSection(listOf(session(at("2026-10-26")))).numbers!!

        assertNull(numbers.longestSec.previous)
        assertNull(numbers.interruptionsPerSession.previous)
        assertNull(numbers.densityPct.previous)
    }

    @Test
    fun `지난주 값이 0이면 그 비교값만 감춘다`() {
        val sessions = listOf(session(at("2026-10-20"), interruptions = 0), session(at("2026-10-26"), interruptions = 2))

        val numbers = weekSection(sessions).numbers!!

        assertNull(numbers.interruptionsPerSession.previous)
        assertEquals(20 * 60, numbers.longestSec.previous)
    }

    @Test
    fun `보이는 자릿수가 같으면 같은 것으로 본다`() {
        // 47분 30초와 47분 10초는 둘 다 47분으로 보인다
        val sessions = listOf(session(at("2026-10-20"), lfsSec = 47 * 60 + 10), session(at("2026-10-26"), lfsSec = 47 * 60 + 30))

        assertEquals(Trend.SAME, weekSection(sessions).numbers!!.longestSec.trend)
    }

    @Test
    fun `통계 세션이 없는 주는 숫자 없이 세션 목록만 있다`() {
        val section = weekSection(listOf(session(at("2026-10-26"), totalMin = 2, focusMin = 2, valid = false)))

        assertNull(section.numbers)
        assertEquals(1, section.days.single().sessions.size)
    }

    @Test
    fun `세션이 없는 주는 세션 목록이 비어 있고 화살표는 남는다`() {
        val section = weekSection(listOf(session(at("2026-10-14"))))

        assertTrue(section.days.isEmpty())
        assertTrue(section.canGoPrevious)
        assertFalse(section.canGoNext)
    }

    @Test
    fun `지난 주를 보면 다음 주로 갈 수 있다`() {
        val section = weekSection(listOf(session(at("2026-10-14")), session(at("2026-10-20"))), weekStart = LAST_WEEK.startAt)

        assertFalse(section.isThisWeek)
        assertEquals(LocalDate.of(2026, 10, 19), section.monday)
        assertTrue(section.canGoPrevious)
        assertTrue(section.canGoNext)
    }

    @Test
    fun `첫 기록의 주에서는 앞으로 갈 수 없다`() {
        assertFalse(weekSection(listOf(session(at("2026-10-26")))).canGoPrevious)
    }

    @Test
    fun `첫 주에는 온보딩 값과 오늘까지의 하루 평균 순공을 나란히 둔다`() {
        // 월~목 4일, 순공 90분 → 하루 평균 22분 30초
        val sessions = listOf(session(at("2026-10-26")), session(at("2026-10-28")))

        assertEquals(FirstWeekGap(selfReportedDailyMin = 180, dailyFocusSec = 90 * 60 / 4), weekSection(sessions, selfReportedDailyMin = 180).firstWeekGap)
    }

    @Test
    fun `1주차 격차는 온보딩 값이 없거나 첫 주가 지나면 없다`() {
        assertNull(weekSection(listOf(session(at("2026-10-26")))).firstWeekGap)
        assertNull(weekSection(listOf(session(at("2026-10-20")), session(at("2026-10-26"))), selfReportedDailyMin = 180).firstWeekGap)
    }

    // 공부 중 켠 앱

    @Test
    fun `딴짓 구간을 앱별로 묶어 시간이 긴 순서로 센다`() {
        val apps = topApps(
            segments = listOf(
                segment("com.kakao.talk", 60_000),
                segment("com.kakao.talk", 90_000),
                segment("com.google.android.youtube", 300_000),
                segment(PseudoPackage.SCREEN_ON_UNKNOWN, 600_000),
                segment("com.google.android.calculator", 600_000, Category.ALLOWED),
            ),
            appLabels = mapOf("com.kakao.talk" to "카카오톡", "com.google.android.youtube" to "YouTube"),
        )

        assertEquals(listOf(TopApp("YouTube", 1, 300), TopApp("카카오톡", 2, 150)), apps)
    }

    @Test
    fun `시간이 같으면 횟수가 많은 앱이 앞이고 다섯 개까지만`() {
        val segments = listOf("a", "b", "c", "d", "e", "f").map { segment(it, 60_000) } + segment("g", 30_000) + segment("g", 30_000)

        val apps = topApps(segments, emptyMap())

        assertEquals(MAX_TOP_APPS, apps.size)
        assertEquals(TopApp("g", 2, 60), apps.first())
    }

    // 요약 문단의 사실

    @Test
    fun `딴짓 앱은 다섯 개로 자르지 않고 모두 센다`() {
        val segments = listOf("a", "b", "c", "d", "e", "f").map { segment(it, 60_000) } + segment(PseudoPackage.SCREEN_ON_UNKNOWN, 60_000)

        assertEquals(6, distractApps(segments, emptyMap()).size)
    }

    @Test
    fun `요약 사실은 그 주 통계 세션으로 내고 앞 주와 앞선 기록을 함께 담는다`() {
        val apps = listOf(DistractApp("카카오톡", 2, 120))
        val sessions = listOf(
            session(at("2026-10-26"), totalMin = 60, focusMin = 45, lfsSec = 30 * 60, interruptions = 3),
            session(at("2026-10-27"), totalMin = 90, focusMin = 80, lfsSec = 50 * 60, interruptions = 1),
            session(at("2026-10-28"), totalMin = 2, focusMin = 2, valid = false), // 통계 제외
            session(at("2026-10-20"), focusMin = 40, lfsSec = 40 * 60), // 지난주
            session(at("2026-10-01"), lfsSec = 45 * 60), // 앞선 기록
            session(at("2026-11-03"), lfsSec = 70 * 60), // 다음 주는 앞선 기록이 아니다
        )

        val input = weeklyReportInput(sessions, THIS_WEEK, apps, SEOUL)

        assertEquals(
            WeeklyReportInput(
                sessionCount = 2,
                sittingSec = 150 * 60,
                focusSec = 125 * 60,
                previousFocusSec = 40 * 60,
                longestSec = 50 * 60,
                previousBestLfsSec = 45 * 60,
                interruptionCount = 4,
                distractApps = apps,
            ),
            input,
        )
    }

    @Test
    fun `앞 주 세션과 앞선 기록이 없으면 비워 두고 그 주 통계 세션이 없으면 요약을 두지 않는다`() {
        val firstWeek = listOf(session(at("2026-10-26")), session(at("2026-10-19"), valid = false))

        val input = weeklyReportInput(firstWeek, THIS_WEEK, emptyList(), SEOUL)!!
        assertNull(input.previousFocusSec)
        assertNull(input.previousBestLfsSec)

        // 앞선 기록이 0초뿐이면 견줄 기록이 없다
        assertNull(weeklyReportInput(firstWeek + session(at("2026-10-19"), lfsSec = 0), THIS_WEEK, emptyList(), SEOUL)!!.previousBestLfsSec)

        assertNull(weeklyReportInput(listOf(session(at("2026-10-26"), timeline = false)), THIS_WEEK, emptyList(), SEOUL))
    }

    // 누적 섹션

    @Test
    fun `누적 섹션은 통계 세션 15개부터 보인다`() {
        val sessions = octoberSessions(14) + session(at("2026-10-20"), valid = false) + session(at("2026-10-21"), timeline = false)

        assertNull(cumulativeSection(sessions))
        assertNotNull(cumulativeSection(sessions + session(at("2026-10-22"))))
    }

    @Test
    fun `이번 달과 지난달 순공은 시작 시각의 로컬 날짜로 나눈다`() {
        val sessions = days("2026-10-02", 9).map { session(at(it)) } +
            session(at("2026-10-01", "00:30")) + // UTC로는 9월 30일
            days("2026-09-01", 5).map { session(at(it)) } +
            session(at("2026-10-20"), valid = false)

        val section = cumulativeSection(sessions)!!

        assertEquals(10 * 45 * 60, section.thisMonthFocusSec)
        assertEquals(5 * 45 * 60, section.lastMonthFocusSec)
        assertEquals(15 * 45 * 60, section.totalFocusSec)
    }

    @Test
    fun `지난달 기록이 없으면 비교값을 감춘다`() {
        val sessions = octoberSessions(15) + days("2026-08-01", 3).map { session(at(it)) }

        val section = cumulativeSection(sessions)!!

        assertNull(section.lastMonthFocusSec)
        assertEquals(18 * 45 * 60, section.totalFocusSec)
    }

    @Test
    fun `최고·최저 집중 시간대는 5세션 이상인 시간대에서 고른다`() {
        val sessions = days("2026-10-01", 5).map { session(at(it, "14:00"), focusMin = 54) } + // 90%
            days("2026-10-06", 5).map { session(at(it, "22:00"), focusMin = 30) } + // 50%
            days("2026-10-11", 4).map { session(at(it, "10:00"), focusMin = 60) } + // 100%지만 4세션
            session(at("2026-10-15", "18:00"))

        val hours = cumulativeSection(sessions)!!.focusHours

        assertEquals(FocusHours(best = HourBand(14, 90, 5), worst = HourBand(22, 50, 5)), hours)
    }

    @Test
    fun `시간대 밀도는 세션 평균이 아니라 기간 합으로 낸다`() {
        val sessions = days("2026-10-01", 4).map { session(at(it, "14:00")) } + // 60분 중 45분
            session(at("2026-10-05", "15:00"), totalMin = 120, focusMin = 120) +
            days("2026-10-06", 10).map { session(at(it, "20:00"), focusMin = 30) }

        // 300분 / 360분 = 83%. 세션 밀도 평균이면 80%다
        assertEquals(HourBand(14, 83, 5), cumulativeSection(sessions)!!.focusHours!!.best)
    }

    @Test
    fun `자정을 넘긴 세션은 시작한 시간대에 들어간다`() {
        val sessions = days("2026-10-01", 5).map { session(at(it, "23:30"), totalMin = 90) } + // 50%
            days("2026-10-06", 10).map { session(at(it, "10:00")) } // 75%

        assertEquals(
            FocusHours(best = HourBand(10, 75, 10), worst = HourBand(22, 50, 5)),
            cumulativeSection(sessions)!!.focusHours,
        )
    }

    @Test
    fun `후보 시간대가 하나뿐이거나 최고와 최저가 같으면 시간대를 감춘다`() {
        assertNull(cumulativeSection(octoberSessions(15))!!.focusHours)

        val sameDensity = days("2026-10-01", 5).map { session(at(it, "14:00")) } + days("2026-10-06", 10).map { session(at(it, "20:00")) }
        assertNull(cumulativeSection(sameDensity)!!.focusHours)
    }

    @Test
    fun `밀도가 같으면 세션이 많은 시간대를 고른다`() {
        val sessions = days("2026-10-01", 5).map { session(at(it, "14:00"), focusMin = 54) } +
            days("2026-10-06", 6).map { session(at(it, "16:00"), focusMin = 54) } +
            days("2026-10-12", 5).map { session(at(it, "22:00"), focusMin = 30) }

        assertEquals(HourBand(16, 90, 6), cumulativeSection(sessions)!!.focusHours!!.best)
    }

    @Test
    fun `최장 기록은 가장 긴 LFS와 그날이고 같으면 먼저 세운 기록이다`() {
        val sessions = octoberSessions(13) +
            session(at("2026-10-20"), lfsSec = 47 * 60) +
            session(at("2026-10-05", "15:00"), lfsSec = 47 * 60) +
            session(at("2026-10-25"), lfsSec = 60 * 60, valid = false)

        assertEquals(LongestRecord(47 * 60, LocalDate.of(2026, 10, 5)), cumulativeSection(sessions)!!.longest)
    }

    @Test
    fun `인식 격차 추이는 답이 있는 주만 최근 4주까지 오래된 순서로 낸다`() {
        val sessions = octoberSessions(9) +
            session(at("2026-09-21"), perceivedMin = 60) + // +33%. 4주 밖
            session(at("2026-09-28"), perceivedMin = 54) + // +20%
            session(at("2026-10-13"), perceivedMin = 50) + // 같은 주 둘은 합으로: 99분 / 90분 = +10%
            session(at("2026-10-14"), perceivedMin = 49) +
            session(at("2026-10-20"), perceivedMin = 45) + // 0%
            session(at("2026-10-21"), perceivedMin = 90, valid = false) + // 통계 제외
            session(at("2026-10-27"), perceivedMin = 36) // -20%

        assertEquals(listOf(20, 10, 0, -20), cumulativeSection(sessions)!!.gapTrendPct)
    }

    @Test
    fun `격차 답이 한 주뿐이면 추이를 감춘다`() {
        val sessions = octoberSessions(14) + session(at("2026-10-20"), perceivedMin = 50)

        assertTrue(cumulativeSection(sessions)!!.gapTrendPct.isEmpty())
    }
}
