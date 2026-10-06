package com.jaeyun.tally.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    /**
     * 과목 시트에서 직접 추가한 과목 → 추가한 시각. 과목은 세션의 라벨이라 따로 저장하지 않지만(§12.2),
     * 아직 세션에 쓰지 않은 과목도 칩에 남아야 해서 여기에 둔다
     */
    val addedSubjects: Map<String, Long> = emptyMap(),
    /** 기준선 활성 조건 완화(12 → 6세션). debug 빌드에서만 효과가 있다 (§5.3) */
    val relaxedBaseline: Boolean = false,
    /**
     * 체감 집중 시간을 마지막으로 물은 세션의 시작 시각. 그 세션이 속한 주에는 다시 묻지 않는다(§3.4).
     * 건너뛰면 세션에 값이 남지 않아서 물었다는 사실을 따로 둔다 (PLAN.md §4)
     */
    val perceivedAskedAt: Long? = null,
    /**
     * 재부팅 뒤 미완료 세션 복구 질문에 `[이어서 기록]`으로 답한 시각 (§6.3 #5, #21). 같은 재부팅을 다시 묻지 않도록
     * 이 시각보다 뒤에 재부팅했을 때만 묻는다
     */
    val recoveryAnsweredAt: Long? = null,
    /** 임시(#21): 화면 꺼짐 확인 다이얼로그(①) 기준을 60분에서 1분으로 낮춘다. debug 빌드에서만 효과가 있다 */
    val quickScreenOffCheck: Boolean = false,
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
                addedSubjects = decodeAdded(prefs[ADDED_SUBJECTS].orEmpty()),
                relaxedBaseline = prefs[RELAXED_BASELINE] ?: false,
                perceivedAskedAt = prefs[PERCEIVED_ASKED_AT],
                recoveryAnsweredAt = prefs[RECOVERY_ANSWERED_AT],
                quickScreenOffCheck = prefs[QUICK_SCREEN_OFF_CHECK] ?: false,
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

    /** 과목을 추가한다. 이미 있으면 추가 시각만 새로 하고, 칩에서 숨긴 과목이었으면 다시 보이게 한다 */
    suspend fun addSubject(subjectName: String, addedAt: Long) {
        dataStore.edit {
            it[ADDED_SUBJECTS] = encodeAdded(decodeAdded(it[ADDED_SUBJECTS].orEmpty()) + (subjectName to addedAt))
            it[HIDDEN_SUBJECTS] = it[HIDDEN_SUBJECTS].orEmpty() - subjectName
        }
    }

    /**
     * 과목 이름이 바뀐 뒤 설정을 따라 고친다. 추가 기록은 새 이름으로 옮기고(이미 있으면 더 최근 시각),
     * 옛 이름은 숨김 목록에서 뺀다. 새 이름의 숨김 여부는 그대로 둔다.
     */
    suspend fun renameSubject(oldName: String, newName: String) {
        dataStore.edit {
            val added = decodeAdded(it[ADDED_SUBJECTS].orEmpty())
            val oldAddedAt = added[oldName]
            if (oldAddedAt != null) {
                val merged = maxOf(oldAddedAt, added[newName] ?: oldAddedAt)
                it[ADDED_SUBJECTS] = encodeAdded(added - oldName + (newName to merged))
            }
            it[HIDDEN_SUBJECTS] = it[HIDDEN_SUBJECTS].orEmpty() - oldName
        }
    }

    suspend fun setRelaxedBaseline(enabled: Boolean) {
        dataStore.edit { it[RELAXED_BASELINE] = enabled }
    }

    /** null이면 지운다 */
    suspend fun setPerceivedAskedAt(sessionStartAt: Long?) {
        dataStore.edit {
            if (sessionStartAt == null) it.remove(PERCEIVED_ASKED_AT) else it[PERCEIVED_ASKED_AT] = sessionStartAt
        }
    }

    suspend fun setRecoveryAnsweredAt(answeredAt: Long) {
        dataStore.edit { it[RECOVERY_ANSWERED_AT] = answeredAt }
    }

    suspend fun setQuickScreenOffCheck(enabled: Boolean) {
        dataStore.edit { it[QUICK_SCREEN_OFF_CHECK] = enabled }
    }

    companion object {
        private const val FILE_NAME = "settings"

        private val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
        private val SELF_REPORTED_DAILY_MIN = intPreferencesKey("selfReportedDailyMin")
        private val HIDDEN_SUBJECTS = stringSetPreferencesKey("hiddenSubjects")
        private val RELAXED_BASELINE = booleanPreferencesKey("relaxedBaseline")
        private val ADDED_SUBJECTS = stringSetPreferencesKey("addedSubjects")
        private val PERCEIVED_ASKED_AT = longPreferencesKey("perceivedAskedAt")
        private val RECOVERY_ANSWERED_AT = longPreferencesKey("recoveryAnsweredAt")
        private val QUICK_SCREEN_OFF_CHECK = booleanPreferencesKey("quickScreenOffCheck")

        // "추가시각\t이름"으로 저장한다. 과목 이름은 공백을 정리해 넣으므로 탭이 남지 않는다(SubjectName.normalize)
        private const val ADDED_SEPARATOR = '\t'

        private fun encodeAdded(added: Map<String, Long>): Set<String> =
            added.map { (name, addedAt) -> "$addedAt$ADDED_SEPARATOR$name" }.toSet()

        private fun decodeAdded(entries: Set<String>): Map<String, Long> = entries.mapNotNull { entry ->
            val at = entry.indexOf(ADDED_SEPARATOR)
            val addedAt = entry.substring(0, at.coerceAtLeast(0)).toLongOrNull()
            if (at < 0 || addedAt == null) null else entry.substring(at + 1) to addedAt
        }.toMap()

        fun create(context: Context): AppSettingsStore = AppSettingsStore(
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(FILE_NAME) },
        )
    }
}
