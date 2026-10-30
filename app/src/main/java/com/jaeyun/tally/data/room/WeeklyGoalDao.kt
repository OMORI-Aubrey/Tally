package com.jaeyun.tally.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WeeklyGoalDao {

    @Upsert
    suspend fun upsert(goal: WeeklyGoal)

    /** 그 주 목표가 이미 있으면 그대로 둔다. 같은 주에 두 번 만들거나 제시한 목표를 덮어쓰지 않는다 (§9) */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(goal: WeeklyGoal)

    @Query("SELECT * FROM WeeklyGoal WHERE weekStartAt = :weekStartAt")
    suspend fun getByWeek(weekStartAt: Long): WeeklyGoal?

    /** [weekStartAt]보다 앞선 주의 목표 중 달성 여부를 아직 채우지 않은 것. 오래된 순서 */
    @Query("SELECT * FROM WeeklyGoal WHERE weekStartAt < :weekStartAt AND isAchieved IS NULL ORDER BY weekStartAt")
    suspend fun getUnjudgedBefore(weekStartAt: Long): List<WeeklyGoal>
}
