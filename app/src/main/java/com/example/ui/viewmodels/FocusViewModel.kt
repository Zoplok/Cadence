package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.TaskEntity
import com.example.data.repository.CadenceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusUiState(
    val activeSession: FocusSessionEntity? = null,
    val nowMs: Long = System.currentTimeMillis(),
    val recentSessions: List<FocusSessionEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList()
) {
    val elapsedSeconds: Int
        get() {
            val active = activeSession ?: return 0
            return ((nowMs - active.startedAtMs) / 1000L).toInt().coerceAtLeast(0)
        }

    val remainingSeconds: Int
        get() {
            val active = activeSession ?: return 0
            return (active.durationSeconds - elapsedSeconds).coerceAtLeast(0)
        }

    val isCountdownComplete: Boolean
        get() {
            val active = activeSession ?: return false
            return active.sessionType != "stopwatch" && elapsedSeconds >= active.durationSeconds
        }
}

class FocusViewModel(
    private val repository: CadenceRepository,
    private val currentUserId: String
) : ViewModel() {

    private val tickerFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val uiState: StateFlow<FocusUiState> = combine(
        repository.observeActiveFocusSession(currentUserId),
        tickerFlow,
        repository.observeFocusSessions(currentUserId),
        repository.observeTasks(currentUserId),
        repository.observeHabits(currentUserId)
    ) { active, nowMs, sessions, tasks, habits ->
        FocusUiState(
            activeSession = active,
            nowMs = nowMs,
            recentSessions = sessions.filter { !it.isActive },
            tasks = tasks.filter { it.status != "done" },
            habits = habits
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FocusUiState()
    )

    fun startSession(
        sessionType: String,
        targetMinutes: Int,
        taskId: String? = null,
        habitId: String? = null
    ) {
        val targetSeconds = if (sessionType == "stopwatch") 0 else targetMinutes.coerceIn(1, 180) * 60
        viewModelScope.launch {
            repository.startFocusSession(
                userId = currentUserId,
                sessionType = sessionType,
                targetDurationSeconds = targetSeconds,
                taskId = taskId,
                habitId = habitId
            )
        }
    }

    fun finishActiveSession() {
        val active = uiState.value.activeSession ?: return
        viewModelScope.launch {
            repository.finishFocusSession(active, markLinkedCompleted = true)
        }
    }
}
