package com.jaeyun.tally.data.repository

import androidx.room.withTransaction
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.domain.metrics.SessionValidity
import kotlinx.coroutines.flow.Flow

/** 세션 시작·종료 (§6.3 #5). 진행 중 세션은 동시에 하나만 둔다 */
class SessionRepository(private val database: TallyDatabase) {

    private val dao = database.studySessionDao()

    /** 진행 중 세션. 없으면 null */
    val runningSession: Flow<StudySession?> = dao.observeUnfinished()

    /**
     * 시작 즉시 저장해 앱이 강제 종료·재부팅돼도 시작 시각이 남게 한다.
     * 이미 진행 중인 세션이 있으면(버튼 연타 등) 새로 만들지 않고 그 세션 id를 돌려준다.
     */
    suspend fun start(subjectName: String?, now: Long): Long = database.withTransaction {
        dao.getUnfinished()?.id
            ?: dao.insert(StudySession(subjectName = subjectName, startAt = now, createdAt = now))
    }

    /**
     * 종료 시각과 세션 길이·통계 포함 여부를 저장하고 종료된 세션을 돌려준다.
     * 이미 끝났거나 없는 세션이면 아무것도 바꾸지 않고 null을 돌려준다(종료 버튼 연타 등).
     *
     * P1에서는 `tTotalSec`가 세션 전체 길이다. P2 지표 계산이 붙으면 그 결과로 다시 채운다.
     */
    suspend fun finish(sessionId: Long, now: Long): StudySession? = database.withTransaction {
        val session = dao.getById(sessionId)
        if (session == null || session.endAt != null) {
            null
        } else {
            val sessionSec = ((now - session.startAt) / 1000).coerceAtLeast(0).toInt()
            session.copy(
                endAt = now,
                tTotalSec = sessionSec,
                isValidForStats = SessionValidity.isValidForStats(sessionSec),
            ).also { dao.update(it) }
        }
    }
}
