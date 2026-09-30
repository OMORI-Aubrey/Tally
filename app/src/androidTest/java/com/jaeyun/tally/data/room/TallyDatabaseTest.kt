package com.jaeyun.tally.data.room

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.GoalMetric
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TallyDatabaseTest {

    private lateinit var db: TallyDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun session(startAt: Long, endAt: Long? = null) =
        StudySession(subjectName = null, startAt = startAt, endAt = endAt, createdAt = startAt)

    @Test
    fun 미완료_세션은_endAt이_없는_가장_최근_세션이다() = runTest {
        val dao = db.studySessionDao()
        dao.insert(session(startAt = 1_000, endAt = 2_000))
        val unfinishedId = dao.insert(session(startAt = 3_000))

        assertEquals(unfinishedId, dao.getUnfinished()?.id)
    }

    @Test
    fun 세션을_지우면_구간도_지워진다() = runTest {
        val sessionId = db.studySessionDao().insert(session(startAt = 0, endAt = 60_000))
        db.appSegmentDao().insertAll(
            listOf(
                AppSegment(sessionId = sessionId, packageName = "com.jaeyun.tally", category = Category.ALLOWED, startAt = 0, endAt = 30_000),
                AppSegment(sessionId = sessionId, packageName = "__SCREEN_OFF__", category = Category.SCREEN_OFF, startAt = 30_000, endAt = 60_000),
            ),
        )

        db.studySessionDao().delete(checkNotNull(db.studySessionDao().getById(sessionId)))

        assertTrue(db.appSegmentDao().getBySession(sessionId).isEmpty())
    }

    @Test
    fun 구간_분류_enum은_그대로_저장되고_시각순으로_읽힌다() = runTest {
        val sessionId = db.studySessionDao().insert(session(startAt = 0, endAt = 20_000))
        db.appSegmentDao().insertAll(
            listOf(
                AppSegment(sessionId = sessionId, packageName = "app", category = Category.AWAY, startAt = 10_000, endAt = 20_000),
                AppSegment(sessionId = sessionId, packageName = "app", category = Category.DISTRACT, startAt = 0, endAt = 10_000),
            ),
        )

        val categories = db.appSegmentDao().getBySession(sessionId).map { it.category }
        assertEquals(listOf(Category.DISTRACT, Category.AWAY), categories)
    }

    @Test
    fun 자동_분류는_사용자_분류를_덮어쓰지_않는다() = runTest {
        val dao = db.appClassificationDao()
        dao.upsert(AppClassification("com.android.chrome", "Chrome", Category.ALLOWED, ClassificationSource.USER))

        dao.insertIfAbsent(
            listOf(
                AppClassification("com.android.chrome", "Chrome", Category.DISTRACT, ClassificationSource.AUTO_CATEGORY),
                AppClassification("com.google.android.youtube", "YouTube", Category.DISTRACT, ClassificationSource.AUTO_CATEGORY),
            ),
        )

        assertEquals(ClassificationSource.USER, dao.get("com.android.chrome")?.source)
        assertEquals(Category.ALLOWED, dao.get("com.android.chrome")?.category)
        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun 주간_목표는_주당_하나이고_달성값은_나중에_채운다() = runTest {
        val dao = db.weeklyGoalDao()
        val goal = WeeklyGoal(
            weekStartAt = 1_000,
            metric = GoalMetric.INTERRUPTION,
            targetValue = 3.4f,
            baselineValue = 3.4f,
        )
        dao.upsert(goal)
        assertNull(dao.getByWeek(1_000)?.isAchieved)

        dao.upsert(goal.copy(achievedValue = 3.2f, isAchieved = true))
        assertEquals(true, dao.getByWeek(1_000)?.isAchieved)
    }
}
