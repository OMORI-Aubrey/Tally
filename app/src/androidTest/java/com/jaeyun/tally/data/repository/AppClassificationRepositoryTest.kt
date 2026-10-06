package com.jaeyun.tally.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.domain.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppClassificationRepositoryTest {

    private lateinit var db: TallyDatabase
    private lateinit var repository: AppClassificationRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        repository = AppClassificationRepository(db.appClassificationDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun 바꾸면_이름은_그대로_두고_사용자_분류가_된다() = runTest {
        db.appClassificationDao().upsert(AppClassification("com.google.android.youtube", "YouTube", Category.DISTRACT, ClassificationSource.AUTO_CATEGORY))

        repository.setCategory("com.google.android.youtube", Category.ALLOWED)

        assertEquals(
            AppClassification("com.google.android.youtube", "YouTube", Category.ALLOWED, ClassificationSource.USER),
            repository.detectedApps.first().single(),
        )
    }

    @Test
    fun 감지되지_않은_앱은_만들지_않는다() = runTest {
        repository.setCategory("com.example.unknown", Category.ALLOWED)

        assertNull(db.appClassificationDao().get("com.example.unknown"))
    }
}
