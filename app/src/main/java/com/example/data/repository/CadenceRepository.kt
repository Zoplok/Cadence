package com.example.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.core.DateUtils
import com.example.core.UuidV7
import com.example.data.local.CadenceDatabase
import com.example.data.local.DailyReviewEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.local.MilestoneEntity
import com.example.data.local.SyncMetadata
import com.example.data.local.SyncPreferences
import com.example.data.local.TaskEntity
import com.example.data.sync.DeltaSyncEngine
import com.example.data.sync.DeltaSyncWorker
import com.example.widget.CadenceHabitWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Single Source of Truth Repository:
 * All reads observe local Room SQLite Flows; all writes persist optimistically to Room first,
 * refresh the Glance Home Screen Widget, and trigger asynchronous background Delta Sync.
 */
class CadenceRepository(private val context: Context) {
    private val appContext = context.applicationContext
    private val db = CadenceDatabase.getInstance(appContext)
    private val prefs = SyncPreferences(appContext)
    private val syncEngine = DeltaSyncEngine(appContext, db, prefs)
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- Reactive Observations (Room SSOT) ---

    fun observeGoals(userId: String): Flow<List<GoalEntity>> = db.goalDao().observeGoals(userId)

    fun observeMilestones(userId: String): Flow<List<MilestoneEntity>> =
        db.milestoneDao().observeMilestones(userId)

    fun observeHabits(userId: String): Flow<List<HabitEntity>> = db.habitDao().observeHabits(userId)

    fun observeHabitLogsForDate(userId: String, logDate: String): Flow<List<HabitLogEntity>> =
        db.habitLogDao().observeLogsForDate(userId, logDate)

    fun observeHabitLogsSince(userId: String, startDate: String): Flow<List<HabitLogEntity>> =
        db.habitLogDao().observeLogsSince(userId, startDate)

    fun observeHabitStreaks(userId: String): Flow<Map<String, Int>> {
        val today = DateUtils.todayIso()
        val yesterday = DateUtils.isoDaysAgo(1)
        return combine(
            db.habitLogDao().observeCurrentStreaks(userId, today, yesterday)
        ) { arr ->
            arr[0].associate { it.habitId to it.currentStreak }
        }
    }

    fun observeTasks(userId: String): Flow<List<TaskEntity>> = db.taskDao().observeTasks(userId)

    fun observeFocusSessions(userId: String): Flow<List<FocusSessionEntity>> =
        db.focusSessionDao().observeSessions(userId)

    fun observeActiveFocusSession(userId: String): Flow<FocusSessionEntity?> =
        db.focusSessionDao().observeActiveSession(userId)

    fun observeDailyReviews(userId: String): Flow<List<DailyReviewEntity>> =
        db.dailyReviewDao().observeReviews(userId)

    fun observeDailyReviewForDate(userId: String, reviewDate: String): Flow<DailyReviewEntity?> =
        db.dailyReviewDao().observeReviewForDate(userId, reviewDate)

    fun observeSyncMetadata(userId: String): Flow<SyncMetadata> = prefs.observeMetadata(userId)

    fun observePendingDirtyCount(userId: String): Flow<Int> = combine(
        listOf(
            db.goalDao().observeDirtyCount(userId),
            db.milestoneDao().observeDirtyCount(userId),
            db.habitDao().observeDirtyCount(userId),
            db.habitLogDao().observeDirtyCount(userId),
            db.taskDao().observeDirtyCount(userId),
            db.focusSessionDao().observeDirtyCount(userId),
            db.dailyReviewDao().observeDirtyCount(userId)
        )
    ) { counts -> counts.sum() }

    // --- Sync Triggers ---

    suspend fun syncNow(userId: String): Result<Unit> {
        val result = syncEngine.sync(userId)
        refreshWidget()
        return result
    }

    private fun notifyLocalMutation() {
        refreshWidget()
        DeltaSyncWorker.enqueueImmediateSync(appContext)
    }

    private fun refreshWidget() {
        widgetScope.launch {
            runCatching { CadenceHabitWidget().updateAll(appContext) }
        }
    }

    // --- Goals & Milestones ---

    suspend fun createGoal(
        userId: String,
        title: String,
        category: String,
        targetDate: String?
    ) {
        val now = System.currentTimeMillis()
        db.goalDao().upsert(
            GoalEntity(
                id = UuidV7.generate(now),
                userId = userId,
                title = title.trim().take(120),
                category = category.trim().take(40).ifBlank { "Personal" },
                targetDate = targetDate?.takeIf { it.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) },
                status = "active",
                createdAtMs = now,
                updatedAtMs = now,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    suspend fun updateGoalStatus(goal: GoalEntity, newStatus: String) {
        val now = System.currentTimeMillis()
        db.goalDao().upsert(goal.copy(status = newStatus, updatedAtMs = now, dirty = true))
        notifyLocalMutation()
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        val now = System.currentTimeMillis()
        db.goalDao().upsert(goal.copy(deletedAtMs = now, updatedAtMs = now, dirty = true))
        notifyLocalMutation()
    }

    suspend fun createMilestone(
        userId: String,
        goalId: String,
        title: String,
        targetValue: Double?
    ) {
        val now = System.currentTimeMillis()
        db.milestoneDao().upsert(
            MilestoneEntity(
                id = UuidV7.generate(now),
                userId = userId,
                goalId = goalId,
                title = title.trim().take(120),
                targetValue = targetValue?.takeIf { it > 0.0 },
                currentValue = 0.0,
                isCompleted = false,
                createdAtMs = now,
                updatedAtMs = now,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    suspend fun toggleMilestone(milestone: MilestoneEntity) {
        val now = System.currentTimeMillis()
        val nextDone = !milestone.isCompleted
        val nextVal = if (nextDone && milestone.targetValue != null) milestone.targetValue else if (!nextDone) 0.0 else milestone.currentValue
        db.milestoneDao().upsert(
            milestone.copy(
                isCompleted = nextDone,
                currentValue = nextVal,
                updatedAtMs = now,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    suspend fun incrementMilestone(milestone: MilestoneEntity, delta: Double) {
        val now = System.currentTimeMillis()
        val nextVal = (milestone.currentValue + delta).coerceAtLeast(0.0)
        val completed = milestone.targetValue?.let { nextVal >= it } ?: milestone.isCompleted
        db.milestoneDao().upsert(
            milestone.copy(
                currentValue = nextVal,
                isCompleted = completed,
                updatedAtMs = now,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    // --- Habits & Logs ---

    suspend fun createHabit(
        userId: String,
        title: String,
        goalId: String?,
        iconName: String,
        colorHex: String,
        metricType: String,
        targetValue: Double,
        unit: String,
        frequencyType: String = "daily",
        frequencyDaysCsv: String = "1,2,3,4,5,6,7"
    ) {
        val now = System.currentTimeMillis()
        db.habitDao().upsert(
            HabitEntity(
                id = UuidV7.generate(now),
                userId = userId,
                goalId = goalId,
                title = title.trim().take(100),
                iconName = iconName,
                colorHex = colorHex,
                metricType = metricType,
                targetValue = targetValue.coerceAtLeast(1.0),
                unit = unit.trim().take(30),
                frequencyType = frequencyType,
                frequencyDaysCsv = frequencyDaysCsv,
                createdAtMs = now,
                updatedAtMs = now,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        val now = System.currentTimeMillis()
        db.habitDao().upsert(habit.copy(deletedAtMs = now, updatedAtMs = now, dirty = true))
        notifyLocalMutation()
    }

    suspend fun upsertHabitLog(
        userId: String,
        habitId: String,
        logDate: String,
        loggedValue: Double,
        note: String? = null
    ) {
        val now = System.currentTimeMillis()
        val existing = db.habitLogDao().getLogForHabitDate(userId, habitId, logDate)
        val deterministicId = existing?.id ?: "${habitId}_${logDate}"
        db.habitLogDao().upsert(
            HabitLogEntity(
                id = deterministicId,
                userId = userId,
                habitId = habitId,
                logDate = logDate,
                loggedValue = loggedValue.coerceAtLeast(0.0),
                note = note?.trim()?.take(500)?.ifBlank { null },
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
                deletedAtMs = null,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    suspend fun toggleOrStepHabit(userId: String, habit: HabitEntity, logDate: String = DateUtils.todayIso()) {
        val existing = db.habitLogDao().getLogForHabitDate(userId, habit.id, logDate)
        val currentVal = if (existing?.deletedAtMs == null) existing?.loggedValue ?: 0.0 else 0.0
        val nextVal = when (habit.metricType) {
            "boolean" -> if (currentVal >= habit.targetValue) 0.0 else habit.targetValue
            "numeric" -> {
                val step = when {
                    habit.targetValue >= 1000 -> 250.0
                    habit.targetValue >= 20 -> 5.0
                    else -> 1.0
                }
                if (currentVal >= habit.targetValue) 0.0 else (currentVal + step).coerceAtMost(habit.targetValue * 2)
            }
            "duration" -> {
                val step = if (habit.targetValue >= 30) 15.0 else 5.0
                if (currentVal >= habit.targetValue) 0.0 else (currentVal + step).coerceAtMost(habit.targetValue * 2)
            }
            else -> if (currentVal >= habit.targetValue) 0.0 else habit.targetValue
        }
        upsertHabitLog(userId, habit.id, logDate, nextVal)
    }

    // --- Tasks & Time-Blocking ---

    suspend fun createTask(
        userId: String,
        title: String,
        priority: Int,
        goalId: String? = null,
        parentTaskId: String? = null,
        dueDate: String? = DateUtils.todayIso(),
        scheduledStartMs: Long? = null,
        scheduledEndMs: Long? = null,
        estimatedMinutes: Int = 30
    ) {
        val now = System.currentTimeMillis()
        db.taskDao().upsert(
            TaskEntity(
                id = UuidV7.generate(now),
                userId = userId,
                goalId = goalId,
                parentTaskId = parentTaskId,
                title = title.trim().take(160),
                priority = priority.coerceIn(1, 4),
                status = "todo",
                dueDate = dueDate,
                scheduledStartMs = scheduledStartMs,
                scheduledEndMs = scheduledEndMs,
                estimatedMinutes = estimatedMinutes.coerceIn(0, 1440),
                createdAtMs = now,
                updatedAtMs = now,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    suspend fun cycleTaskStatus(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val nextStatus = when (task.status) {
            "todo" -> "in_progress"
            "in_progress" -> "done"
            else -> "todo"
        }
        db.taskDao().upsert(task.copy(status = nextStatus, updatedAtMs = now, dirty = true))
        notifyLocalMutation()
    }

    suspend fun toggleTaskDone(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val nextStatus = if (task.status == "done") "todo" else "done"
        db.taskDao().upsert(task.copy(status = nextStatus, updatedAtMs = now, dirty = true))
        notifyLocalMutation()
    }

    suspend fun scheduleTaskBlock(task: TaskEntity, startMs: Long, endMs: Long) {
        val now = System.currentTimeMillis()
        db.taskDao().upsert(
            task.copy(
                scheduledStartMs = startMs,
                scheduledEndMs = endMs,
                dueDate = task.dueDate ?: DateUtils.todayIso(),
                updatedAtMs = now,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    suspend fun deleteTask(task: TaskEntity) {
        val now = System.currentTimeMillis()
        db.taskDao().upsert(task.copy(deletedAtMs = now, updatedAtMs = now, dirty = true))
        notifyLocalMutation()
    }

    // --- Focus & Pomodoro Sessions (Doze-Safe: startedAtMs persisted in SQLite) ---

    suspend fun startFocusSession(
        userId: String,
        sessionType: String,
        targetDurationSeconds: Int,
        taskId: String? = null,
        habitId: String? = null
    ) {
        val now = System.currentTimeMillis()
        // Finish any stray active session cleanly
        val active = db.focusSessionDao().getActiveSessionSnapshot(userId)
        if (active != null) {
            val elapsed = ((now - active.startedAtMs) / 1000L).toInt().coerceIn(0, 86400)
            db.focusSessionDao().upsert(
                active.copy(
                    endedAtMs = now,
                    durationSeconds = elapsed,
                    isActive = false,
                    updatedAtMs = now,
                    dirty = true
                )
            )
        }
        db.focusSessionDao().upsert(
            FocusSessionEntity(
                id = UuidV7.generate(now),
                userId = userId,
                taskId = taskId,
                habitId = habitId,
                startedAtMs = now,
                endedAtMs = null,
                durationSeconds = targetDurationSeconds.coerceIn(0, 86400),
                sessionType = sessionType,
                isActive = true,
                createdAtMs = now,
                updatedAtMs = now,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    suspend fun finishFocusSession(session: FocusSessionEntity, markLinkedCompleted: Boolean = true) {
        val now = System.currentTimeMillis()
        val actualElapsed = ((now - session.startedAtMs) / 1000L).toInt().coerceIn(1, 86400)
        db.focusSessionDao().upsert(
            session.copy(
                endedAtMs = now,
                durationSeconds = actualElapsed,
                isActive = false,
                updatedAtMs = now,
                dirty = true
            )
        )
        if (markLinkedCompleted && session.habitId != null) {
            val habit = db.habitDao().getById(session.habitId)
            if (habit != null && habit.metricType == "duration") {
                val mins = (actualElapsed / 60.0).coerceAtLeast(1.0)
                val existing = db.habitLogDao().getLogForHabitDate(session.userId, habit.id, DateUtils.todayIso())
                val prev = if (existing?.deletedAtMs == null) existing?.loggedValue ?: 0.0 else 0.0
                upsertHabitLog(session.userId, habit.id, DateUtils.todayIso(), prev + mins)
                return
            }
        }
        notifyLocalMutation()
    }

    // --- Daily Reviews ---

    suspend fun saveDailyReview(
        userId: String,
        reviewDate: String,
        moodScore: Int,
        energyScore: Int,
        winsText: String,
        blockersText: String
    ) {
        val now = System.currentTimeMillis()
        val id = "review_$reviewDate"
        val existing = db.dailyReviewDao().getById(id)
        db.dailyReviewDao().upsert(
            DailyReviewEntity(
                id = id,
                userId = userId,
                reviewDate = reviewDate,
                moodScore = moodScore.coerceIn(1, 5),
                energyScore = energyScore.coerceIn(1, 5),
                winsText = winsText.trim().take(2000),
                blockersText = blockersText.trim().take(2000),
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
                deletedAtMs = null,
                dirty = true
            )
        )
        notifyLocalMutation()
    }

    // --- Optional Starter Routine (User-Initiated Onboarding Template) ---

    suspend fun seedStarterRoutine(userId: String) {
        val now = System.currentTimeMillis()
        val today = DateUtils.todayIso()

        val goalId = UuidV7.generate(now)
        db.goalDao().upsert(
            GoalEntity(
                id = goalId,
                userId = userId,
                title = "Build Peak Daily Focus & Energy",
                category = "Health",
                targetDate = DateUtils.shiftIsoDate(today, 30),
                status = "active",
                createdAtMs = now,
                updatedAtMs = now,
                dirty = true
            )
        )
        db.milestoneDao().upsert(
            MilestoneEntity(
                id = UuidV7.generate(now + 1),
                userId = userId,
                goalId = goalId,
                title = "Complete 14-day hydration & deep work streak",
                targetValue = 14.0,
                currentValue = 1.0,
                isCompleted = false,
                createdAtMs = now + 1,
                updatedAtMs = now + 1,
                dirty = true
            )
        )
        val h1 = UuidV7.generate(now + 2)
        val h2 = UuidV7.generate(now + 3)
        val h3 = UuidV7.generate(now + 4)
        db.habitDao().upsert(
            HabitEntity(
                id = h1, userId = userId, goalId = goalId,
                title = "Morning Breathwork", iconName = "self_improvement", colorHex = "#178A5E",
                metricType = "boolean", targetValue = 1.0, unit = "session",
                createdAtMs = now + 2, updatedAtMs = now + 2, dirty = true
            )
        )
        db.habitDao().upsert(
            HabitEntity(
                id = h2, userId = userId, goalId = goalId,
                title = "Hydration Target", iconName = "water_drop", colorHex = "#2D5BFF",
                metricType = "numeric", targetValue = 2500.0, unit = "ml",
                createdAtMs = now + 3, updatedAtMs = now + 3, dirty = true
            )
        )
        db.habitDao().upsert(
            HabitEntity(
                id = h3, userId = userId, goalId = goalId,
                title = "Deep Coding / Study", iconName = "code", colorHex = "#D95B38",
                metricType = "duration", targetValue = 90.0, unit = "mins",
                createdAtMs = now + 4, updatedAtMs = now + 4, dirty = true
            )
        )
        val blockStart = DateUtils.todayAtHourMinute(9, 0)
        val blockEnd = DateUtils.todayAtHourMinute(10, 30)
        db.taskDao().upsert(
            TaskEntity(
                id = UuidV7.generate(now + 5), userId = userId, goalId = goalId,
                title = "Deep Work Architecture Sprint", priority = 4, status = "todo",
                dueDate = today, scheduledStartMs = blockStart, scheduledEndMs = blockEnd,
                estimatedMinutes = 90, createdAtMs = now + 5, updatedAtMs = now + 5, dirty = true
            )
        )
        db.taskDao().upsert(
            TaskEntity(
                id = UuidV7.generate(now + 6), userId = userId, goalId = goalId,
                title = "Review daily priorities & close open loops", priority = 2, status = "todo",
                dueDate = today, scheduledStartMs = DateUtils.todayAtHourMinute(16, 30),
                scheduledEndMs = DateUtils.todayAtHourMinute(17, 0),
                estimatedMinutes = 30, createdAtMs = now + 6, updatedAtMs = now + 6, dirty = true
            )
        )
        notifyLocalMutation()
    }
}
