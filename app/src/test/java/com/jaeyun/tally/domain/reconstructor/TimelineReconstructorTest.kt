package com.jaeyun.tally.domain.reconstructor

import com.jaeyun.tally.domain.metrics.MetricsCalculator
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.Category.ALLOWED
import com.jaeyun.tally.domain.model.Category.DISTRACT
import com.jaeyun.tally.domain.model.Category.SCREEN_OFF
import com.jaeyun.tally.domain.model.PseudoPackage
import com.jaeyun.tally.domain.model.RawEvent
import com.jaeyun.tally.domain.model.RawEventType
import com.jaeyun.tally.domain.model.Segment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SELF = "com.jaeyun.tally"
private const val YOUTUBE = "com.google.android.youtube"
private const val INSTAGRAM = "com.instagram.android"
private const val LAUNCHER = "com.sec.android.app.launcher"
private const val DICTIONARY = "com.example.dictionary"
private const val SETTINGS = "com.android.settings"

private val DISTRACT_APPS = setOf(YOUTUBE, INSTAGRAM)
private val classify: (String) -> Category = { if (it in DISTRACT_APPS) DISTRACT else ALLOWED }

/** 세션 시작 시각. 이벤트 시각은 모두 이 기준 상대값으로 적는다 */
private const val T0 = 1_759_300_000_000L

private fun sec(s: Double): Long = (s * 1000).toLong()
private fun sec(s: Int): Long = s * 1000L
private fun min(m: Int): Long = m * 60_000L

private fun resumed(at: Long, pkg: String) = RawEvent(T0 + at, RawEventType.ACTIVITY_RESUMED, pkg)
private fun screenOn(at: Long) = RawEvent(T0 + at, RawEventType.SCREEN_INTERACTIVE)
private fun screenOff(at: Long) = RawEvent(T0 + at, RawEventType.SCREEN_NON_INTERACTIVE)
private fun unlock(at: Long) = RawEvent(T0 + at, RawEventType.KEYGUARD_HIDDEN)

/** §14.2 — 모든 타임라인 테스트의 마지막 줄 */
private fun assertInvariant(segments: List<Segment>, start: Long, end: Long) {
    assertEquals(start, segments.first().startAt)
    assertEquals(end, segments.last().endAt)
    segments.zipWithNext { a, b -> assertEquals(a.endAt, b.startAt) }
    assertEquals(end - start, segments.sumOf { it.endAt - it.startAt })
}

// 지표는 MetricsCalculator(§5.2)로 계산한다. 이 파일의 케이스는 모두 초 단위로 떨어진다
private fun List<Segment>.metrics() = MetricsCalculator.calculate(this, first().startAt, last().endAt)
private fun List<Segment>.interruptions(): Int = metrics().interruptionCount
private fun List<Segment>.distractMillis(): Long = metrics().tDistSec * 1000L
private fun List<Segment>.longestFocusMillis(): Long = metrics().lfsSec * 1000L

class TimelineReconstructorTest {

    private fun reconstruct(sessionMillis: Long, vararg events: RawEvent): List<Segment> {
        val timeline = TimelineReconstructor.reconstruct(events.toList(), T0, T0 + sessionMillis, SELF, classify)
        assertNull(timeline.invariantViolation)
        return timeline.segments
    }

    // ── §14.2 타임라인 케이스 13개 ──

    @Test
    fun `딴짓 없는 단일 구간은 순공 = 착석, 이탈 0회`() {
        val end = min(30)
        val segments = reconstruct(end, resumed(min(1), DICTIONARY))

        assertEquals(listOf(ALLOWED), segments.map { it.category })
        assertEquals(0L, segments.distractMillis())
        assertEquals(0, segments.interruptions())
        assertEquals(end, segments.longestFocusMillis())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `딴짓 앱 1회 진입은 그 시간만 차감하고 이탈 1회`() {
        val end = min(30)
        val segments = reconstruct(end, resumed(min(10), YOUTUBE), resumed(min(15), SELF))

        assertEquals(listOf(ALLOWED, DISTRACT, ALLOWED), segments.map { it.category })
        assertEquals(min(5), segments.distractMillis())
        assertEquals(1, segments.interruptions())
        assertEquals(min(15), segments.longestFocusMillis())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `딴짓 → 홈 2초 → 같은 딴짓은 이탈 1회`() {
        val end = min(30)
        val segments = reconstruct(
            end,
            resumed(min(10), YOUTUBE),
            resumed(min(15), LAUNCHER),
            resumed(min(15) + sec(2), YOUTUBE),
            resumed(min(20), SELF),
        )

        assertEquals(listOf(ALLOWED, DISTRACT, ALLOWED), segments.map { it.category })
        assertEquals(1, segments.interruptions())
        assertEquals(min(10), segments.distractMillis())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `중간에 화면이 꺼진 시간은 순공에 포함된다`() {
        val end = min(30)
        val segments = reconstruct(end, screenOff(min(5)), screenOn(min(25)), resumed(min(25) + sec(1), SELF))

        assertEquals(listOf(ALLOWED, SCREEN_OFF, ALLOWED), segments.map { it.category })
        assertEquals(0, segments.interruptions())
        assertEquals(end, segments.longestFocusMillis())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `종료 직전 RESUMED가 없으면 마지막 구간이 세션 끝까지 이어진다`() {
        val end = min(30)
        val segments = reconstruct(end, resumed(min(10), YOUTUBE))

        assertEquals(DISTRACT, segments.last().category)
        assertEquals(T0 + min(10), segments.last().startAt)
        assertEquals(min(20), segments.distractMillis())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `이벤트가 하나도 없으면 seed 구간이 세션 전체를 덮는다`() {
        val end = min(45)
        val segments = reconstruct(end)

        assertEquals(listOf(Segment(SELF, ALLOWED, T0, T0 + end)), segments)
        assertEquals(0L, segments.distractMillis())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `화면 켬 → 앱 없이 30초 → 화면 끔은 화면 켜짐 30초가 딴짓, 이탈 1회`() {
        val end = min(30)
        val segments = reconstruct(end, screenOff(min(5)), screenOn(min(10)), screenOff(min(10) + sec(30)))

        val distract = segments.single { it.category == DISTRACT }
        assertEquals(PseudoPackage.SCREEN_ON_UNKNOWN, distract.packageName)
        assertEquals(sec(30), distract.durationMillis)
        assertEquals(1, segments.interruptions())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `화면 켬 → 2초 → 앱 실행은 흡수되어 이탈 0회`() {
        val end = min(30)
        val segments = reconstruct(end, screenOff(min(5)), screenOn(min(10)), resumed(min(10) + sec(2), DICTIONARY))

        assertEquals(0, segments.interruptions())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `화면 켬 → 7초 → 앱 실행은 화면 켜짐 임계 10초로 흡수되어 이탈 0회`() {
        val end = min(30)
        val segments = reconstruct(end, screenOff(min(5)), screenOn(min(10)), resumed(min(10) + sec(7), DICTIONARY))

        assertEquals(0, segments.interruptions())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `런처 RESUMED → 26ms 후 화면 켬 → 0_16초 후 잠금 해제 → 홈 19초 → 앱은 홈 체류가 허용, 이탈 0회`() {
        // #1 실기기(One UI) 실측 재현: 화면을 끄기 전 포그라운드였던 런처가 화면 켜짐보다 먼저 RESUMED된다
        val end = min(30)
        val on = min(10) + 26
        val unlockAt = on + 160
        val segments = reconstruct(
            end,
            screenOff(min(5)),
            resumed(min(10), LAUNCHER),
            screenOn(on),
            unlock(unlockAt),
            resumed(unlockAt + sec(19), SETTINGS),
        )

        assertEquals(0, segments.interruptions())
        assertEquals(listOf(ALLOWED, SCREEN_OFF, ALLOWED), segments.map { it.category })
        // 화면 켜짐 0.16초와 런처 26ms는 화면 꺼짐에 흡수되고, 해제 뒤 홈 19초부터 허용 구간이다
        assertEquals(T0 + unlockAt, segments.last().startAt)
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `화면 켬 → 앱 RESUMED → 이어서 잠금 해제는 해제가 무시되어 구간이 쪼개지지 않는다`() {
        val end = min(30)
        val segments = reconstruct(
            end,
            screenOff(min(5)),
            screenOn(min(10)),
            resumed(min(10) + sec(1), YOUTUBE),
            unlock(min(10) + sec(1.5)),
            resumed(min(12), SELF),
        )

        val distract = segments.single { it.category == DISTRACT }
        assertEquals(YOUTUBE, distract.packageName)
        assertEquals(T0 + min(10) + sec(1), distract.startAt)
        assertEquals(T0 + min(12), distract.endAt)
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `시작 후 10분 무이벤트 → 학습앱 20분이면 LFS = 30분`() {
        val end = min(30)
        val segments = reconstruct(end, resumed(min(10), DICTIONARY))

        assertEquals(min(30), segments.longestFocusMillis())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `화면 꺼짐 단일 구간 70분은 확인 다이얼로그 대상`() {
        val end = min(90)
        val segments = reconstruct(end, screenOff(min(5)), screenOn(min(75)), resumed(min(75) + sec(1), SELF))

        val long = segments.longScreenOffSegments()
        assertEquals(1, long.size)
        assertEquals(T0 + min(5), long.single().startAt)
        assertInvariant(segments, T0, T0 + end)
    }

    // ── 이슈 #9 완료 조건의 추가 케이스 ──

    @Test
    fun `딴짓 → 홈 6초 → 같은 딴짓은 일반 구간 임계 5초라 이탈 2회`() {
        val end = min(30)
        val segments = reconstruct(
            end,
            resumed(min(10), YOUTUBE),
            resumed(min(15), LAUNCHER),
            resumed(min(15) + sec(6), YOUTUBE),
            resumed(min(20), SELF),
        )

        assertEquals(2, segments.interruptions())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `화면 켬 → 12초 → 화면 끔은 화면 켜짐 임계 10초를 넘어 이탈 1회`() {
        val end = min(30)
        val segments = reconstruct(end, screenOff(min(5)), screenOn(min(10)), screenOff(min(10) + sec(12)))

        assertEquals(1, segments.interruptions())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `딴짓 앱 RESUMED → 화면 켬 → 0_12초 후 잠금 해제 → 그 앱 9_8초는 화면 켜짐이 아니라 그 앱의 구간`() {
        // #1 실측의 NAVER 사례. 해제 뒤 새 RESUMED가 없어도 lastResumed로 전환된다
        val end = min(30)
        val on = min(10) + 20
        val segments = reconstruct(
            end,
            screenOff(min(5)),
            resumed(min(10), INSTAGRAM),
            screenOn(on),
            unlock(on + 120),
            screenOff(on + 120 + sec(9.8)),
        )

        val distract = segments.single { it.category == DISTRACT }
        assertEquals(INSTAGRAM, distract.packageName)
        assertEquals(sec(9.8), distract.durationMillis)
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `잠금 화면에서 해제 없이 알림만 보고 끄면 그대로 딴짓`() {
        val end = min(30)
        val segments = reconstruct(end, screenOff(min(5)), resumed(min(10), LAUNCHER), screenOn(min(10) + 20), screenOff(min(11)))

        val distract = segments.single { it.category == DISTRACT }
        assertEquals(PseudoPackage.SCREEN_ON_UNKNOWN, distract.packageName)
        assertInvariant(segments, T0, T0 + end)
    }

    // ── 경계와 병합 규칙 ──

    @Test
    fun `서로 다른 딴짓 앱이 이어지면 이탈 1회이고 가장 오래 쓴 앱을 대표로 남긴다`() {
        val end = min(30)
        val segments = reconstruct(end, resumed(min(10), YOUTUBE), resumed(min(11), INSTAGRAM), resumed(min(14), SELF))

        val distract = segments.single { it.category == DISTRACT }
        assertEquals(INSTAGRAM, distract.packageName)
        assertEquals(min(4), distract.durationMillis)
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `첫 구간이 짧으면 다음 구간이 세션 시작부터 덮는다`() {
        val end = min(30)
        val segments = reconstruct(end, resumed(sec(2), YOUTUBE), resumed(min(10), SELF))

        assertEquals(Segment(YOUTUBE, DISTRACT, T0, T0 + min(10)), segments.first())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `세션 밖의 이벤트는 무시하고 세션 끝 시각의 이벤트도 불변식을 깨지 않는다`() {
        val end = min(30)
        val segments = reconstruct(
            end,
            RawEvent(T0 - sec(5), RawEventType.ACTIVITY_RESUMED, YOUTUBE),
            resumed(end, YOUTUBE),
            RawEvent(T0 + end + sec(5), RawEventType.ACTIVITY_RESUMED, INSTAGRAM),
        )

        assertEquals(listOf(Segment(SELF, ALLOWED, T0, T0 + end)), segments)
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `순서가 섞인 이벤트도 시각순으로 복원한다`() {
        val end = min(30)
        val segments = reconstruct(end, resumed(min(15), SELF), resumed(min(10), YOUTUBE))

        assertEquals(listOf(ALLOWED, DISTRACT, ALLOWED), segments.map { it.category })
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `패키지 없는 RESUMED는 무시한다`() {
        val end = min(30)
        val segments = reconstruct(end, RawEvent(T0 + min(10), RawEventType.ACTIVITY_RESUMED, packageName = null))

        assertEquals(listOf(Segment(SELF, ALLOWED, T0, T0 + end)), segments)
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `화면 꺼짐 정확히 60분은 다이얼로그 대상이 아니다`() {
        val end = min(90)
        // 화면 켜짐 1초는 화면 꺼짐에 흡수되므로, 흡수 뒤 화면 꺼짐이 정확히 60분이 되게 둔다
        val segments = reconstruct(end, screenOff(min(5)), screenOn(min(65) - sec(1)), resumed(min(65), SELF))

        assertEquals(min(60), segments.single { it.category == SCREEN_OFF }.durationMillis)
        assertTrue(segments.longScreenOffSegments().isEmpty())
        assertInvariant(segments, T0, T0 + end)
    }

    @Test
    fun `자리 비웠어요를 고른 화면 꺼짐만 자리 비움이 되고 착석과 순공에서 빠진다 (#21)`() {
        // 공부 5분 → 화면 꺼짐 70분 → 유튜브 5분 → 화면 꺼짐 10분(짧아서 묻지 않음)
        val end = min(90)
        val segments = reconstruct(end, screenOff(min(5)), resumed(min(75), YOUTUBE), screenOff(min(80)))
        val off = segments.longScreenOffSegments().single()

        val marked = segments.markAway(off.startAt, off.endAt)

        assertEquals(listOf(ALLOWED, Category.AWAY, DISTRACT, SCREEN_OFF), marked.map { it.category })
        val metrics = marked.metrics()
        assertEquals(20 * 60, metrics.tTotalSec)
        assertEquals(15 * 60, metrics.tFocusSec)
        assertEquals(10 * 60, metrics.lfsSec)
        assertInvariant(marked, T0, T0 + end)
    }

    @Test
    fun `경계가 다르거나 화면 꺼짐이 아닌 구간은 자리 비움으로 바꾸지 않는다`() {
        val segments = listOf(Segment(SELF, ALLOWED, 0, 10), Segment(PseudoPackage.SCREEN_OFF, SCREEN_OFF, 10, 20))

        assertEquals(segments, segments.markAway(10, 19))
        assertEquals(segments, segments.markAway(0, 10))
    }

    // ── 불변식 검사 자체 ──

    @Test
    fun `불변식 검사는 빈틈과 겹침과 경계 어긋남을 잡는다`() {
        val ok = listOf(Segment(SELF, ALLOWED, 0, 10), Segment(YOUTUBE, DISTRACT, 10, 20))
        assertNull(TimelineReconstructor.checkInvariant(ok, 0, 20))

        assertNotNull(TimelineReconstructor.checkInvariant(emptyList(), 0, 20))
        assertNotNull(TimelineReconstructor.checkInvariant(ok, 0, 25))
        assertNotNull(TimelineReconstructor.checkInvariant(listOf(ok[0], ok[1].copy(startAt = 12)), 0, 20))
        assertNotNull(TimelineReconstructor.checkInvariant(listOf(ok[0], ok[1].copy(startAt = 8)), 0, 20))
    }
}
