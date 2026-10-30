package com.jaeyun.tally.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AppSegmentDao {

    @Insert
    suspend fun insertAll(segments: List<AppSegment>)

    @Query("SELECT * FROM AppSegment WHERE sessionId = :sessionId ORDER BY startAt")
    suspend fun getBySession(sessionId: Long): List<AppSegment>

    /** 여러 세션의 구간 (기록 탭 공부 중 켠 앱, §8.4) */
    @Query("SELECT * FROM AppSegment WHERE sessionId IN (:sessionIds) ORDER BY startAt")
    suspend fun getBySessions(sessionIds: List<Long>): List<AppSegment>

    /** 세션을 다시 복원할 때(재분류, §5.1) 기존 구간을 지운다 */
    @Query("DELETE FROM AppSegment WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}
