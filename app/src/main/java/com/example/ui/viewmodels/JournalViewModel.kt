package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DateUtils
import com.example.data.local.DailyReviewEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
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

data class HeatmapDay(
    val isoDate: String,
    val completionRatio: Float,
    val completedCount: Int,
    val totalHabits: Int
)

data class JournalUiState(
    val selectedDate: String = DateUtils.todayIso(),
    val currentReview: DailyReviewEntity? = null,
    val recentReviews: List<DailyReviewEntity> = emptyList(),
    val heatmapDays: List<HeatmapDay> = emptyList(),
    val weeklyFocusMinutes: Int = 0,
    val tasksCompletedCount: Int = 0,
    val averageMood: Float = 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val repository: CadenceRepository,
    private val currentUserId: String
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(DateUtils.todayIso())
    private val heatmapStartDate = DateUtils.isoDaysAgo(27) // 28 days (4x7 grid)

    private val currentReviewFlow = selectedDateFlow.flatMapLatest { date ->
        repository.observeDailyReviewForDate(currentUserId, date)
    }

    val uiState: StateFlow<JournalUiState> = combine(
        selectedDateFlow,
        currentReviewFlow,
        repository.observeDailyReviews(currentUserId),
        repository.observeHabits(currentUserId),
        repository.observeHabitLogsSince(currentUserId, heatmapStartDate),
        repository.observeFocusSessions(currentUserId),
        repository.observeTasks(currentUserId)
    ) { values ->
        val date = values[0] as String
        val currentReview = values[1] as DailyReviewEntity?
        @Suppress("UNCHECKED_CAST")
        val reviews = values[2] as List<DailyReviewEntity>
        @Suppress("UNCHECKED_CAST")
        val habits = values[3] as List<HabitEntity>
        @Suppress("UNCHECKED_CAST")
        val logs = values[4] as List<HabitLogEntity>
        @Suppress("UNCHECKED_CAST")
        val sessions = values[5] as List<FocusSessionEntity>
        @Suppress("UNCHECKED_CAST")
        val tasks = values[6] as List<TaskEntity>

        val habitsById = habits.associateBy { it.id }
        val logsByDate = logs.groupBy { it.logDate }
        val totalHabits = habits.size

        val heatmap = (27 downTo 0).map { daysAgo ->
            val iso = DateUtils.isoDaysAgo(daysAgo)
            val dayLogs = logsByDate[iso].orEmpty()
            val completed = dayLogs.count { log ->
                val target = habitsById[log.habitId]?.targetValue ?: 1.0
                log.loggedValue >= target
            }
            val ratio = if (totalHabits == 0) 0f else (completed.toFloat() / totalHabits).coerceIn(0f, 1f)
            HeatmapDay(
                isoDate = iso,
                completionRatio = ratio,
                completedCount = completed,
                totalHabits = totalHabits
            )
        }

        val sevenDaysAgoMs = System.currentTimeMillis() - 7L * 24L * 3600L * 1000L
        val focusMins = sessions
            .filter { !it.isActive && it.startedAtMs >= sevenDaysAgoMs }
            .sumOf { it.durationSeconds } / 60

        val doneTasks = tasks.count { it.status == "done" }
        val avgMood = if (reviews.isEmpty()) 0f else reviews.take(7).map { it.moodScore }.average().toFloat()

        JournalUiState(
            selectedDate = date,
            currentReview = currentReview,
            recentReviews = reviews,
            heatmapDays = heatmap,
            weeklyFocusMinutes = focusMins,
            tasksCompletedCount = doneTasks,
            averageMood = avgMood
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = JournalUiState()
    )

    fun shiftDate(deltaDays: Int) {
        selectedDateFlow.value = DateUtils.shiftIsoDate(selectedDateFlow.value, deltaDays)
    }

    fun saveReview(moodScore: Int, energyScore: Int, wins: String, blockers: String) {
        val date = selectedDateFlow.value
        viewModelScope.launch {
            repository.saveDailyReview(
                userId = currentUserId,
                reviewDate = date,
                moodScore = moodScore,
                energyScore = energyScore,
                winsText = wins,
                blockersText = blockers
            )
        }
    }
}
