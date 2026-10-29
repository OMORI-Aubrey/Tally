package com.jaeyun.tally.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.TallyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SubjectRepositoryTest {

    private lateinit var db: TallyDatabase
    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: SubjectRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        dataStoreFile = File(context.cacheDir, "subject-test-${System.nanoTime()}.preferences_pb")
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val settings = AppSettingsStore(PreferenceDataStoreFactory.create(scope = dataStoreScope) { dataStoreFile })
        repository = SubjectRepository(db.studySessionDao(), settings)
    }

    @After
    fun tearDown() {
        db.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    private suspend fun session(subject: String?, startAt: Long) {
        db.studySessionDao().insert(StudySession(subjectName = subject, startAt = startAt, endAt = startAt + 1, createdAt = startAt))
    }

    @Test
    fun 칩은_최근에_쓴_순서다() = runTest {
        session("수학", startAt = 1)
        session("영어", startAt = 2)
        session("수학", startAt = 3)
        session(null, startAt = 4)

        assertEquals(listOf("수학", "영어"), repository.recentVisibleSubjects.first())
    }

    @Test
    fun 추가만_한_과목도_칩과_시트에_남는다() = runTest {
        session("수학", startAt = 1)
        repository.add("코틀린", now = 2)
        repository.add("자바", now = 3)

        assertEquals(listOf("자바", "코틀린", "수학"), repository.recentVisibleSubjects.first())
        assertEquals(SubjectSummary("코틀린", 0, hidden = false), repository.allSubjects.first().last())
    }

    @Test
    fun 시트는_세션_많은_순이고_숨긴_과목도_보인다() = runTest {
        session("영어", startAt = 1)
        session("수학", startAt = 2)
        session("수학", startAt = 3)
        repository.setHidden("수학", hidden = true)

        assertEquals(
            listOf(SubjectSummary("수학", 2, hidden = true), SubjectSummary("영어", 1, hidden = false)),
            repository.allSubjects.first(),
        )
        assertEquals(listOf("영어"), repository.recentVisibleSubjects.first())
    }

    @Test
    fun 마지막_과목이_숨김이거나_없으면_자동_선택하지_않는다() = runTest {
        session("수학", startAt = 1)
        assertEquals("수학", repository.lastUsedVisibleSubject())

        repository.setHidden("수학", hidden = true)
        assertNull(repository.lastUsedVisibleSubject())

        session(null, startAt = 2)
        repository.setHidden("수학", hidden = false)
        assertNull(repository.lastUsedVisibleSubject())
    }

    @Test
    fun 이름을_바꾸면_과거_세션도_바뀌고_같은_이름과_합쳐진다() = runTest {
        session("수햑", startAt = 1)
        session("수학", startAt = 2)
        session("수학", startAt = 3)

        repository.rename("수햑", "수학")

        assertEquals(listOf(SubjectSummary("수학", 3, hidden = false)), repository.allSubjects.first())
    }

    @Test
    fun 숨긴_과목의_이름을_바꾸면_옛_이름은_숨김_목록에서_빠진다() = runTest {
        session("수햑", startAt = 1)
        repository.setHidden("수햑", hidden = true)

        repository.rename("수햑", "수학")

        assertEquals(listOf("수학"), repository.recentVisibleSubjects.first())
    }
}
