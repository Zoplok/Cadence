package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ActivityMultiplier
import com.example.core.CalorieCalculator
import com.example.core.CalorieGoalMode
import com.example.core.CalorieResult
import com.example.core.ExerciseActivity
import com.example.data.local.CalorieProfile
import com.example.data.local.SyncPreferences
import com.example.data.repository.CadenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalorieUiState(
    val profile: CalorieProfile = CalorieProfile(),
    val result: CalorieResult = CalorieCalculator.calculate(
        isMale = true,
        age = 28,
        weightKg = 72f,
        heightCm = 175f,
        activity = ActivityMultiplier.MODERATE,
        goal = CalorieGoalMode.MAINTAIN
    ),
    val selectedExercise: ExerciseActivity = ExerciseActivity.RUNNING,
    val exerciseDurationMins: Int = 30,
    val estimatedBurnKcal: Int = 353
)

class CalorieViewModel(
    private val repository: CadenceRepository,
    private val prefs: SyncPreferences,
    private val currentUserId: String
) : ViewModel() {

    private val exerciseFlow = MutableStateFlow(ExerciseActivity.RUNNING)
    private val durationFlow = MutableStateFlow(30)

    val uiState: StateFlow<CalorieUiState> = combine(
        prefs.observeCalorieProfile(),
        exerciseFlow,
        durationFlow
    ) { profile, exercise, mins ->
        val calc = CalorieCalculator.calculate(
            isMale = profile.isMale,
            age = profile.age,
            weightKg = profile.weightKg,
            heightCm = profile.heightCm,
            activity = ActivityMultiplier.fromKey(profile.activityKey),
            goal = CalorieGoalMode.fromKey(profile.goalKey)
        )
        val burn = CalorieCalculator.estimateExerciseBurnKcal(exercise, profile.weightKg, mins)
        CalorieUiState(
            profile = profile,
            result = calc,
            selectedExercise = exercise,
            exerciseDurationMins = mins,
            estimatedBurnKcal = burn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = CalorieUiState()
    )

    fun updateProfile(transform: (CalorieProfile) -> CalorieProfile) {
        val next = transform(uiState.value.profile)
        viewModelScope.launch {
            prefs.saveCalorieProfile(next)
        }
    }

    fun selectExercise(activity: ExerciseActivity) {
        exerciseFlow.value = activity
    }

    fun setExerciseDuration(mins: Int) {
        durationFlow.value = mins.coerceIn(5, 240)
    }

    fun saveTargetAsHabit(habitTitle: String, targetKcal: Int) {
        viewModelScope.launch {
            repository.createHabit(
                userId = currentUserId,
                title = habitTitle,
                goalId = null,
                iconName = "fitness_center",
                colorHex = "#D95B38",
                metricType = "numeric",
                targetValue = targetKcal.toDouble(),
                unit = "kcal"
            )
        }
    }
}
