package com.jaeyun.tally.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppClassificationDao {

    /** 새로 감지된 앱의 자동 분류를 넣는다. 이미 있는 행(특히 사용자 분류)은 건드리지 않는다 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(classifications: List<AppClassification>)

    /** 사용자가 분류를 바꿀 때 쓴다 */
    @Upsert
    suspend fun upsert(classification: AppClassification)

    @Query("SELECT * FROM AppClassification WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppClassification?

    @Query("SELECT * FROM AppClassification WHERE packageName IN (:packageNames)")
    suspend fun getByPackages(packageNames: List<String>): List<AppClassification>

    /** 자동 분류를 새 규칙으로 갱신할 때 쓴다. 사용자 분류 행은 호출하는 쪽에서 미리 걸러낸다 */
    @Upsert
    suspend fun upsertAll(classifications: List<AppClassification>)

    @Query("SELECT * FROM AppClassification ORDER BY appLabel")
    suspend fun getAll(): List<AppClassification>

    /** 앱 분류 화면 (#18). 정렬은 화면이 한국어 순서로 한다 */
    @Query("SELECT * FROM AppClassification")
    fun observeAll(): Flow<List<AppClassification>>
}
