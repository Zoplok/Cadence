package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DateUtils
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.local.SyncMetadata
import com.example.data.local.TaskEntity
import com.example.data.repository.CadenceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitWithProgress(
    val habit: HabitEntity,
    val log: HabitLogEntity?,
    val streakDays: Int
) {
    val loggedValue: Double get() = log?.loggedValue ?: 0.0
    val isCompleted: Boolean get() = loggedValue >= habit.targetValue
    val progressFraction: Float
        get() = if (habit.targetValue <= 0.0) 0f else (loggedValue / habit.targetValue).toFloat().coerceIn(0f, 1f)
}

data class TodayUiState(
    val selectedDate: String = DateUtils.todayIso(),
    val habits: List<HabitWithProgress> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val syncMetadata: SyncMetadata = SyncMetadata(),
    val pendingChangesCount: Int = 0,
    val isManualSyncing: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: CadenceRepository,
    private val currentUserId: String
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(DateUtils.todayIso())
    private val manualSyncingFlow = MutableStateFlow(false)

    private val logsForSelectedDate = selectedDateFlow.flatMapLatest { date ->
        repository.observeHabitLogsForDate(currentUserId, date)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        selectedDateFlow,
        repository.observeHabits(currentUserId),
        logsForSelectedDate,
        repository.observeHabitStreaks(currentUserId),
        repository.observeTasks(currentUserId),
        repository.observeGoals(currentUserId),
        repository.observeSyncMetadata(currentUserId),
        repository.observePendingDirtyCount(currentUserId),
        manualSyncingFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val date = values[0] as String
        @Suppress("UNCHECKED_CAST")
        val habits = values[1] as List<HabitEntity>
        @Suppress("UNCHECKED_CAST")
        val logs = values[2] as List<HabitLogEntity>
        @Suppress("UNCHECKED_CAST")
        val streaks = values[3] as Map<String, Int>
        @Suppress("UNCHECKED_CAST")
        val tasks = values[4] as List<TaskEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = values[5] as List<GoalEntity>
        val syncMeta = values[6] as SyncMetadata
        val dirtyCount = values[7] as Int
        val manualSyncing = values[8] as Boolean

        val logsByHabit = logs.associateBy { it.habitId }
        val habitItems = habits.map { habit ->
            HabitWithProgress(
                habit = habit,
                log = logsByHabit[habit.id],
                streakDays = streaks[habit.id] ?: 0
            )
        }
        TodayUiState(
            selectedDate = date,
            habits = habitItems,
            tasks = tasks,
            goals = goals,
            syncMetadata = syncMeta,
            pendingChangesCount = dirtyCount,
            isManualSyncing = manualSyncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TodayUiState()
    )

    init {
        triggerSync()
    }

    fun shiftDate(deltaDays: Int) {
        selectedDateFlow.value = DateUtils.shiftIsoDate(selectedDateFlow.value, deltaDays)
    }

    fun jumpToToday() {
        selectedDateFlow.value = DateUtils.todayIso()
    }

    fun triggerSync() {
        if (manualSyncingFlow.value) return
        viewModelScope.launch {
            manualSyncingFlow.value = true
            try {
                repository.syncNow(currentUserId)
            } finally {
                manualSyncingFlow.value = false
            }
        }
    }

    fun toggleOrStepHabit(habit: HabitEntity) {
        val date = selectedDateFlow.value
        viewModelScope.launch {
            repository.toggleOrStepHabit(currentUserId, habit, date)
        }
    }

    fun setHabitLoggedValue(habit: HabitEntity, value: Double, note: String? = null) {
        val date = selectedDateFlow.value
        viewModelScope.launch {
            repository.upsertHabitLog(currentUserId, habit.id, date, value, note)
        }
    }

    fun createHabit(
        title: String,
        goalId: String?,
        iconName: String,
        colorHex: String,
        metricType: String,
        targetValue: Double,
        unit: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createHabit(
                userId = currentUserId,
                title = title,
                goalId = goalId,
                iconName = iconName,
                colorHex = colorHex,
                metricType = metricType,
                targetValue = targetValue,
                unit = unit
            )
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun createTask(
        title: String,
        priority: Int,
        goalId: String?,
        parentTaskId: String?,
        estimatedMinutes: Int,
        scheduledStartMs: Long?,
        scheduledEndMs: Long?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createTask(
                userId = currentUserId,
                title = title,
                priority = priority,
                goalId = goalId,
                parentTaskId = parentTaskId,
                dueDate = selectedDateFlow.value,
                scheduledStartMs = scheduledStartMs,
                scheduledEndMs = scheduledEndMs,
                estimatedMinutes = estimatedMinutes
            )
        }
    }

    fun toggleTaskDone(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskDone(task)
        }
    }

    fun cycleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            repository.cycleTaskStatus(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun seedStarterRoutine() {
        viewModelScope.launch {
            repository.seedStarterRoutine(currentUserId)
        }
    }
}
