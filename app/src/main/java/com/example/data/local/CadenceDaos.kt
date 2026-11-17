package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId AND deletedAtMs IS NULL ORDER BY createdAtMs DESC")
    fun observeGoals(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND dirty = 1")
    suspend fun getDirtyGoals(userId: String): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Query("UPDATE goals SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND dirty = 1")
    fun observeDirtyCount(userId: String): Flow<Int>
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE userId = :userId AND deletedAtMs IS NULL ORDER BY createdAtMs ASC")
    fun observeMilestones(userId: String): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones WHERE userId = :userId AND dirty = 1")
    suspend fun getDirtyMilestones(userId: String): List<MilestoneEntity>

    @Query("SELECT * FROM milestones WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MilestoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(milestone: MilestoneEntity)

    @Query("UPDATE milestones SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM milestones WHERE userId = :userId AND dirty = 1")
    fun observeDirtyCount(userId: String): Flow<Int>
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId AND deletedAtMs IS NULL ORDER BY createdAtMs ASC")
    fun observeHabits(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND deletedAtMs IS NULL ORDER BY createdAtMs ASC")
    suspend fun getActiveHabitsSnapshot(userId: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE userId = :userId AND dirty = 1")
    suspend fun getDirtyHabits(userId: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Query("UPDATE habits SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM habits WHERE userId = :userId AND dirty = 1")
    fun observeDirtyCount(userId: String): Flow<Int>
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE userId = :userId AND logDate = :logDate AND deletedAtMs IS NULL")
    fun observeLogsForDate(userId: String, logDate: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE userId = :userId AND logDate >= :startDate AND deletedAtMs IS NULL")
    fun observeLogsSince(userId: String, startDate: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE userId = :userId AND logDate = :logDate AND deletedAtMs IS NULL")
    suspend fun getLogsForDateSnapshot(userId: String, logDate: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE userId = :userId AND habitId = :habitId AND logDate = :logDate LIMIT 1")
    suspend fun getLogForHabitDate(userId: String, habitId: String, logDate: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE userId = :userId AND dirty = 1")
    suspend fun getDirtyLogs(userId: String): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: HabitLogEntity)

    @Query("UPDATE habit_logs SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM habit_logs WHERE userId = :userId AND dirty = 1")
    fun observeDirtyCount(userId: String): Flow<Int>

    /**
     * Dynamically computes consecutive streaks from habit_logs using a SQLite recursive CTE
     * so retroactive logs immediately update streaks without fragile cached integer columns.
     */
    @Query(
        """
        WITH RECURSIVE
          completed_dates(habitId, logDate) AS (
            SELECT l.habitId, l.logDate
            FROM habit_logs l
            INNER JOIN habits h ON h.id = l.habitId
            WHERE l.userId = :userId
              AND l.deletedAtMs IS NULL
              AND h.deletedAtMs IS NULL
              AND l.loggedValue >= h.targetValue
          ),
          streak_walk(habitId, curDate, depth) AS (
            SELECT c.habitId, c.logDate, 1
            FROM completed_dates c
            WHERE c.logDate = :todayIso
               OR (c.logDate = :yesterdayIso AND NOT EXISTS (
                    SELECT 1 FROM completed_dates t
                    WHERE t.habitId = c.habitId AND t.logDate = :todayIso
                  ))
            UNION ALL
            SELECT s.habitId, date(s.curDate, '-1 day'), s.depth + 1
            FROM streak_walk s
            INNER JOIN completed_dates c
              ON c.habitId = s.habitId AND c.logDate = date(s.curDate, '-1 day')
          )
        SELECT habitId, MAX(depth) AS currentStreak
        FROM streak_walk
        GROUP BY habitId
        """
    )
    fun observeCurrentStreaks(userId: String, todayIso: String, yesterdayIso: String): Flow<List<HabitStreakRow>>
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId AND deletedAtMs IS NULL ORDER BY CASE status WHEN 'done' THEN 1 ELSE 0 END ASC, priority DESC, createdAtMs DESC")
    fun observeTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND dirty = 1")
    suspend fun getDirtyTasks(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND dirty = 1")
    fun observeDirtyCount(userId: String): Flow<Int>
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE userId = :userId AND deletedAtMs IS NULL ORDER BY startedAtMs DESC")
    fun observeSessions(userId: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId AND isActive = 1 AND deletedAtMs IS NULL ORDER BY startedAtMs DESC LIMIT 1")
    fun observeActiveSession(userId: String): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId AND isActive = 1 AND deletedAtMs IS NULL ORDER BY startedAtMs DESC LIMIT 1")
    suspend fun getActiveSessionSnapshot(userId: String): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId AND dirty = 1")
    suspend fun getDirtySessions(userId: String): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FocusSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: FocusSessionEntity)

    @Query("UPDATE focus_sessions SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE userId = :userId AND dirty = 1")
    fun observeDirtyCount(userId: String): Flow<Int>
}

@Dao
interface DailyReviewDao {
    @Query("SELECT * FROM daily_reviews WHERE userId = :userId AND deletedAtMs IS NULL ORDER BY reviewDate DESC")
    fun observeReviews(userId: String): Flow<List<DailyReviewEntity>>

    @Query("SELECT * FROM daily_reviews WHERE userId = :userId AND reviewDate = :reviewDate AND deletedAtMs IS NULL LIMIT 1")
    fun observeReviewForDate(userId: String, reviewDate: String): Flow<DailyReviewEntity?>

    @Query("SELECT * FROM daily_reviews WHERE userId = :userId AND dirty = 1")
    suspend fun getDirtyReviews(userId: String): List<DailyReviewEntity>

    @Query("SELECT * FROM daily_reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DailyReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: DailyReviewEntity)

    @Query("UPDATE daily_reviews SET dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM daily_reviews WHERE userId = :userId AND dirty = 1")
    fun observeDirtyCount(userId: String): Flow<Int>
}
