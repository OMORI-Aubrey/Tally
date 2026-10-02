package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.StudySessionDao
import kotlinx.coroutines.flow.first

/**
 * 체감 집중 시간 (§3.4). 세션 종료 직후, 실측을 보여주기 전에 주 1회만 묻는다.
 *
 * 물을지는 타이머 화면이 정한다(트리거 규칙은 ViewModel 함수). 여기서는 판단에 쓸 기록을 읽고 쓴다.
 * 답한 값은 세션의 `perceivedFocusMin`에, 물었다는 사실은 DataStore에 둔다 — 건너뛴 주도 다시 묻지 않기 위해서다.
 */
class PerceivedFocusRepository(
    private val dao: StudySessionDao,
    private val settings: AppSettingsStore,
) {

    suspend fun session(id: Long): StudySession? = dao.getById(id)

    /** `[startAt, endAt)`에 시작해 끝난 세션. 시작한 순서 */
    suspend fun finishedStartedBetween(startAt: Long, endAt: Long): List<StudySession> =
        dao.getFinishedStartedBetween(startAt, endAt)

    /** 마지막으로 체감을 물은 세션의 시작 시각. 물은 적이 없으면 null */
    suspend fun lastAskedAt(): Long? = settings.settings.first().perceivedAskedAt

    /** 화면을 띄우기 전에 적는다. 답하지 않고 앱을 닫아도 그 주에는 다시 묻지 않는다 */
    suspend fun markAsked(sessionStartAt: Long) = settings.setPerceivedAskedAt(sessionStartAt)

    suspend fun save(sessionId: Long, minutes: Int) = dao.setPerceivedFocus(sessionId, minutes)
}
