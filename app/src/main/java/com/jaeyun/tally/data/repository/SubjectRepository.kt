package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.room.StudySessionDao
import com.jaeyun.tally.data.room.SubjectStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 과목 시트 한 줄. [hidden]이면 칩에서만 빠지고 시트에는 흐리게 남는다. 추가만 한 과목은 [sessionCount]가 0 */
data class SubjectSummary(val name: String, val sessionCount: Int, val hidden: Boolean)

/** [mergeSubjects] 결과. [lastUsedAt]은 마지막 세션 시각과 추가한 시각 중 늦은 쪽 */
internal data class SubjectEntry(val summary: SubjectSummary, val lastUsedAt: Long)

/** 세션에 쓰인 과목과 시트에서 추가만 한 과목을 합친다 */
internal fun mergeSubjects(
    stats: List<SubjectStat>,
    added: Map<String, Long>,
    hidden: Set<String>,
): List<SubjectEntry> {
    val byName = stats.associateBy { it.name }
    return (byName.keys + added.keys).map { name ->
        val stat = byName[name]
        SubjectEntry(
            summary = SubjectSummary(name, sessionCount = stat?.sessionCount ?: 0, hidden = name in hidden),
            lastUsedAt = maxOf(stat?.lastStartAt ?: Long.MIN_VALUE, added[name] ?: Long.MIN_VALUE),
        )
    }
}

/** 칩 후보: 숨긴 과목을 빼고 최근에 쓰거나 추가한 순서 */
internal fun List<SubjectEntry>.recentVisible(): List<String> =
    filterNot { it.summary.hidden }.sortedByDescending { it.lastUsedAt }.map { it.summary.name }

/** 시트 목록: 세션 많은 순(§8.1.1). 같으면 최근 순 */
internal fun List<SubjectEntry>.forSheet(): List<SubjectSummary> =
    sortedWith(compareByDescending<SubjectEntry> { it.summary.sessionCount }.thenByDescending { it.lastUsedAt })
        .map { it.summary }

/**
 * 과목 조회·추가·이름 바꾸기·숨기기 (§8.1.1). 과목은 세션의 `subjectName` 문자열에서 모으고, 아직 세션에 쓰지 않은
 * 추가 과목과 칩에서 숨긴 과목은 DataStore에 둔다. 삭제는 두지 않는다.
 */
class SubjectRepository(
    private val dao: StudySessionDao,
    private val settings: AppSettingsStore,
) {
    private val entries: Flow<List<SubjectEntry>> =
        combine(dao.observeSubjectStats(), settings.settings) { stats, s ->
            mergeSubjects(stats, s.addedSubjects, s.hiddenSubjects)
        }

    /** 칩 후보. 최근에 쓰거나 추가한 순서, 숨긴 과목 제외 */
    val recentVisibleSubjects: Flow<List<String>> = entries.map { it.recentVisible() }

    /** 시트 목록. 세션 많은 순 */
    val allSubjects: Flow<List<SubjectSummary>> = entries.map { it.forSheet() }

    /** 마지막 세션의 과목. 과목 없이 시작했거나 칩에서 숨긴 과목이면 null */
    suspend fun lastUsedVisibleSubject(): String? {
        val last = dao.getLastSubject() ?: return null
        return last.takeUnless { it in settings.settings.first().hiddenSubjects }
    }

    /** 시트에서 새 과목을 추가한다. 숨긴 과목 이름이면 다시 보이게 한다 */
    suspend fun add(name: String, now: Long) = settings.addSubject(name, addedAt = now)

    /**
     * 과거 세션까지 소급해 바꾼다(§8.1.1). [newName]이 이미 있는 과목이면 두 과목이 합쳐진다.
     * 옛 이름은 더 이상 없으므로 숨김 목록에서도 뺀다.
     */
    suspend fun rename(oldName: String, newName: String) {
        if (oldName == newName) return
        dao.renameSubject(oldName, newName)
        settings.renameSubject(oldName, newName)
    }

    suspend fun setHidden(name: String, hidden: Boolean) = settings.setSubjectHidden(name, hidden)
}
