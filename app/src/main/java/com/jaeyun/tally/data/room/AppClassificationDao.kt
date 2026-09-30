package com.jaeyun.tally.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

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

    @Query("SELECT * FROM AppClassification ORDER BY appLabel")
    suspend fun getAll(): List<AppClassification>
}
