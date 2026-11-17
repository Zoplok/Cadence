package com.example.core

import kotlin.math.roundToInt

enum class ActivityMultiplier(val key: String, val factor: Double) {
    SEDENTARY("sedentary", 1.2),
    LIGHT("light", 1.375),
    MODERATE("moderate", 1.55),
    ACTIVE("active", 1.725),
    ATHLETE("athlete", 1.9);

    companion object {
        fun fromKey(key: String): ActivityMultiplier =
            entries.firstOrNull { it.key == key } ?: MODERATE
    }
}

enum class CalorieGoalMode(val key: String, val deltaKcal: Int) {
    CUT("cut", -500),
    MAINTAIN("maintain", 0),
    GAIN("gain", 300);

    companion object {
        fun fromKey(key: String): CalorieGoalMode =
            entries.firstOrNull { it.key == key } ?: MAINTAIN
    }
}

enum class ExerciseActivity(val key: String, val met: Double) {
    RUNNING("running", 9.8),
    WALKING("walking", 3.8),
    CYCLING("cycling", 7.5),
    STRENGTH("strength", 6.0),
    SWIMMING("swimming", 8.0)
}

data class CalorieResult(
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val targetKcal: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int
)

object CalorieCalculator {
    /**
     * Mifflin-St Jeor equation:
     * Male:   10 * weight(kg) + 6.25 * height(cm) - 5 * age + 5
     * Female: 10 * weight(kg) + 6.25 * height(cm) - 5 * age - 161
     */
    fun calculate(
        isMale: Boolean,
        age: Int,
        weightKg: Float,
        heightCm: Float,
        activity: ActivityMultiplier,
        goal: CalorieGoalMode
    ): CalorieResult {
        val safeAge = age.coerceIn(10, 110)
        val safeWeight = weightKg.coerceIn(25f, 300f)
        val safeHeight = heightCm.coerceIn(100f, 240f)
        val sexOffset = if (isMale) 5.0 else -161.0

        val bmr = (10.0 * safeWeight + 6.25 * safeHeight - 5.0 * safeAge + sexOffset)
            .roundToInt()
            .coerceAtLeast(800)
        val tdee = (bmr * activity.factor).roundToInt()
        val target = (tdee + goal.deltaKcal).coerceAtLeast(1100)

        // Balanced athletic macro split: ~30% protein (min 1.6g/kg capped), 25% fat, remainder carbs
        val proteinG = (safeWeight * 1.8).roundToInt().coerceIn(50, (target * 0.40 / 4.0).roundToInt())
        val fatG = ((target * 0.25) / 9.0).roundToInt().coerceAtLeast(30)
        val remainingKcal = (target - (proteinG * 4) - (fatG * 9)).coerceAtLeast(80)
        val carbsG = (remainingKcal / 4.0).roundToInt()

        return CalorieResult(
            bmrKcal = bmr,
            tdeeKcal = tdee,
            targetKcal = target,
            proteinGrams = proteinG,
            carbsGrams = carbsG,
            fatGrams = fatG
        )
    }

    /**
     * MET formula: kcal = MET * weight(kg) * duration(hours)
     */
    fun estimateExerciseBurnKcal(
        activity: ExerciseActivity,
        weightKg: Float,
        durationMinutes: Int
    ): Int {
        val safeWeight = weightKg.coerceIn(25f, 300f)
        val safeMins = durationMinutes.coerceIn(1, 480)
        return (activity.met * safeWeight * (safeMins / 60.0)).roundToInt()
    }
}
