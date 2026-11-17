package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.ActivityMultiplier
import com.example.core.CalorieGoalMode
import com.example.core.ExerciseActivity
import com.example.ui.viewmodels.CalorieViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalorieCalculatorScreen(viewModel: CalorieViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val profile = state.profile
    val result = state.result

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.kcal_kicker),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = stringResource(R.string.kcal_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = stringResource(R.string.kcal_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Hero Result Card
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.daily_kcal_target),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${result.targetKcal} kcal",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.testTag("kcal_target_value")
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = stringResource(R.string.bmr_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${result.bmrKcal} kcal",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = stringResource(R.string.tdee_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${result.tdeeKcal} kcal",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.macros_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val totalMacroKcal = (result.proteinGrams * 4 + result.carbsGrams * 4 + result.fatGrams * 9)
                                .coerceAtLeast(1)
                            MacroBar(
                                label = stringResource(R.string.macro_protein),
                                grams = result.proteinGrams,
                                kcal = result.proteinGrams * 4,
                                fraction = (result.proteinGrams * 4f) / totalMacroKcal,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MacroBar(
                                label = stringResource(R.string.macro_carbs),
                                grams = result.carbsGrams,
                                kcal = result.carbsGrams * 4,
                                fraction = (result.carbsGrams * 4f) / totalMacroKcal,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MacroBar(
                                label = stringResource(R.string.macro_fat),
                                grams = result.fatGrams,
                                kcal = result.fatGrams * 9,
                                fraction = (result.fatGrams * 9f) / totalMacroKcal,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            val savedMsg = stringResource(R.string.kcal_habit_saved, result.targetKcal)
                            val habitTitle = "${stringResource(R.string.daily_kcal_target)} (${result.targetKcal} kcal)"
                            Button(
                                onClick = {
                                    viewModel.saveTargetAsHabit(habitTitle, result.targetKcal)
                                    scope.launch { snackbarHostState.showSnackbar(savedMsg) }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_kcal_habit_button")
                            ) {
                                Icon(Icons.Rounded.AddTask, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.save_kcal_habit))
                            }
                        }
                    }
                }

                // Body Metrics & Goal Controls
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.sex_label), style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = profile.isMale,
                                    onClick = { viewModel.updateProfile { it.copy(isMale = true) } },
                                    label = { Text(stringResource(R.string.sex_male)) }
                                )
                                FilterChip(
                                    selected = !profile.isMale,
                                    onClick = { viewModel.updateProfile { it.copy(isMale = false) } },
                                    label = { Text(stringResource(R.string.sex_female)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${stringResource(R.string.age_label)}: ${profile.age}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Slider(
                                value = profile.age.toFloat(),
                                onValueChange = { v -> viewModel.updateProfile { it.copy(age = v.toInt()) } },
                                valueRange = 14f..85f
                            )

                            Text(
                                text = "${stringResource(R.string.weight_kg_label)}: ${profile.weightKg.toInt()} kg",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Slider(
                                value = profile.weightKg,
                                onValueChange = { v -> viewModel.updateProfile { it.copy(weightKg = v.toInt().toFloat()) } },
                                valueRange = 35f..160f
                            )

                            Text(
                                text = "${stringResource(R.string.height_cm_label)}: ${profile.heightCm.toInt()} cm",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Slider(
                                value = profile.heightCm,
                                onValueChange = { v -> viewModel.updateProfile { it.copy(heightCm = v.toInt().toFloat()) } },
                                valueRange = 130f..220f
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.activity_level_label), style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            val activityLabels = mapOf(
                                ActivityMultiplier.SEDENTARY to stringResource(R.string.activity_sedentary),
                                ActivityMultiplier.LIGHT to stringResource(R.string.activity_light),
                                ActivityMultiplier.MODERATE to stringResource(R.string.activity_moderate),
                                ActivityMultiplier.ACTIVE to stringResource(R.string.activity_active),
                                ActivityMultiplier.ATHLETE to stringResource(R.string.activity_athlete)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActivityMultiplier.entries.forEach { mult ->
                                    FilterChip(
                                        selected = profile.activityKey == mult.key,
                                        onClick = { viewModel.updateProfile { it.copy(activityKey = mult.key) } },
                                        label = { Text(activityLabels[mult] ?: mult.key) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.goal_mode_label), style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            val goalLabels = mapOf(
                                CalorieGoalMode.CUT to stringResource(R.string.goal_cut),
                                CalorieGoalMode.MAINTAIN to stringResource(R.string.goal_maintain),
                                CalorieGoalMode.GAIN to stringResource(R.string.goal_gain)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CalorieGoalMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = profile.goalKey == mode.key,
                                        onClick = { viewModel.updateProfile { it.copy(goalKey = mode.key) } },
                                        label = { Text(goalLabels[mode] ?: mode.key) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Activity Burn Estimator
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.DirectionsRun,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.burn_estimator_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val exLabels = mapOf(
                                ExerciseActivity.RUNNING to stringResource(R.string.burn_running),
                                ExerciseActivity.WALKING to stringResource(R.string.burn_walking),
                                ExerciseActivity.CYCLING to stringResource(R.string.burn_cycling),
                                ExerciseActivity.STRENGTH to stringResource(R.string.burn_strength),
                                ExerciseActivity.SWIMMING to stringResource(R.string.burn_swimming)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExerciseActivity.entries.forEach { ex ->
                                    FilterChip(
                                        selected = state.selectedExercise == ex,
                                        onClick = { viewModel.selectExercise(ex) },
                                        label = { Text(exLabels[ex] ?: ex.key) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.burn_duration_mins, state.exerciseDurationMins),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Slider(
                                value = state.exerciseDurationMins.toFloat(),
                                onValueChange = { viewModel.setExerciseDuration((it / 5f).toInt().coerceAtLeast(1) * 5) },
                                valueRange = 5f..180f
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.estimated_burn_value, state.estimatedBurnKcal),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MacroBar(
    label: String,
    grams: Int,
    kcal: Int,
    fraction: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${grams}g · $kcal kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
