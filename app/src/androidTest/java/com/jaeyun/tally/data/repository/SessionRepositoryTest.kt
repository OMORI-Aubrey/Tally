package com.jaeyun.tally.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.data.usagestats.AppAutoClassifier
import com.jaeyun.tally.data.usagestats.AutoCategoryReason
import com.jaeyun.tally.data.usagestats.AutoClassification
import com.jaeyun.tally.data.usagestats.UsageEventQuery
import com.jaeyun.tally.data.usagestats.UsageQueryResult
import com.jaeyun.tally.domain.metrics.MetricsVersion
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.RawEvent
import com.jaeyun.tally.domain.model.RawEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val SELF = "com.jaeyun.tally"
private const val YOUTUBE = "com.google.android.youtube"
private const val ZOOM = "us.zoom.videomeetings"
private const val MIN = 60_000L

/** 정해 둔 결과를 돌려주는 사용 기록 */
private class FakeUsageEvents(var result: UsageQueryResult = UsageQueryResult.Events(emptyList())) : UsageEventQuery {
    override fun query(beginAt: Long, endAt: Long) = result
}

/** 유튜브만 딴짓으로 보는 자동 분류. 몇 번 불렸는지 센다 */
private class FakeClassifier : AppAutoClassifier {
    val requested = mutableListOf<String>()
    override fun classifyAll(packageNames: Collection<String>) = packageNames.map {
        requested += it
        val category = if (it == YOUTUBE) Category.DISTRACT else Category.ALLOWED
        AutoClassification(it, "label:$it", category, AutoCategoryReason.INSTALLED)
    }
}

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {

    private lateinit var db: TallyDatabase
    private lateinit var usageEvents: FakeUsageEvents
    private lateinit var classifier: FakeClassifier
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        usageEvents = FakeUsageEvents()
        classifier = FakeClassifier()
        repository = SessionRepository(db, usageEvents, classifier, SELF, strictInvariant = true, ioDispatcher = Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun resumed(at: Long, pkg: String) = RawEvent(at, RawEventType.ACTIVITY_RESUMED, pkg)

    @Test
    fun 시작하면_진행_중_세션이_된다() = runTest {
        val id = repository.start(subjectName = "수학", now = 1_000)

        val running = repository.runningSession.first()
        assertEquals(id, running?.id)
        assertEquals(1_000L, running?.startAt)
        assertEquals("수학", running?.subjectName)
    }

    @Test
    fun 진행_중에_다시_시작하면_새_세션을_만들지_않는다() = runTest {
        val first = repository.start(subjectName = null, now = 1_000)
        val second = repository.start(subjectName = null, now = 2_000)

        assertEquals(first, second)
        assertEquals(1_000L, db.studySessionDao().getById(first)?.startAt)
    }

    @Test
    fun 종료하면_복원한_지표와_구간을_함께_저장한다() = runTest {
        val id = repository.start(subjectName = null, now = 0)
        usageEvents.result = UsageQueryResult.Events(listOf(resumed(10 * MIN, YOUTUBE), resumed(15 * MIN, SELF)))

        val finished = repository.finish(id, now = 30 * MIN)!!

        assertEquals(30 * 60, finished.tTotalSec)
        assertEquals(25 * 60, finished.tFocusSec)
        assertEquals(1, finished.interruptionCount)
        assertEquals(15 * 60, finished.lfsSec)
        assertEquals(MetricsVersion.CURRENT, finished.metricsVersion)
        assertTrue(finished.hasTimeline)
        assertTrue(finished.isValidForStats)
        assertNull(repository.runningSession.first())

        val segments = db.appSegmentDao().getBySession(id)
        assertEquals(listOf(Category.ALLOWED, Category.DISTRACT, Category.ALLOWED), segments.map { it.category })
        // 실기기 완료 조건과 같은 검사: 구간 합 = 세션 길이
        assertEquals(30 * MIN, segments.sumOf { it.endAt - it.startAt })
    }

    @Test
    fun 권한이_없으면_착석만_저장한다() = runTest {
        val id = repository.start(subjectName = null, now = 0)
        usageEvents.result = UsageQueryResult.NoPermission

        val finished = repository.finish(id, now = 30 * MIN)!!

        assertFalse(finished.hasTimeline)
        assertEquals(30 * 60, finished.tTotalSec)
        assertEquals(MetricsVersion.NOT_COMPUTED, finished.metricsVersion)
        assertTrue(db.appSegmentDao().getBySession(id).isEmpty())
    }

    @Test
    fun 새로_본_앱은_자동_분류로_저장하고_본_앱은_저장하지_않는다() = runTest {
        val id = repository.start(subjectName = null, now = 0)
        usageEvents.result = UsageQueryResult.Events(listOf(resumed(10 * MIN, YOUTUBE), resumed(15 * MIN, SELF)))

        repository.finish(id, now = 30 * MIN)

        val stored = db.appClassificationDao().getAll()
        assertEquals(listOf(YOUTUBE), stored.map { it.packageName })
        assertEquals(ClassificationSource.AUTO_CATEGORY, stored.single().source)
        assertEquals(Category.DISTRACT, stored.single().category)
    }

    @Test
    fun 사용자가_정한_분류는_덮어쓰지_않고_복원에_그대로_쓴다() = runTest {
        // 사용자가 유튜브를 허용(강의 시청)으로 바꿔 두었다
        db.appClassificationDao().upsert(AppClassification(YOUTUBE, "YouTube", Category.ALLOWED, ClassificationSource.USER))
        val id = repository.start(subjectName = null, now = 0)
        usageEvents.result = UsageQueryResult.Events(listOf(resumed(10 * MIN, YOUTUBE), resumed(15 * MIN, SELF)))

        val finished = repository.finish(id, now = 30 * MIN)!!

        assertEquals(0, finished.interruptionCount)
        assertEquals(ClassificationSource.USER, db.appClassificationDao().get(YOUTUBE)?.source)
        assertTrue(YOUTUBE !in classifier.requested)
    }

    @Test
    fun 자동_분류_행은_세션마다_새_규칙으로_갱신한다() = runTest {
        db.appClassificationDao().upsert(AppClassification(ZOOM, "옛 이름", Category.DISTRACT, ClassificationSource.AUTO_CATEGORY))
        val id = repository.start(subjectName = null, now = 0)
        usageEvents.result = UsageQueryResult.Events(listOf(resumed(10 * MIN, ZOOM)))

        repository.finish(id, now = 30 * MIN)

        val zoom = db.appClassificationDao().get(ZOOM)!!
        assertEquals(Category.ALLOWED, zoom.category)
        assertEquals("label:$ZOOM", zoom.appLabel)
    }

    @Test
    fun 세션이_3분_미만이면_기록하되_통계에서_뺀다() = runTest {
        val id = repository.start(subjectName = null, now = 0)

        val finished = repository.finish(id, now = 2 * MIN)

        assertFalse(finished!!.isValidForStats)
        assertEquals(finished, db.studySessionDao().getById(id))
    }

    @Test
    fun 리포트에서_허용으로_바꾸면_그_세션을_다시_계산한다() = runTest {
        val id = repository.start(subjectName = null, now = 0)
        usageEvents.result = UsageQueryResult.Events(listOf(resumed(10 * MIN, YOUTUBE), resumed(15 * MIN, SELF)))
        repository.finish(id, now = 30 * MIN)

        val result = repository.reclassify(id, YOUTUBE, Category.ALLOWED)

        assertEquals(ReclassifyResult.RECOMPUTED, result)
        val session = db.studySessionDao().getById(id)!!
        assertEquals(0, session.interruptionCount)
        assertEquals(30 * 60, session.lfsSec)
        assertEquals(listOf(Category.ALLOWED), db.appSegmentDao().getBySession(id).map { it.category })
        assertEquals(ClassificationSource.USER, db.appClassificationDao().get(YOUTUBE)?.source)
    }

    @Test
    fun 사용_기록이_지워졌으면_분류만_저장하고_다시_계산하지_않는다() = runTest {
        val id = repository.start(subjectName = null, now = 0)
        usageEvents.result = UsageQueryResult.Events(listOf(resumed(10 * MIN, YOUTUBE), resumed(15 * MIN, SELF)))
        repository.finish(id, now = 30 * MIN)
        usageEvents.result = UsageQueryResult.Events(emptyList())

        val result = repository.reclassify(id, YOUTUBE, Category.ALLOWED)

        assertEquals(ReclassifyResult.SAVED_ONLY, result)
        assertEquals(1, db.studySessionDao().getById(id)?.interruptionCount)
        assertEquals(Category.ALLOWED, db.appClassificationDao().get(YOUTUBE)?.category)
    }

    @Test
    fun 분류가_그대로면_사용자_분류로_확정만_한다() = runTest {
        val id = repository.start(subjectName = null, now = 0)
        usageEvents.result = UsageQueryResult.Events(listOf(resumed(10 * MIN, YOUTUBE), resumed(15 * MIN, SELF)))
        repository.finish(id, now = 30 * MIN)

        assertEquals(ReclassifyResult.UNCHANGED, repository.reclassify(id, YOUTUBE, Category.DISTRACT))
        assertEquals(ClassificationSource.USER, db.appClassificationDao().get(YOUTUBE)?.source)
        assertEquals(1, db.studySessionDao().getById(id)?.interruptionCount)
    }

    @Test
    fun 리포트_재료의_기록은_다른_세션에서_찾는다() = runTest {
        val first = repository.start(subjectName = null, now = 0)
        repository.finish(first, now = 20 * MIN)
        val second = repository.start(subjectName = null, now = 30 * MIN)
        repository.finish(second, now = 40 * MIN)

        val report = repository.loadReport(second)!!

        assertEquals(20 * 60, report.bestOtherLfsSec)
        assertNull(repository.loadReport(9_999))
    }

    @Test
    fun 이미_끝난_세션을_다시_종료하면_바꾸지_않는다() = runTest {
        val id = repository.start(subjectName = null, now = 0)
        repository.finish(id, now = 10 * MIN)

        assertNull(repository.finish(id, now = 20 * MIN))
        assertEquals(10 * MIN, db.studySessionDao().getById(id)?.endAt)
    }
}
