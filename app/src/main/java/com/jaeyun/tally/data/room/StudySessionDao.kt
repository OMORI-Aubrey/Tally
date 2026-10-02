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

    // 과목 (§8.1.1). 과목은 세션의 라벨 문자열이라 별도 테이블 없이 세션에서 모은다

    /** 세션에 쓰인 과목별 세션 수와 마지막 세션 시각. 정렬은 저장소가 추가만 한 과목과 합친 뒤에 한다 */
    @Query(
        "SELECT subjectName AS name, COUNT(*) AS sessionCount, MAX(startAt) AS lastStartAt FROM StudySession " +
            "WHERE subjectName IS NOT NULL GROUP BY subjectName"
    )
    fun observeSubjectStats(): Flow<List<SubjectStat>>

    /** 가장 최근 세션의 과목. 세션이 없거나 과목 없이 시작했으면 null */
    @Query("SELECT subjectName FROM StudySession ORDER BY startAt DESC LIMIT 1")
    suspend fun getLastSubject(): String?

    /** 과거 세션까지 소급해 바꾼다. 바뀐 세션 수를 돌려준다 */
    @Query("UPDATE StudySession SET subjectName = :newName WHERE subjectName = :oldName")
    suspend fun renameSubject(oldName: String, newName: String): Int
}
