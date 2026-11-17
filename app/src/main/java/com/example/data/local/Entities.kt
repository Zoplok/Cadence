package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [Index(value = ["userId", "status"]), Index(value = ["userId", "dirty"])]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val targetDate: String? = null,
    val status: String = "active", // active | completed | archived
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val deletedAtMs: Long? = null,
    val dirty: Boolean = true
)

@Entity(
    tableName = "milestones",
    indices = [Index(value = ["userId", "goalId"]), Index(value = ["userId", "dirty"])]
)
data class MilestoneEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalId: String,
    val title: String,
    val targetValue: Double? = null,
    val currentValue: Double = 0.0,
    val isCompleted: Boolean = false,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val deletedAtMs: Long? = null,
    val dirty: Boolean = true
)

@Entity(
    tableName = "habits",
    indices = [Index(value = ["userId", "deletedAtMs"]), Index(value = ["userId", "dirty"])]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalId: String? = null,
    val title: String,
    val iconName: String = "check_circle",
    val colorHex: String = "#2D5BFF",
    val metricType: String = "boolean", // boolean | numeric | duration
    val targetValue: Double = 1.0,
    val unit: String = "",
    val frequencyType: String = "daily", // daily | weekly
    val frequencyDaysCsv: String = "1,2,3,4,5,6,7",
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val deletedAtMs: Long? = null,
    val dirty: Boolean = true
)

@Entity(
    tableName = "habit_logs",
    indices = [
        Index(value = ["userId", "habitId", "logDate"], unique = true),
        Index(value = ["logDate"]),
        Index(value = ["userId", "dirty"])
    ]
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val habitId: String,
    val logDate: String, // YYYY-MM-DD
    val loggedValue: Double,
    val note: String? = null,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val deletedAtMs: Long? = null,
    val dirty: Boolean = true
)

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["userId", "status"]),
        Index(value = ["userId", "dueDate"]),
        Index(value = ["userId", "dirty"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalId: String? = null,
    val parentTaskId: String? = null,
    val title: String,
    val priority: Int = 2, // 1..4
    val status: String = "todo", // todo | in_progress | done
    val dueDate: String? = null,
    val scheduledStartMs: Long? = null,
    val scheduledEndMs: Long? = null,
    val estimatedMinutes: Int = 30,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val deletedAtMs: Long? = null,
    val dirty: Boolean = true
)

@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["userId", "startedAtMs"]), Index(value = ["userId", "dirty"])]
)
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val taskId: String? = null,
    val habitId: String? = null,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val durationSeconds: Int,
    val sessionType: String = "pomodoro", // pomodoro | deep_work | stopwatch
    val isActive: Boolean = true,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val deletedAtMs: Long? = null,
    val dirty: Boolean = true
)

@Entity(
    tableName = "daily_reviews",
    indices = [
        Index(value = ["userId", "reviewDate"], unique = true),
        Index(value = ["userId", "dirty"])
    ]
)
data class DailyReviewEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val reviewDate: String, // YYYY-MM-DD
    val moodScore: Int = 3, // 1..5
    val energyScore: Int = 3, // 1..5
    val winsText: String = "",
    val blockersText: String = "",
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val deletedAtMs: Long? = null,
    val dirty: Boolean = true
)

data class HabitStreakRow(
    val habitId: String,
    val currentStreak: Int
)
