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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppDataRepositoryTest {

    private lateinit var db: TallyDatabase
    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var settings: AppSettingsStore
    private lateinit var repository: AppDataRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        dataStoreFile = File(context.cacheDir, "app-data-test-${System.nanoTime()}.preferences_pb")
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        settings = AppSettingsStore(PreferenceDataStoreFactory.create(scope = dataStoreScope) { dataStoreFile })
        repository = AppDataRepository(db, settings)
    }

    @After
    fun tearDown() {
        db.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    @Test
    fun 전체_삭제하면_세션과_분류와_설정이_모두_지워진다() = runTest {
        val id = db.studySessionDao().insert(StudySession(subjectName = "수학", startAt = 1, endAt = 2, createdAt = 1))
        db.appClassificationDao().upsert(
            AppClassification("com.kakao.talk", "카카오톡", Category.ALLOWED, ClassificationSource.USER),
        )
        settings.setOnboardingDone(true)

        repository.deleteAll()

        assertNull(db.studySessionDao().getById(id))
        assertTrue(db.appClassificationDao().getAll().isEmpty())
        assertEquals(AppSettings(), settings.settings.first())
    }
}
