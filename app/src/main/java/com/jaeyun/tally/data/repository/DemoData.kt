package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.PseudoPackage
import com.jaeyun.tally.domain.model.Segment
import com.jaeyun.tally.domain.reconstructor.ReconstructedTimeline
import com.jaeyun.tally.util.weekRangeOf
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.random.Random

/** 데모 데이터에 나오는 앱. 이탈 목록과 앱 분류 화면이 이 이름을 보여 준다 */
private class DemoApp(val packageName: String, val label: String, val category: Category)

private val YOUTUBE = DemoApp("com.google.android.youtube", "YouTube", Category.DISTRACT)
private val INSTAGRAM = DemoApp("com.instagram.android", "Instagram", Category.DISTRACT)
private val KAKAO_TALK = DemoApp("com.kakao.talk", "카카오톡", Category.DISTRACT)
private val CALCULATOR = DemoApp("com.google.android.calculator", "계산기", Category.ALLOWED)

private val DISTRACT_APPS = listOf(KAKAO_TALK, YOUTUBE, INSTAGRAM)

/**
 * 합성 세션 하나의 계획.
 *
 * @param week 0 = 4주 전, 4 = 이번 주
 * @param day 월요일 0 ~ 일요일 6
 * @param startMinute 그날 00:00부터 몇 분 뒤에 시작하는지
 * @param distractions 딴짓 횟수(= 이탈 횟수)
 * @param distractMin 딴짓 한 번의 길이(분)
 * @param perceived 체감 집중 시간을 적은 세션인지(주 1회, §3.4)
 */
private class DemoPlan(
    val week: Int,
    val day: Int,
    val startMinute: Int,
    val lengthMin: Int,
    val subject: String,
    val distractions: Int,
    val distractMin: Int,
    val perceived: Boolean = false,
)

private fun at(hour: Int, minute: Int = 0) = hour * 60 + minute

/**
 * 5주의 흐름: 평범한 두 주 → 밤 세션이 많고 이탈이 잦은 힘든 주(2) → 회복(3) → 이번 주 나아짐(4).
 * 체감은 주마다 한 번(셋째 세션) 적었고 세션 길이에 가깝게 느낀다 — 힘든 주에 인식 격차가 가장 크고 회복하며 줄어든다.
 * 1주에는 3분이 안 되는 오조작 세션이 하나 있다(통계 제외).
 */
private val PLANS = listOf(
    DemoPlan(0, 0, at(10), 90, "수학", distractions = 3, distractMin = 4),
    DemoPlan(0, 1, at(20), 60, "영어", distractions = 3, distractMin = 3),
    DemoPlan(0, 3, at(14), 100, "수학", distractions = 4, distractMin = 4, perceived = true),
    DemoPlan(0, 5, at(23), 50, "물리", distractions = 5, distractMin = 3),

    DemoPlan(1, 0, at(10, 30), 80, "수학", distractions = 3, distractMin = 3),
    DemoPlan(1, 2, at(19, 30), 70, "영어", distractions = 2, distractMin = 4),
    DemoPlan(1, 3, at(23, 10), 45, "물리", distractions = 5, distractMin = 2, perceived = true),
    DemoPlan(1, 4, at(16), 2, "수학", distractions = 0, distractMin = 0),
    DemoPlan(1, 5, at(14), 110, "수학", distractions = 3, distractMin = 4),

    DemoPlan(2, 0, at(22, 30), 60, "수학", distractions = 7, distractMin = 3),
    DemoPlan(2, 1, at(23), 50, "영어", distractions = 6, distractMin = 3),
    DemoPlan(2, 3, at(21), 90, "물리", distractions = 7, distractMin = 4, perceived = true),
    DemoPlan(2, 6, at(15), 70, "수학", distractions = 5, distractMin = 3),

    DemoPlan(3, 0, at(10), 90, "수학", distractions = 4, distractMin = 3),
    DemoPlan(3, 2, at(14), 80, "영어", distractions = 3, distractMin = 3),
    DemoPlan(3, 3, at(20), 100, "물리", distractions = 3, distractMin = 3, perceived = true),
    DemoPlan(3, 5, at(10), 120, "수학", distractions = 2, distractMin = 3),

    DemoPlan(4, 0, at(9, 30), 90, "수학", distractions = 2, distractMin = 2),
    DemoPlan(4, 1, at(14), 100, "영어", distractions = 1, distractMin = 3),
    DemoPlan(4, 2, at(20), 80, "물리", distractions = 2, distractMin = 2),
    DemoPlan(4, 4, at(10), 110, "수학", distractions = 2, distractMin = 2),
)

/** 시작 버튼을 누른 본 앱 구간(seed)과 종료 버튼을 누르러 연 본 앱 구간 */
private const val SEED_MILLIS = 20_000L
private const val TAIL_MILLIS = 30_000L

/** 계산기를 잠깐 쓰는 길이. 1시간 이상 세션에 한 번 넣는다 */
private const val CALCULATOR_MILLIS = 90_000L

/** 체감은 세션 길이의 95%로 적는다. 타이머 길이를 넘지 않는다(PLAN.md §4 체감 입력 방식) */
private const val PERCEIVED_RATIO = 0.95

/** 저장할 데모 데이터. 세션 id는 0이고 넣을 때 정해진다 */
internal data class DemoData(val sessions: List<CompletedSession>, val classifications: List<AppClassification>)

/**
 * 데모 데이터(§14.4): [now]가 속한 주와 그 앞 4주, 5주의 합성 세션. 이미 끝난 세션만 만든다 — 이번 주에서 [now] 뒤에 끝날
 * 세션은 뺀다. 앞 4주에 통계에 셀 세션이 16개라 기준선(12세션)과 누적 섹션(15세션) 조건을 채운다.
 *
 * 구간은 복원 불변식(빈틈·겹침 없이 세션 전체를 덮는다)을 지키고, 지표는 실제 세션과 같은 [completeSession]으로 계산한다.
 * 같은 [now]면 항상 같은 데이터가 나온다.
 *
 * @param selfPackage 본 앱 패키지. 시작·종료 구간의 앱이다
 */
internal fun demoData(now: Long, zone: ZoneId, selfPackage: String): DemoData {
    val thisMonday = Instant.ofEpochMilli(weekRangeOf(now, zone).startAt).atZone(zone).toLocalDate()
    val sessions = PLANS.mapIndexedNotNull { index, plan ->
        val startAt = thisMonday.minusWeeks((4 - plan.week).toLong()).plusDays(plan.day.toLong())
            .atStartOfDay(zone).plusMinutes(plan.startMinute.toLong()).toInstant().toEpochMilli()
        val endAt = startAt + plan.lengthMin * 60_000L
        if (endAt > now) return@mapIndexedNotNull null

        val segments = demoSegments(plan, startAt, endAt, selfPackage, Random(index))
        val completed = completeSession(
            session = StudySession(subjectName = plan.subject, startAt = startAt, createdAt = startAt),
            now = endAt,
            timeline = ReconstructedTimeline(segments, invariantViolation = null),
            strictInvariant = true,
            onViolation = {},
        )
        val perceived = if (plan.perceived) (plan.lengthMin * PERCEIVED_RATIO).roundToInt() else null
        completed.copy(session = completed.session.copy(perceivedFocusMin = perceived))
    }
    val classifications = (DISTRACT_APPS + CALCULATOR).map {
        AppClassification(it.packageName, it.label, it.category, ClassificationSource.AUTO_CATEGORY)
    }
    return DemoData(sessions, classifications)
}

/**
 * 본 앱(시작) → 화면 꺼짐과 앱 사용이 번갈아 → 본 앱(종료). 앱 사용 사이는 모두 화면 꺼짐이라 같은 분류가 이어 붙지 않는다.
 * 딴짓 앱은 돌아가며 쓰고 길이는 ±30초 흔든다. 화면 꺼짐 길이는 무작위 비율로 나눈다.
 */
private fun demoSegments(plan: DemoPlan, startAt: Long, endAt: Long, selfPackage: String, random: Random): List<Segment> {
    val uses = buildList {
        repeat(plan.distractions) { i ->
            val millis = plan.distractMin * 60_000L + random.nextLong(-30, 31) * 1000L
            add(DISTRACT_APPS[(plan.week + i) % DISTRACT_APPS.size] to millis)
        }
        if (plan.lengthMin >= 60) add(size / 2, CALCULATOR to CALCULATOR_MILLIS)
    }
    val offMillis = (endAt - startAt) - SEED_MILLIS - TAIL_MILLIS - uses.sumOf { it.second }
    check(offMillis > 0) { "데모 세션 계획이 세션 길이를 넘는다: ${plan.week}주 ${plan.day}일" }

    // 화면 꺼짐 구간 uses.size + 1개. 초 단위로 나누고 남는 것은 마지막 구간에 붙인다
    val weights = List(uses.size + 1) { 0.5 + random.nextDouble() }
    val offSeconds = offMillis / 1000
    val offs = weights.map { (offSeconds * it / weights.sum()).toLong() * 1000L }.toMutableList()
    offs[offs.lastIndex] += offMillis - offs.sum()

    val segments = mutableListOf<Segment>()
    var cursor = startAt
    fun add(packageName: String, category: Category, millis: Long) {
        segments += Segment(packageName, category, cursor, cursor + millis)
        cursor += millis
    }
    add(selfPackage, Category.ALLOWED, SEED_MILLIS)
    uses.forEachIndexed { i, (app, millis) ->
        add(PseudoPackage.SCREEN_OFF, Category.SCREEN_OFF, offs[i])
        add(app.packageName, app.category, millis)
    }
    add(PseudoPackage.SCREEN_OFF, Category.SCREEN_OFF, offs.last())
    add(selfPackage, Category.ALLOWED, TAIL_MILLIS)
    return segments
}
