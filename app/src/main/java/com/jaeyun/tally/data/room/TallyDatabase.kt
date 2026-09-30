package com.jaeyun.tally.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 세션 데이터의 단일 진실 공급원 (§10.1). 엔티티 4종 (§9).
 *
 * 스키마는 `app/schemas/`에 내보낸다. 엔티티를 바꾸면 [version]을 올리고 마이그레이션을 작성한다.
 */
@Database(
    entities = [StudySession::class, AppSegment::class, AppClassification::class, WeeklyGoal::class],
    version = 1,
    exportSchema = true,
)
abstract class TallyDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao

    abstract fun appSegmentDao(): AppSegmentDao

    abstract fun appClassificationDao(): AppClassificationDao

    abstract fun weeklyGoalDao(): WeeklyGoalDao

    companion object {
        private const val NAME = "tally.db"

        fun create(context: Context): TallyDatabase =
            Room.databaseBuilder(context, TallyDatabase::class.java, NAME).build()
    }
}
