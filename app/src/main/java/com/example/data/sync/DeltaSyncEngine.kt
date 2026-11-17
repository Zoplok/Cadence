package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.local.CadenceDatabase
import com.example.data.local.DailyReviewEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.local.MilestoneEntity
import com.example.data.local.SyncPreferences
import com.example.data.local.TaskEntity
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Implements the 2-step Delta Sync protocol:
 * 1) Push dirty/soft-deleted Room records to Firestore with Last-Write-Wins (and MAX merge for habit logs).
 * 2) Pull Firestore records modified since lastPulledAtMs and upsert into Room SSOT.
 */
class DeltaSyncEngine(
    private val context: Context,
    private val database: CadenceDatabase = CadenceDatabase.getInstance(context),
    private val prefs: SyncPreferences = SyncPreferences(context),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    ),
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val syncMutex = Mutex()

    suspend fun sync(userId: String): Result<Unit> = syncMutex.withLock {
        val currentUid = auth.currentUser?.uid
        if (currentUid.isNullOrBlank() || currentUid != userId) {
            return Result.failure(IllegalStateException("User must be signed in with Google to sync."))
        }

        val userRoot = db.collection("users").document(userId)
        val lastPulledMs = prefs.getLastPulledAtMs(userId)
        prefs.setSyncing(userId)

        return try {
            pushAllDirty(userId, userRoot)
            val maxServerTsMs = pullAllDeltas(userId, userRoot, lastPulledMs)
            val newCursor = maxOf(System.currentTimeMillis(), maxServerTsMs)
            prefs.setSyncSuccess(userId, newCursor)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Delta sync failed for $userId: ${e.message}", e)
            prefs.setSyncError(userId, e.localizedMessage ?: "Sync failed")
            Result.failure(e)
        }
    }

    private suspend fun pushAllDirty(userId: String, userRoot: com.google.firebase.firestore.DocumentReference) {
        val goalDao = database.goalDao()
        for (goal in goalDao.getDirtyGoals(userId)) {
            pushGoal(userRoot.collection("goals"), goal)
            goalDao.markSynced(goal.id)
        }

        val milestoneDao = database.milestoneDao()
        for (milestone in milestoneDao.getDirtyMilestones(userId)) {
            pushMilestone(userRoot.collection("milestones"), milestone)
            milestoneDao.markSynced(milestone.id)
        }

        val habitDao = database.habitDao()
        for (habit in habitDao.getDirtyHabits(userId)) {
            pushHabit(userRoot.collection("habits"), habit)
            habitDao.markSynced(habit.id)
        }

        val habitLogDao = database.habitLogDao()
        for (log in habitLogDao.getDirtyLogs(userId)) {
            pushHabitLog(userRoot.collection("habitLogs"), log)
            habitLogDao.markSynced(log.id)
        }

        val taskDao = database.taskDao()
        for (task in taskDao.getDirtyTasks(userId)) {
            pushTask(userRoot.collection("tasks"), task)
            taskDao.markSynced(task.id)
        }

        val focusDao = database.focusSessionDao()
        for (session in focusDao.getDirtySessions(userId)) {
            pushFocusSession(userRoot.collection("focusSessions"), session)
            focusDao.markSynced(session.id)
        }

        val reviewDao = database.dailyReviewDao()
        for (review in reviewDao.getDirtyReviews(userId)) {
            pushDailyReview(userRoot.collection("dailyReviews"), review)
            reviewDao.markSynced(review.id)
        }
    }

    private suspend fun pushGoal(col: CollectionReference, g: GoalEntity) {
        val docRef = col.document(g.id)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to g.userId,
                    "title" to g.title.take(120),
                    "category" to g.category.take(40),
                    "targetDate" to g.targetDate,
                    "status" to g.status,
                    "updatedAtMs" to g.updatedAtMs,
                    "deletedAtMs" to g.deletedAtMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val remoteMs = snap.getLong("updatedAtMs") ?: 0L
                if (g.updatedAtMs >= remoteMs) {
                    val updates = mapOf<String, Any>(
                        "title" to g.title.take(120),
                        "category" to g.category.take(40),
                        "targetDate" to (g.targetDate ?: FieldValue.delete()),
                        "status" to g.status,
                        "updatedAtMs" to g.updatedAtMs,
                        "deletedAtMs" to (g.deletedAtMs ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun pushMilestone(col: CollectionReference, m: MilestoneEntity) {
        val docRef = col.document(m.id)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to m.userId,
                    "goalId" to m.goalId,
                    "title" to m.title.take(120),
                    "targetValue" to m.targetValue,
                    "currentValue" to m.currentValue,
                    "completed" to m.isCompleted,
                    "updatedAtMs" to m.updatedAtMs,
                    "deletedAtMs" to m.deletedAtMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val remoteMs = snap.getLong("updatedAtMs") ?: 0L
                if (m.updatedAtMs >= remoteMs) {
                    val updates = mapOf<String, Any>(
                        "goalId" to m.goalId,
                        "title" to m.title.take(120),
                        "targetValue" to (m.targetValue ?: FieldValue.delete()),
                        "currentValue" to m.currentValue,
                        "completed" to m.isCompleted,
                        "updatedAtMs" to m.updatedAtMs,
                        "deletedAtMs" to (m.deletedAtMs ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun pushHabit(col: CollectionReference, h: HabitEntity) {
        val docRef = col.document(h.id)
        val daysList = h.frequencyDaysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .take(7)
        val freqMap = mapOf(
            "type" to if (h.frequencyType == "weekly") "weekly" else "daily",
            "days" to daysList
        )
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to h.userId,
                    "goalId" to h.goalId,
                    "title" to h.title.take(100),
                    "iconName" to h.iconName.take(40),
                    "colorHex" to h.colorHex.take(16),
                    "metricType" to h.metricType,
                    "targetValue" to h.targetValue,
                    "unit" to h.unit.take(30),
                    "frequencyRule" to freqMap,
                    "updatedAtMs" to h.updatedAtMs,
                    "deletedAtMs" to h.deletedAtMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val remoteMs = snap.getLong("updatedAtMs") ?: 0L
                if (h.updatedAtMs >= remoteMs) {
                    val updates = mapOf<String, Any>(
                        "goalId" to (h.goalId ?: FieldValue.delete()),
                        "title" to h.title.take(100),
                        "iconName" to h.iconName.take(40),
                        "colorHex" to h.colorHex.take(16),
                        "metricType" to h.metricType,
                        "targetValue" to h.targetValue,
                        "unit" to h.unit.take(30),
                        "frequencyRule" to freqMap,
                        "updatedAtMs" to h.updatedAtMs,
                        "deletedAtMs" to (h.deletedAtMs ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun pushHabitLog(col: CollectionReference, l: HabitLogEntity) {
        val docRef = col.document(l.id)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to l.userId,
                    "habitId" to l.habitId,
                    "logDate" to l.logDate,
                    "loggedValue" to l.loggedValue,
                    "note" to l.note?.take(500),
                    "updatedAtMs" to l.updatedAtMs,
                    "deletedAtMs" to l.deletedAtMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val remoteMs = snap.getLong("updatedAtMs") ?: 0L
                val remoteVal = (snap.get("loggedValue") as? Number)?.toDouble() ?: 0.0
                val mergedVal = if (l.deletedAtMs == null && snap.getLong("deletedAtMs") == null) {
                    maxOf(l.loggedValue, remoteVal)
                } else {
                    l.loggedValue
                }
                if (l.updatedAtMs >= remoteMs || mergedVal > remoteVal) {
                    val updates = mapOf<String, Any>(
                        "habitId" to l.habitId,
                        "logDate" to l.logDate,
                        "loggedValue" to mergedVal,
                        "note" to (l.note?.take(500) ?: FieldValue.delete()),
                        "updatedAtMs" to maxOf(l.updatedAtMs, remoteMs),
                        "deletedAtMs" to (l.deletedAtMs ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun pushTask(col: CollectionReference, t: TaskEntity) {
        val docRef = col.document(t.id)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to t.userId,
                    "goalId" to t.goalId,
                    "parentTaskId" to t.parentTaskId,
                    "title" to t.title.take(160),
                    "priority" to t.priority.coerceIn(1, 4),
                    "status" to t.status,
                    "dueDate" to t.dueDate,
                    "scheduledStartMs" to t.scheduledStartMs,
                    "scheduledEndMs" to t.scheduledEndMs,
                    "estimatedMinutes" to t.estimatedMinutes.coerceIn(0, 1440),
                    "updatedAtMs" to t.updatedAtMs,
                    "deletedAtMs" to t.deletedAtMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val remoteMs = snap.getLong("updatedAtMs") ?: 0L
                if (t.updatedAtMs >= remoteMs) {
                    val updates = mapOf<String, Any>(
                        "goalId" to (t.goalId ?: FieldValue.delete()),
                        "parentTaskId" to (t.parentTaskId ?: FieldValue.delete()),
                        "title" to t.title.take(160),
                        "priority" to t.priority.coerceIn(1, 4),
                        "status" to t.status,
                        "dueDate" to (t.dueDate ?: FieldValue.delete()),
                        "scheduledStartMs" to (t.scheduledStartMs ?: FieldValue.delete()),
                        "scheduledEndMs" to (t.scheduledEndMs ?: FieldValue.delete()),
                        "estimatedMinutes" to t.estimatedMinutes.coerceIn(0, 1440),
                        "updatedAtMs" to t.updatedAtMs,
                        "deletedAtMs" to (t.deletedAtMs ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun pushFocusSession(col: CollectionReference, f: FocusSessionEntity) {
        val docRef = col.document(f.id)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to f.userId,
                    "taskId" to f.taskId,
                    "habitId" to f.habitId,
                    "startedAtMs" to f.startedAtMs,
                    "endedAtMs" to f.endedAtMs,
                    "durationSeconds" to f.durationSeconds.coerceIn(0, 86400),
                    "sessionType" to f.sessionType,
                    "active" to f.isActive,
                    "updatedAtMs" to f.updatedAtMs,
                    "deletedAtMs" to f.deletedAtMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val remoteMs = snap.getLong("updatedAtMs") ?: 0L
                if (f.updatedAtMs >= remoteMs) {
                    val updates = mapOf<String, Any>(
                        "taskId" to (f.taskId ?: FieldValue.delete()),
                        "habitId" to (f.habitId ?: FieldValue.delete()),
                        "startedAtMs" to f.startedAtMs,
                        "endedAtMs" to (f.endedAtMs ?: FieldValue.delete()),
                        "durationSeconds" to f.durationSeconds.coerceIn(0, 86400),
                        "sessionType" to f.sessionType,
                        "active" to f.isActive,
                        "updatedAtMs" to f.updatedAtMs,
                        "deletedAtMs" to (f.deletedAtMs ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun pushDailyReview(col: CollectionReference, r: DailyReviewEntity) {
        val docRef = col.document(r.id)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to r.userId,
                    "reviewDate" to r.reviewDate,
                    "moodScore" to r.moodScore.coerceIn(1, 5),
                    "energyScore" to r.energyScore.coerceIn(1, 5),
                    "winsText" to r.winsText.take(2000),
                    "blockersText" to r.blockersText.take(2000),
                    "updatedAtMs" to r.updatedAtMs,
                    "deletedAtMs" to r.deletedAtMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val remoteMs = snap.getLong("updatedAtMs") ?: 0L
                if (r.updatedAtMs >= remoteMs) {
                    val updates = mapOf<String, Any>(
                        "reviewDate" to r.reviewDate,
                        "moodScore" to r.moodScore.coerceIn(1, 5),
                        "energyScore" to r.energyScore.coerceIn(1, 5),
                        "winsText" to r.winsText.take(2000),
                        "blockersText" to r.blockersText.take(2000),
                        "updatedAtMs" to r.updatedAtMs,
                        "deletedAtMs" to (r.deletedAtMs ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun pullAllDeltas(
        userId: String,
        userRoot: com.google.firebase.firestore.DocumentReference,
        lastPulledMs: Long
    ): Long {
        var maxServerTs = lastPulledMs
        val deltaCursor = if (lastPulledMs > 5000L) Timestamp((lastPulledMs - 5000L) / 1000L, 0) else null

        suspend fun queryCol(name: String): List<DocumentSnapshot> {
            val colRef = userRoot.collection(name)
            return try {
                val query = if (deltaCursor != null) {
                    colRef.whereGreaterThan("updatedAt", deltaCursor)
                } else {
                    colRef
                }
                query.get().await().documents
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, colRef.path)
                throw e
            }
        }

        fun trackServerTime(doc: DocumentSnapshot) {
            val ts = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (ts != null) {
                val ms = ts.toDate().time
                if (ms > maxServerTs) maxServerTs = ms
            }
        }

        // 1. Goals
        val goalDao = database.goalDao()
        for (doc in queryCol("goals")) {
            trackServerTime(doc)
            val remoteUpdatedMs = doc.getLong("updatedAtMs") ?: continue
            val local = goalDao.getById(doc.id)
            if (local == null || remoteUpdatedMs >= local.updatedAtMs) {
                val createdMs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: remoteUpdatedMs
                goalDao.upsert(
                    GoalEntity(
                        id = doc.id,
                        userId = userId,
                        title = doc.getString("title") ?: "",
                        category = doc.getString("category") ?: "Personal",
                        targetDate = doc.getString("targetDate"),
                        status = doc.getString("status") ?: "active",
                        createdAtMs = local?.createdAtMs ?: createdMs,
                        updatedAtMs = remoteUpdatedMs,
                        deletedAtMs = doc.getLong("deletedAtMs"),
                        dirty = false
                    )
                )
            }
        }

        // 2. Milestones
        val milestoneDao = database.milestoneDao()
        for (doc in queryCol("milestones")) {
            trackServerTime(doc)
            val remoteUpdatedMs = doc.getLong("updatedAtMs") ?: continue
            val local = milestoneDao.getById(doc.id)
            if (local == null || remoteUpdatedMs >= local.updatedAtMs) {
                val createdMs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: remoteUpdatedMs
                milestoneDao.upsert(
                    MilestoneEntity(
                        id = doc.id,
                        userId = userId,
                        goalId = doc.getString("goalId") ?: "",
                        title = doc.getString("title") ?: "",
                        targetValue = (doc.get("targetValue") as? Number)?.toDouble(),
                        currentValue = (doc.get("currentValue") as? Number)?.toDouble() ?: 0.0,
                        isCompleted = doc.getBoolean("completed") ?: false,
                        createdAtMs = local?.createdAtMs ?: createdMs,
                        updatedAtMs = remoteUpdatedMs,
                        deletedAtMs = doc.getLong("deletedAtMs"),
                        dirty = false
                    )
                )
            }
        }

        // 3. Habits
        val habitDao = database.habitDao()
        for (doc in queryCol("habits")) {
            trackServerTime(doc)
            val remoteUpdatedMs = doc.getLong("updatedAtMs") ?: continue
            val local = habitDao.getById(doc.id)
            if (local == null || remoteUpdatedMs >= local.updatedAtMs) {
                val createdMs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: remoteUpdatedMs
                @Suppress("UNCHECKED_CAST")
                val freqRule = doc.get("frequencyRule") as? Map<String, Any?>
                val freqType = (freqRule?.get("type") as? String) ?: "daily"
                val daysList = (freqRule?.get("days") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toInt() }
                    ?: listOf(1, 2, 3, 4, 5, 6, 7)
                habitDao.upsert(
                    HabitEntity(
                        id = doc.id,
                        userId = userId,
                        goalId = doc.getString("goalId"),
                        title = doc.getString("title") ?: "",
                        iconName = doc.getString("iconName") ?: "check_circle",
                        colorHex = doc.getString("colorHex") ?: "#2D5BFF",
                        metricType = doc.getString("metricType") ?: "boolean",
                        targetValue = (doc.get("targetValue") as? Number)?.toDouble() ?: 1.0,
                        unit = doc.getString("unit") ?: "",
                        frequencyType = freqType,
                        frequencyDaysCsv = daysList.joinToString(","),
                        createdAtMs = local?.createdAtMs ?: createdMs,
                        updatedAtMs = remoteUpdatedMs,
                        deletedAtMs = doc.getLong("deletedAtMs"),
                        dirty = false
                    )
                )
            }
        }

        // 4. HabitLogs (with MAX merge semantics for same-day offline conflicts)
        val habitLogDao = database.habitLogDao()
        for (doc in queryCol("habitLogs")) {
            trackServerTime(doc)
            val remoteUpdatedMs = doc.getLong("updatedAtMs") ?: continue
            val remoteVal = (doc.get("loggedValue") as? Number)?.toDouble() ?: 0.0
            val remoteDeleted = doc.getLong("deletedAtMs")
            val local = habitLogDao.getById(doc.id)
            if (local == null || remoteUpdatedMs >= local.updatedAtMs) {
                val createdMs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: remoteUpdatedMs
                val mergedVal = if (local != null && local.deletedAtMs == null && remoteDeleted == null) {
                    maxOf(local.loggedValue, remoteVal)
                } else {
                    remoteVal
                }
                habitLogDao.upsert(
                    HabitLogEntity(
                        id = doc.id,
                        userId = userId,
                        habitId = doc.getString("habitId") ?: "",
                        logDate = doc.getString("logDate") ?: "",
                        loggedValue = mergedVal,
                        note = doc.getString("note"),
                        createdAtMs = local?.createdAtMs ?: createdMs,
                        updatedAtMs = remoteUpdatedMs,
                        deletedAtMs = remoteDeleted,
                        dirty = false
                    )
                )
            }
        }

        // 5. Tasks
        val taskDao = database.taskDao()
        for (doc in queryCol("tasks")) {
            trackServerTime(doc)
            val remoteUpdatedMs = doc.getLong("updatedAtMs") ?: continue
            val local = taskDao.getById(doc.id)
            if (local == null || remoteUpdatedMs >= local.updatedAtMs) {
                val createdMs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: remoteUpdatedMs
                taskDao.upsert(
                    TaskEntity(
                        id = doc.id,
                        userId = userId,
                        goalId = doc.getString("goalId"),
                        parentTaskId = doc.getString("parentTaskId"),
                        title = doc.getString("title") ?: "",
                        priority = ((doc.get("priority") as? Number)?.toInt() ?: 2).coerceIn(1, 4),
                        status = doc.getString("status") ?: "todo",
                        dueDate = doc.getString("dueDate"),
                        scheduledStartMs = doc.getLong("scheduledStartMs"),
                        scheduledEndMs = doc.getLong("scheduledEndMs"),
                        estimatedMinutes = (doc.get("estimatedMinutes") as? Number)?.toInt() ?: 30,
                        createdAtMs = local?.createdAtMs ?: createdMs,
                        updatedAtMs = remoteUpdatedMs,
                        deletedAtMs = doc.getLong("deletedAtMs"),
                        dirty = false
                    )
                )
            }
        }

        // 6. FocusSessions
        val focusDao = database.focusSessionDao()
        for (doc in queryCol("focusSessions")) {
            trackServerTime(doc)
            val remoteUpdatedMs = doc.getLong("updatedAtMs") ?: continue
            val local = focusDao.getById(doc.id)
            if (local == null || remoteUpdatedMs >= local.updatedAtMs) {
                val createdMs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: remoteUpdatedMs
                focusDao.upsert(
                    FocusSessionEntity(
                        id = doc.id,
                        userId = userId,
                        taskId = doc.getString("taskId"),
                        habitId = doc.getString("habitId"),
                        startedAtMs = doc.getLong("startedAtMs") ?: remoteUpdatedMs,
                        endedAtMs = doc.getLong("endedAtMs"),
                        durationSeconds = (doc.get("durationSeconds") as? Number)?.toInt() ?: 0,
                        sessionType = doc.getString("sessionType") ?: "pomodoro",
                        isActive = doc.getBoolean("active") ?: false,
                        createdAtMs = local?.createdAtMs ?: createdMs,
                        updatedAtMs = remoteUpdatedMs,
                        deletedAtMs = doc.getLong("deletedAtMs"),
                        dirty = false
                    )
                )
            }
        }

        // 7. DailyReviews
        val reviewDao = database.dailyReviewDao()
        for (doc in queryCol("dailyReviews")) {
            trackServerTime(doc)
            val remoteUpdatedMs = doc.getLong("updatedAtMs") ?: continue
            val local = reviewDao.getById(doc.id)
            if (local == null || remoteUpdatedMs >= local.updatedAtMs) {
                val createdMs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: remoteUpdatedMs
                reviewDao.upsert(
                    DailyReviewEntity(
                        id = doc.id,
                        userId = userId,
                        reviewDate = doc.getString("reviewDate") ?: "",
                        moodScore = ((doc.get("moodScore") as? Number)?.toInt() ?: 3).coerceIn(1, 5),
                        energyScore = ((doc.get("energyScore") as? Number)?.toInt() ?: 3).coerceIn(1, 5),
                        winsText = doc.getString("winsText") ?: "",
                        blockersText = doc.getString("blockersText") ?: "",
                        createdAtMs = local?.createdAtMs ?: createdMs,
                        updatedAtMs = remoteUpdatedMs,
                        deletedAtMs = doc.getLong("deletedAtMs"),
                        dirty = false
                    )
                )
            }
        }

        return maxServerTs
    }

    private companion object {
        const val TAG = "DeltaSyncEngine"
    }
}
