package com.jaeyun.tally.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Insert
    suspend fun insert(session: StudySession): Long

    @Update
    suspend fun update(session: StudySession)

    /** 구간(`AppSegment`)도 함께 지워진다 */
    @Delete
    suspend fun delete(session: StudySession)

    @Query("SELECT * FROM StudySession WHERE id = :id")
    suspend fun getById(id: Long): StudySession?

    /** 진행 중이거나 강제 종료로 끝나지 않은 세션 (§6.3 #5). 동시에 하나만 존재해야 한다 */
    @Query("SELECT * FROM StudySession WHERE endAt IS NULL ORDER BY startAt DESC LIMIT 1")
    suspend fun getUnfinished(): StudySession?

    /** [getUnfinished]를 관찰한다. 타이머 화면이 IDLE/RUNNING을 이 값으로 정한다 */
    @Query("SELECT * FROM StudySession WHERE endAt IS NULL ORDER BY startAt DESC LIMIT 1")
    fun observeUnfinished(): Flow<StudySession?>
}
