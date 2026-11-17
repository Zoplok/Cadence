package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.MilestoneEntity
import com.example.data.local.TaskEntity
import com.example.data.repository.CadenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalBundle(
    val goal: GoalEntity,
    val milestones: List<MilestoneEntity>,
    val linkedHabitsCount: Int,
    val linkedTasksCount: Int
) {
    val completionFraction: Float
        get() {
            if (milestones.isEmpty()) {
                return if (goal.status == "completed") 1f else 0f
            }
            val doneWeight = milestones.sumOf { m ->
                when {
                    m.isCompleted -> 1.0
                    m.targetValue != null && m.targetValue > 0.0 -> (m.currentValue / m.targetValue).coerceIn(0.0, 1.0)
                    else -> 0.0
                }
            }
            return (doneWeight / milestones.size).toFloat().coerceIn(0f, 1f)
        }
}

data class GoalsUiState(
    val goals: List<GoalBundle> = emptyList()
)

class GoalsViewModel(
    private val repository: CadenceRepository,
    private val currentUserId: String
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = combine(
        repository.observeGoals(currentUserId),
        repository.observeMilestones(currentUserId),
        repository.observeHabits(currentUserId),
        repository.observeTasks(currentUserId)
    ) { goals, milestones, habits, tasks ->
        val milestonesByGoal = milestones.groupBy { it.goalId }
        val habitsByGoal = habits.groupBy { it.goalId }
        val tasksByGoal = tasks.groupBy { it.goalId }
        GoalsUiState(
            goals = goals.map { goal ->
                GoalBundle(
                    goal = goal,
                    milestones = milestonesByGoal[goal.id].orEmpty(),
                    linkedHabitsCount = habitsByGoal[goal.id]?.size ?: 0,
                    linkedTasksCount = tasksByGoal[goal.id]?.size ?: 0
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = GoalsUiState()
    )

    fun createGoal(title: String, category: String, targetDate: String?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createGoal(currentUserId, title, category, targetDate)
        }
    }

    fun updateGoalStatus(goal: GoalEntity, status: String) {
        viewModelScope.launch {
            repository.updateGoalStatus(goal, status)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun createMilestone(goalId: String, title: String, targetValue: Double?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createMilestone(currentUserId, goalId, title, targetValue)
        }
    }

    fun toggleMilestone(milestone: MilestoneEntity) {
        viewModelScope.launch {
            repository.toggleMilestone(milestone)
        }
    }

    fun incrementMilestone(milestone: MilestoneEntity, delta: Double) {
        viewModelScope.launch {
            repository.incrementMilestone(milestone, delta)
        }
    }
}
