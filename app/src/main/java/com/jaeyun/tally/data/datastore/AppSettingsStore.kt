package com.jaeyun.tally.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** 세션 데이터가 아닌 앱 설정 (§9). Room에 두지 않는다 */
data class AppSettings(
    val onboardingDone: Boolean = false,
    /** 온보딩 자기보고 "하루 평균 공부 시간(분)". 건너뛰면 null. 1주차 격차 대조값으로만 쓴다 (§5.8) */
    val selfReportedDailyMin: Int? = null,
    /** 칩 목록에서 숨긴 과목. 세션 데이터는 그대로 둔다 (§8.1.1) */
    val hiddenSubjects: Set<String> = emptySet(),
    /** 기준선 활성 조건 완화(12 → 6세션). debug 빌드에서만 효과가 있다 (§5.3) */
    val relaxedBaseline: Boolean = false,
)

/** 파일 하나당 인스턴스가 하나여야 하므로 `AppContainer`에서만 만든다 */
class AppSettingsStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                onboardingDone = prefs[ONBOARDING_DONE] ?: false,
                selfReportedDailyMin = prefs[SELF_REPORTED_DAILY_MIN],
                hiddenSubjects = prefs[HIDDEN_SUBJECTS].orEmpty(),
                relaxedBaseline = prefs[RELAXED_BASELINE] ?: false,
            )
        }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[ONBOARDING_DONE] = done }
    }

    /** null이면 값을 지운다(건너뛰기) */
    suspend fun setSelfReportedDailyMin(minutes: Int?) {
        dataStore.edit {
            if (minutes == null) it.remove(SELF_REPORTED_DAILY_MIN) else it[SELF_REPORTED_DAILY_MIN] = minutes
        }
    }

    suspend fun setSubjectHidden(subjectName: String, hidden: Boolean) {
        dataStore.edit {
            val current = it[HIDDEN_SUBJECTS].orEmpty()
            it[HIDDEN_SUBJECTS] = if (hidden) current + subjectName else current - subjectName
        }
    }

    suspend fun setRelaxedBaseline(enabled: Boolean) {
        dataStore.edit { it[RELAXED_BASELINE] = enabled }
    }

    companion object {
        private const val FILE_NAME = "settings"

        private val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
        private val SELF_REPORTED_DAILY_MIN = intPreferencesKey("selfReportedDailyMin")
        private val HIDDEN_SUBJECTS = stringSetPreferencesKey("hiddenSubjects")
        private val RELAXED_BASELINE = booleanPreferencesKey("relaxedBaseline")

        fun create(context: Context): AppSettingsStore = AppSettingsStore(
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(FILE_NAME) },
        )
    }
}
