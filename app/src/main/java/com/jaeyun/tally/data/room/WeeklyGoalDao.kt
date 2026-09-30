package com.jaeyun.tally.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WeeklyGoalDao {

    @Upsert
    suspend fun upsert(goal: WeeklyGoal)

    @Query("SELECT * FROM WeeklyGoal WHERE weekStartAt = :weekStartAt")
    suspend fun getByWeek(weekStartAt: Long): WeeklyGoal?
}
