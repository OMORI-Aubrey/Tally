package com.jaeyun.tally.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 파일 기반 DataStore는 Windows JVM에서 임시 파일을 기존 파일 위로 rename하지 못해 두 번째 쓰기에서 실패한다.
 * 저장 형식이 아니라 키·기본값 규칙을 검증하는 테스트이므로 메모리 구현을 쓴다.
 */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}

class AppSettingsStoreTest {

    private fun store() = AppSettingsStore(InMemoryPreferencesDataStore())

    @Test
    fun `처음에는 기본값을 준다`() = runTest {
        assertEquals(AppSettings(), store().settings.first())
    }

    @Test
    fun `저장한 값을 그대로 읽는다`() = runTest {
        val store = store()
        store.setOnboardingDone(true)
        store.setSelfReportedDailyMin(180)
        store.setRelaxedBaseline(true)

        val settings = store.settings.first()
        assertEquals(true, settings.onboardingDone)
        assertEquals(180, settings.selfReportedDailyMin)
        assertEquals(true, settings.relaxedBaseline)
    }

    @Test
    fun `자기보고를 건너뛰면 값이 지워진다`() = runTest {
        val store = store()
        store.setSelfReportedDailyMin(120)
        store.setSelfReportedDailyMin(null)

        assertNull(store.settings.first().selfReportedDailyMin)
    }

    @Test
    fun `과목 숨기기와 되돌리기`() = runTest {
        val store = store()
        store.setSubjectHidden("수햑", hidden = true)
        store.setSubjectHidden("국어", hidden = true)
        store.setSubjectHidden("국어", hidden = false)

        assertEquals(setOf("수햑"), store.settings.first().hiddenSubjects)
    }
}
