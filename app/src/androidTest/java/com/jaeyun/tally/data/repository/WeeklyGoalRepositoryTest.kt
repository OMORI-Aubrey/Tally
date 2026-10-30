package com.jaeyun.tally.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.data.room.WeeklyGoal
import com.jaeyun.tally.domain.model.GoalMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class WeeklyGoalRepositoryTest {

    private lateinit var db: TallyDatabase
    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: WeeklyGoalRepository

    private val seoul = ZoneId.of("Asia/Seoul")
    private val day = 24 * 60 * 60_000L

    /** 이번 주 월요일 2026-10-26 00:00 KST */
    private val thisWeek = LocalDate.of(2026, 10, 26).atStartOfDay(seoul).toInstant().toEpochMilli()
    private val lastWeek = thisWeek - 7 * day

    /** 2026-10-29 (목) 12:00 KST */
    private val now = thisWeek + 3 * day + 12 * 60 * 60_000L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        dataStoreFile = File(context.cacheDir, "weekly-goal-test-${System.nanoTime()}.preferences_pb")
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val settings = AppSettingsStore(PreferenceDataStoreFactory.create(scope = dataStoreScope) { dataStoreFile })
        repository = WeeklyGoalRepository(db, settings, debugBuild = false)
    }

    @After
    fun tearDown() {
        db.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    /** 통계에 셀 1시간 세션. 순공 45분, LFS 40분 */
    private suspend fun insertSession(startAt: Long, interruptions: Int) {
        db.studySessionDao().insert(
            StudySession(
                subjectName = null,
                startAt = startAt,
                endAt = startAt + 60 * 60_000L,
                tTotalSec = 3600,
                tFocusSec = 2700,
                tDistSec = 900,
                lfsSec = 2400,
                densityPct = 75f,
                interruptionCount = interruptions,
                metricsVersion = 1,
                isValidForStats = true,
                hasTimeline = true,
                createdAt = startAt,
            ),
        )
    }

    /** 지난주 기준선 기간(지난주를 뺀 직전 4주)에 세션당 이탈 2회인 세션 12개 */
    private suspend fun insertBaselineSessions() {
        repeat(12) { insertSession(startAt = lastWeek - (it + 1) * 2 * day, interruptions = 2) }
    }

    @Test
    fun 지난주_값으로_이번_주_목표를_만든다() = runTest {
        insertBaselineSessions()
        repeat(3) { insertSession(startAt = lastWeek + it * day, interruptions = 3) }

        val goals = repository.refresh(now, seoul)

        // 지난주 이탈 3회가 기준선 2회의 150%라 1순위: 기준선으로 복귀
        assertEquals(WeeklyGoal(thisWeek, GoalMetric.INTERRUPTION, targetValue = 2f, baselineValue = 2f), goals.current)
    }

    @Test
    fun 같은_주에는_목표를_다시_만들거나_바꾸지_않는다() = runTest {
        insertBaselineSessions()
        repeat(3) { insertSession(startAt = lastWeek + it * day, interruptions = 3) }
        val first = repository.refresh(now, seoul).current

        // 지난주 값이 바뀌어도(예: 재분류) 제시한 목표는 그대로다
        repeat(3) { insertSession(startAt = lastWeek + it * day + 60 * 60_000L, interruptions = 0) }
        val second = repository.refresh(now + day, seoul).current

        assertNotNull(first)
        assertEquals(first, second)
    }

    @Test
    fun 지난주에_통계_세션이_없으면_목표가_없다() = runTest {
        insertBaselineSessions()

        assertNull(repository.refresh(now, seoul).current)
    }

    @Test
    fun 지난주_목표를_달성하면_배지_데이터가_있다() = runTest {
        db.weeklyGoalDao().upsert(WeeklyGoal(lastWeek, GoalMetric.INTERRUPTION, targetValue = 2.5f, baselineValue = 2.5f))
        repeat(2) { insertSession(startAt = lastWeek + it * day, interruptions = 2) }

        val badge = repository.refresh(now, seoul).achievedLastWeek

        assertEquals(2f, badge?.achievedValue)
        assertEquals(true, badge?.isAchieved)
        assertEquals(true, db.weeklyGoalDao().getByWeek(lastWeek)?.isAchieved)
    }

    @Test
    fun 지난주_목표를_달성하지_못하면_아무것도_보여_주지_않는다() = runTest {
        db.weeklyGoalDao().upsert(WeeklyGoal(lastWeek, GoalMetric.INTERRUPTION, targetValue = 2.5f, baselineValue = 2.5f))
        repeat(2) { insertSession(startAt = lastWeek + it * day, interruptions = 4) }

        assertNull(repository.refresh(now, seoul).achievedLastWeek)
        assertEquals(false, db.weeklyGoalDao().getByWeek(lastWeek)?.isAchieved)
    }

    @Test
    fun 기록_탭을_열지_않아_밀린_주의_목표도_판정한다() = runTest {
        val twoWeeksAgo = lastWeek - 7 * day
        db.weeklyGoalDao().upsert(WeeklyGoal(twoWeeksAgo, GoalMetric.INTERRUPTION, targetValue = 2.5f, baselineValue = 2.5f))
        insertSession(startAt = twoWeeksAgo + day, interruptions = 1)

        repository.refresh(now, seoul)

        val judged = db.weeklyGoalDao().getByWeek(twoWeeksAgo)
        assertEquals(1f, judged?.achievedValue)
        assertEquals(true, judged?.isAchieved)
    }
}
