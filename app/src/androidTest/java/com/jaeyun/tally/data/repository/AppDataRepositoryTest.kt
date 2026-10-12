package com.jaeyun.tally.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jaeyun.tally.data.datastore.AppSettings
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.domain.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AppDataRepositoryTest {

    private lateinit var db: TallyDatabase
    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var exportDir: File
    private lateinit var settings: AppSettingsStore
    private lateinit var repository: AppDataRepository

    private val seoul = ZoneId.of("Asia/Seoul")

    // 2026-10-09 (금) 18:00 KST
    private val now = 1_791_536_400_000L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        dataStoreFile = File(context.cacheDir, "app-data-test-${System.nanoTime()}.preferences_pb")
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        settings = AppSettingsStore(PreferenceDataStoreFactory.create(scope = dataStoreScope) { dataStoreFile })
        exportDir = File(context.cacheDir, "app-data-test-exports-${System.nanoTime()}")
        repository = AppDataRepository(db, settings, exportDir, selfPackage = "com.jaeyun.tally")
    }

    @After
    fun tearDown() {
        db.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
        exportDir.deleteRecursively()
    }

    @Test
    fun 전체_삭제하면_세션과_분류와_설정과_내보낸_파일이_모두_지워진다() = runTest {
        db.studySessionDao().insert(StudySession(subjectName = "수학", startAt = 1, endAt = 2, createdAt = 1))
        db.appClassificationDao().upsert(
            AppClassification("com.kakao.talk", "카카오톡", Category.ALLOWED, ClassificationSource.USER),
        )
        settings.setOnboardingDone(true)
        repository.exportSessionsCsv(now, seoul)

        repository.deleteAll()

        assertFalse(repository.hasSessions.first())
        assertTrue(db.appClassificationDao().getAll().isEmpty())
        assertEquals(AppSettings(), settings.settings.first())
        assertFalse(exportDir.exists())
    }

    @Test
    fun 데모_데이터는_기록이_없을_때만_넣는다() = runTest {
        assertTrue(repository.insertDemoData(now, seoul))
        val count = db.studySessionDao().getFinished().size
        assertTrue(count > 0)

        assertFalse(repository.insertDemoData(now, seoul))
        assertEquals(count, db.studySessionDao().getFinished().size)
    }

    @Test
    fun 기록이_있으면_데모_데이터를_넣지_않는다() = runTest {
        db.studySessionDao().insert(StudySession(subjectName = null, startAt = 1, createdAt = 1))

        assertFalse(repository.insertDemoData(now, seoul))
        assertTrue(db.studySessionDao().getFinished().isEmpty())
    }

    @Test
    fun 데모_세션의_구간이_세션에_붙어_저장된다() = runTest {
        repository.insertDemoData(now, seoul)

        db.studySessionDao().getFinished().forEach { session ->
            val segments = db.appSegmentDao().getBySession(session.id)
            assertEquals(session.startAt, segments.first().startAt)
            assertEquals(session.endAt, segments.last().endAt)
        }
    }

    @Test
    fun CSV는_BOM으로_시작하고_끝난_세션만_쓴다() = runTest {
        db.studySessionDao().insert(StudySession(subjectName = "수학", startAt = 1, endAt = 2, createdAt = 1))
        db.studySessionDao().insert(StudySession(subjectName = "영어", startAt = 3, createdAt = 3))

        val text = repository.exportSessionsCsv(now, seoul).readText(Charsets.UTF_8)

        assertTrue(text.startsWith(CSV_BOM))
        assertEquals(3, text.split("\r\n").size)
    }
}
