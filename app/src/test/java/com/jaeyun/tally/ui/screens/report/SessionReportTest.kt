package com.jaeyun.tally.ui.screens.report

import com.jaeyun.tally.data.repository.SessionReportData
import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.data.room.AppSegment
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.PseudoPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val MIN = 60_000L
private const val YOUTUBE = "com.google.android.youtube"
private const val KAKAO = "com.kakao.talk"
private const val INSTA = "com.instagram.android"

class SessionReportTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val start = LocalDateTime.parse("2026-10-02T19:00").atZone(seoul).toInstant().toEpochMilli()

    /** 19:00 시작 98분 세션. 지표는 따로 넣는다 */
    private fun session(
        lfsMin: Int = 34,
        interruptions: Int = 5,
        focusMin: Int = 88,
        distractMin: Int = 10,
        totalMin: Int = 98,
        lengthMin: Int = 98,
        density: Float = 89.8f,
        perceived: Int? = null,
        timeline: Boolean = true,
        valid: Boolean = true,
        subject: String? = "코틀린",
    ) = StudySession(
        id = 1,
        subjectName = subject,
        startAt = start,
        endAt = start + lengthMin * MIN,
        perceivedFocusMin = perceived,
        tTotalSec = totalMin * 60,
        tFocusSec = focusMin * 60,
        tDistSec = distractMin * 60,
        lfsSec = lfsMin * 60,
        densityPct = density,
        interruptionCount = interruptions,
        isValidForStats = valid,
        hasTimeline = timeline,
        createdAt = start,
    )

    private fun segment(pkg: String, category: Category, fromMin: Long, toMin: Long) =
        AppSegment(sessionId = 1, packageName = pkg, category = category, startAt = start + fromMin * MIN, endAt = start + toMin * MIN)

    private fun auto(pkg: String, label: String) = AppClassification(pkg, label, Category.DISTRACT, ClassificationSource.AUTO_CATEGORY)

    private fun report(
        session: StudySession = session(),
        segments: List<AppSegment> = emptyList(),
        classifications: List<AppClassification> = emptyList(),
        bestOther: Int? = null,
    ) = buildSessionReport(SessionReportData(session, segments, classifications.associateBy { it.packageName }, bestOther), seoul)

    @Test
    fun `체감을 적었으면 격차를 반올림해 양수를 과대평가로 둔다`() {
        val gap = report(session(perceived = 120, focusMin = 88)).gap!!

        // (120 − 88) / 88 = 36.4%
        assertEquals(36, gap.gapPct)
        assertEquals(120, gap.perceivedMin)
        assertEquals(34 * 60, gap.lfsSec)
        assertEquals(-20, report(session(perceived = 40, focusMin = 50)).gap!!.gapPct)
    }

    @Test
    fun `체감이 없거나 순공이 0이거나 타임라인이 없으면 격차를 두지 않는다`() {
        assertNull(report(session(perceived = null)).gap)
        assertNull(report(session(perceived = 30, focusMin = 0)).gap)
        assertNull(report(session(perceived = 30, timeline = false)).gap)
    }

    @Test
    fun `조각 수는 이탈 횟수 더하기 1이고 비율은 착석 대비 버림이다`() {
        val structure = report(session(lfsMin = 34, interruptions = 5, totalMin = 98)).structure!!

        assertEquals(6, structure.pieces)
        // 34 / 98 = 34.7%
        assertEquals(34, structure.lfsSharePct)
        // 1%도 안 되면 0%를 쓰지 않는다
        assertNull(report(session(lfsMin = 0, totalMin = 98)).structure!!.lfsSharePct)
    }

    @Test
    fun `기록은 다른 세션과 견준다`() {
        assertEquals(RecordMark.Best(47 * 60), report(session(lfsMin = 34), bestOther = 47 * 60).structure!!.record)
        assertEquals(RecordMark.NewRecord, report(session(lfsMin = 52), bestOther = 47 * 60).structure!!.record)
        // 같으면 갱신이 아니다
        assertEquals(RecordMark.Best(47 * 60), report(session(lfsMin = 47), bestOther = 47 * 60).structure!!.record)
    }

    @Test
    fun `첫 세션이거나 통계 제외 세션이면 기록 줄을 두지 않는다`() {
        assertNull(report(session(), bestOther = null).structure!!.record)
        assertNull(report(session(valid = false, lfsMin = 2), bestOther = 47 * 60).structure!!.record)
        // 다른 세션의 기록이 0초뿐이면 견주지 않는다
        assertNull(report(session(), bestOther = 0).structure!!.record)
    }

    @Test
    fun `타임라인이 없으면 구조·지표 카드 없이 착석만 둔다`() {
        val report = report(session(timeline = false))

        assertNull(report.structure)
        assertNull(report.cards)
        assertEquals(Totals(focusSec = null, sittingSec = 98 * 60, distractSec = null), report.totals)
    }

    @Test
    fun `지표 카드는 밀도를 버리고 세션 길이는 타이머 길이다`() {
        // 자리 비움으로 착석(80분)이 타이머 길이(98분)보다 짧다
        val cards = report(session(totalMin = 80, lengthMin = 98, density = 89.8f)).cards!!

        assertEquals(89, cards.densityPct)
        assertEquals(98 * 60, cards.lengthSec)
        assertNull(report(session(density = 0.4f)).cards!!.densityPct)
    }

    @Test
    fun `이탈 목록은 딴짓 구간을 시각 순으로, 화면만 켠 구간은 이름 없이 둔다`() {
        val distractions = report(
            segments = listOf(
                segment("com.jaeyun.tally", Category.ALLOWED, 0, 24),
                segment(INSTA, Category.DISTRACT, 24, 27),
                segment(PseudoPackage.SCREEN_OFF, Category.SCREEN_OFF, 27, 51),
                segment(PseudoPackage.SCREEN_ON_UNKNOWN, Category.DISTRACT, 51, 52),
                segment(YOUTUBE, Category.DISTRACT, 52, 57),
            ),
            classifications = listOf(auto(INSTA, "인스타그램")),
        ).distractions

        assertEquals(listOf(LocalTime.of(19, 24), LocalTime.of(19, 51), LocalTime.of(19, 52)), distractions.map { it.startTime })
        // 분류표에 이름이 없으면 패키지 이름을 쓴다
        assertEquals(listOf("인스타그램", null, YOUTUBE), distractions.map { it.appLabel })
        // 화면 켜짐은 앱이 아니라 재분류할 수 없다
        assertEquals(listOf(INSTA, null, YOUTUBE), distractions.map { it.packageName })
        assertEquals(listOf(180, 60, 300), distractions.map { it.durationSec })
    }

    @Test
    fun `미분류 앱은 사용자가 정하지 않은 딴짓 앱을 딴짓 시간이 긴 순서로 2개까지 묻는다`() {
        val unclassified = report(
            segments = listOf(
                segment(KAKAO, Category.DISTRACT, 0, 2),
                segment(INSTA, Category.DISTRACT, 2, 5),
                segment(YOUTUBE, Category.DISTRACT, 5, 6),
                segment(KAKAO, Category.DISTRACT, 6, 9),
                segment(PseudoPackage.SCREEN_ON_UNKNOWN, Category.DISTRACT, 9, 30),
                segment("com.example.notes", Category.ALLOWED, 30, 60),
            ),
            classifications = listOf(
                auto(KAKAO, "카카오톡"),
                auto(INSTA, "인스타그램"),
                AppClassification(YOUTUBE, "YouTube", Category.DISTRACT, ClassificationSource.USER),
                AppClassification("com.example.notes", "노트", Category.ALLOWED, ClassificationSource.AUTO_CATEGORY),
            ),
        ).unclassified

        // 카카오톡 5분, 인스타그램 3분. 유튜브는 사용자가 정했고, 화면 켜짐과 허용 앱은 묻지 않는다
        assertEquals(listOf(UnclassifiedApp(KAKAO, "카카오톡"), UnclassifiedApp(INSTA, "인스타그램")), unclassified)
    }

    @Test
    fun `메타는 로컬 날짜와 시각, 통계 제외 여부를 둔다`() {
        val meta = report(session(lengthMin = 98, valid = false, subject = null)).meta

        assertNull(meta.subjectName)
        assertEquals(LocalDate.of(2026, 10, 2), meta.date)
        assertEquals(LocalTime.of(19, 0), meta.startTime)
        assertEquals(LocalTime.of(20, 38), meta.endTime)
        assertTrue(meta.excludedFromStats)
        assertFalse(report(session()).meta.excludedFromStats)
    }
}
