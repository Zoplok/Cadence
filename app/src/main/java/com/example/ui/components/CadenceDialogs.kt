package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.DateUtils
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity

fun habitIconForName(name: String): ImageVector = when (name) {
    "water_drop" -> Icons.Rounded.WaterDrop
    "self_improvement" -> Icons.Rounded.SelfImprovement
    "code" -> Icons.Rounded.Code
    "fitness_center" -> Icons.Rounded.FitnessCenter
    "menu_book" -> Icons.Rounded.MenuBook
    "bedtime" -> Icons.Rounded.Bedtime
    else -> Icons.Rounded.CheckCircle
}

fun parseHexColor(hex: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { fallback }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateHabitSheet(
    goals: List<GoalEntity>,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        goalId: String?,
        iconName: String,
        colorHex: String,
        metricType: String,
        targetValue: Double,
        unit: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var metricType by remember { mutableStateOf("boolean") }
    var targetText by remember { mutableStateOf("2500") }
    var unitText by remember { mutableStateOf("ml") }
    var selectedIcon by remember { mutableStateOf("self_improvement") }
    var selectedColor by remember { mutableStateOf("#2D5BFF") }
    var selectedGoalId by remember { mutableStateOf<String?>(null) }

    val icons = listOf(
        "self_improvement", "water_drop", "code", "fitness_center", "menu_book", "bedtime", "check_circle"
    )
    val colors = listOf("#2D5BFF", "#178A5E", "#D95B38", "#7C3AED", "#0284C7", "#CA8A04")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(stringResource(R.string.new_daily_habit), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(100) },
                label = { Text(stringResource(R.string.habit_title_label)) },
                placeholder = { Text(stringResource(R.string.habit_title_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(stringResource(R.string.metric_type), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "boolean" to stringResource(R.string.metric_boolean),
                    "numeric" to stringResource(R.string.metric_numeric),
                    "duration" to stringResource(R.string.metric_duration)
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = metricType == key,
                        onClick = {
                            metricType = key
                            when (key) {
                                "boolean" -> { targetText = "1"; unitText = "session" }
                                "numeric" -> { targetText = "2500"; unitText = "ml" }
                                "duration" -> { targetText = "60"; unitText = "mins" }
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (metricType != "boolean") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.daily_target)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it.take(30) },
                        label = { Text(stringResource(R.string.unit_label)) },
                        placeholder = { Text("ml, mins, kcal") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(stringResource(R.string.icon_accent), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                icons.forEach { iconKey ->
                    val isSelected = selectedIcon == iconKey
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedIcon = iconKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = habitIconForName(iconKey),
                            contentDescription = iconKey,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colors.forEach { hex ->
                    val c = parseHexColor(hex, MaterialTheme.colorScheme.primary)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (selectedColor == hex) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            if (goals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(stringResource(R.string.link_goal_optional), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedGoalId == null,
                        onClick = { selectedGoalId = null },
                        label = { Text(stringResource(R.string.none_label)) }
                    )
                    goals.filter { it.status == "active" }.forEach { goal ->
                        FilterChip(
                            selected = selectedGoalId == goal.id,
                            onClick = { selectedGoalId = goal.id },
                            label = { Text(goal.title.take(24)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val target = if (metricType == "boolean") 1.0 else (targetText.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)
                    onCreate(title, selectedGoalId, selectedIcon, selectedColor, metricType, target, unitText)
                    onDismiss()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_habit_button")
            ) {
                Text(stringResource(R.string.create_habit))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskSheet(
    goals: List<GoalEntity>,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        priority: Int,
        goalId: String?,
        estimatedMinutes: Int,
        scheduledStartMs: Long?,
        scheduledEndMs: Long?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(2) }
    var estMinutes by remember { mutableIntStateOf(45) }
    var includeTimeBlock by remember { mutableStateOf(true) }
    var startHour by remember { mutableIntStateOf(10) }
    var selectedGoalId by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(stringResource(R.string.new_task_timeblock), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(160) },
                label = { Text(stringResource(R.string.task_title_label)) },
                placeholder = { Text(stringResource(R.string.task_title_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(stringResource(R.string.priority_label), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    1 to stringResource(R.string.priority_low),
                    2 to stringResource(R.string.priority_medium),
                    3 to stringResource(R.string.priority_high),
                    4 to stringResource(R.string.priority_urgent)
                ).forEach { (p, label) ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.estimated_duration, estMinutes), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = estMinutes.toFloat(),
                onValueChange = { estMinutes = (it / 15f).toInt().coerceIn(1, 16) * 15 },
                valueRange = 15f..240f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.schedule_timeline), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (includeTimeBlock) {
                            stringResource(R.string.starts_at_hour, "%02d:00".format(startHour))
                        } else {
                            stringResource(R.string.unscheduled_backlog)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = includeTimeBlock, onCheckedChange = { includeTimeBlock = it })
            }

            if (includeTimeBlock) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (6..21).forEach { hour ->
                        FilterChip(
                            selected = startHour == hour,
                            onClick = { startHour = hour },
                            label = { Text("%02d:00".format(hour)) }
                        )
                    }
                }
            }

            if (goals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.link_goal_optional), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedGoalId == null,
                        onClick = { selectedGoalId = null },
                        label = { Text(stringResource(R.string.none_label)) }
                    )
                    goals.filter { it.status == "active" }.forEach { goal ->
                        FilterChip(
                            selected = selectedGoalId == goal.id,
                            onClick = { selectedGoalId = goal.id },
                            label = { Text(goal.title.take(24)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val startMs = if (includeTimeBlock) DateUtils.todayAtHourMinute(startHour, 0) else null
                    val endMs = startMs?.plus(estMinutes * 60_000L)
                    onCreate(title, priority, selectedGoalId, estMinutes, startMs, endMs)
                    onDismiss()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button")
            ) {
                Text(stringResource(R.string.add_task))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LogHabitValueDialog(
    habit: HabitEntity,
    currentValue: Double,
    currentNote: String?,
    onDismiss: () -> Unit,
    onSave: (value: Double, note: String?) -> Unit
) {
    var value by remember { mutableDoubleStateOf(currentValue) }
    var note by remember { mutableStateOf(currentNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(habit.title) },
        text = {
            Column {
                Text(
                    text = "${value.toInt()} / ${habit.targetValue.toInt()} ${habit.unit}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = value.toFloat(),
                    onValueChange = { value = it.toDouble() },
                    valueRange = 0f..(habit.targetValue * 1.5).toFloat().coerceAtLeast(10f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = { Text(stringResource(R.string.note_optional)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(value, note)
                onDismiss()
            }) {
                Text(stringResource(R.string.save_log))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
