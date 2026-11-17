package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GoalEntity::class,
        MilestoneEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        TaskEntity::class,
        FocusSessionEntity::class,
        DailyReviewEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CadenceDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun dailyReviewDao(): DailyReviewDao

    companion object {
        @Volatile
        private var INSTANCE: CadenceDatabase? = null

        fun getInstance(context: Context): CadenceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CadenceDatabase::class.java,
                    "cadence_local_ssot.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
