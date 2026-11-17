package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.DateUtils
import com.example.ui.viewmodels.FocusViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    preselectedTaskId: String? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf("pomodoro") }
    var customMinutes by remember { mutableIntStateOf(25) }
    var linkedTaskId by remember(preselectedTaskId) { mutableStateOf(preselectedTaskId) }
    var linkedHabitId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedType) {
        customMinutes = when (selectedType) {
            "pomodoro" -> 25
            "deep_work" -> 50
            else -> 0
        }
    }

    val active = state.activeSession
    val dialProgress = if (active == null) {
        0f
    } else if (active.sessionType == "stopwatch" || active.durationSeconds <= 0) {
        ((state.elapsedSeconds % 60) / 60f)
    } else {
        (state.elapsedSeconds.toFloat() / active.durationSeconds.toFloat()).coerceIn(0f, 1f)
    }

    val displaySeconds = when {
        active == null && selectedType == "stopwatch" -> 0
        active == null -> customMinutes * 60
        active.sessionType == "stopwatch" -> state.elapsedSeconds
        else -> state.remainingSeconds
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (state.isCountdownComplete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.fillMaxSize(),
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
                        text = stringResource(R.string.focus_kicker),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.focus_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.focus_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (active == null) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "pomodoro" to stringResource(R.string.mode_pomodoro),
                            "deep_work" to stringResource(R.string.mode_deep_work),
                            "stopwatch" to stringResource(R.string.mode_stopwatch)
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedType == key,
                                onClick = { selectedType = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Timer Dial Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 14.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                                    topLeft = Offset(stroke / 2, stroke / 2),
                                    size = Size(size.width - stroke, size.height - stroke)
                                )
                                drawArc(
                                    color = activeColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * dialProgress,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                                    topLeft = Offset(stroke / 2, stroke / 2),
                                    size = Size(size.width - stroke, size.height - stroke)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = DateUtils.formatDurationSeconds(displaySeconds),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when {
                                        active == null -> stringResource(R.string.status_ready)
                                        state.isCountdownComplete -> stringResource(R.string.status_target_reached)
                                        else -> active.sessionType.uppercase().replace('_', ' ')
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (active == null) {
                            Button(
                                onClick = {
                                    viewModel.startSession(
                                        sessionType = selectedType,
                                        targetMinutes = customMinutes,
                                        taskId = linkedTaskId,
                                        habitId = linkedHabitId
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("start_focus_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.start_focus_session))
                            }
                        } else {
                            Button(
                                onClick = { viewModel.finishActiveSession() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("finish_focus_button")
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(
                                        R.string.complete_and_log,
                                        state.elapsedSeconds / 60,
                                        state.elapsedSeconds % 60
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (active == null && (state.tasks.isNotEmpty() || state.habits.isNotEmpty())) {
                item {
                    Column {
                        Text(stringResource(R.string.link_session_optional), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.tasks.take(5).forEach { task ->
                                FilterChip(
                                    selected = linkedTaskId == task.id,
                                    onClick = {
                                        linkedTaskId = if (linkedTaskId == task.id) null else task.id
                                        if (linkedTaskId != null) linkedHabitId = null
                                    },
                                    label = { Text(task.title.take(22)) }
                                )
                            }
                            state.habits.filter { it.metricType == "duration" }.forEach { habit ->
                                FilterChip(
                                    selected = linkedHabitId == habit.id,
                                    onClick = {
                                        linkedHabitId = if (linkedHabitId == habit.id) null else habit.id
                                        if (linkedHabitId != null) linkedTaskId = null
                                    },
                                    label = { Text(habit.title.take(22)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.recent_sessions), style = MaterialTheme.typography.titleLarge)
            }

            if (state.recentSessions.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.recent_sessions_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.recentSessions.take(15), key = { it.id }) { session ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val sessionTitle = when (session.sessionType) {
                                    "pomodoro" -> stringResource(R.string.mode_pomodoro)
                                    "deep_work" -> stringResource(R.string.mode_deep_work)
                                    "stopwatch" -> stringResource(R.string.mode_stopwatch)
                                    else -> session.sessionType.replace('_', ' ').replaceFirstChar { it.uppercase() }
                                }
                                Text(
                                    text = sessionTitle,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(
                                        R.string.started_at_time,
                                        DateUtils.formatTimeMs(session.startedAtMs)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = DateUtils.formatDurationSeconds(session.durationSeconds),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
