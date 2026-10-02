package com.jaeyun.tally.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jaeyun.tally.data.room.TallyDatabase
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

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {

    private lateinit var db: TallyDatabase
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        repository = SessionRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

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
    fun 종료하면_길이와_통계_포함_여부를_저장한다() = runTest {
        val id = repository.start(subjectName = null, now = 0)

        val finished = repository.finish(id, now = 30 * 60 * 1000L)

        assertEquals(30 * 60 * 1000L, finished?.endAt)
        assertEquals(30 * 60, finished?.tTotalSec)
        assertTrue(finished!!.isValidForStats)
        assertNull(repository.runningSession.first())
    }

    @Test
    fun 세션이_3분_미만이면_기록하되_통계에서_뺀다() = runTest {
        val id = repository.start(subjectName = null, now = 0)

        val finished = repository.finish(id, now = 2 * 60 * 1000L)

        assertFalse(finished!!.isValidForStats)
        assertEquals(finished, db.studySessionDao().getById(id))
    }

    @Test
    fun 이미_끝난_세션을_다시_종료하면_바꾸지_않는다() = runTest {
        val id = repository.start(subjectName = null, now = 0)
        repository.finish(id, now = 10 * 60 * 1000L)

        assertNull(repository.finish(id, now = 20 * 60 * 1000L))
        assertEquals(10 * 60 * 1000L, db.studySessionDao().getById(id)?.endAt)
    }
}
