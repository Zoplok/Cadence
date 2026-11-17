package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.DateUtils
import com.example.data.local.TaskEntity
import com.example.ui.components.CreateHabitSheet
import com.example.ui.components.CreateTaskSheet
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.LogHabitValueDialog
import com.example.ui.components.habitIconForName
import com.example.ui.components.parseHexColor
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityUrgentColor
import com.example.ui.viewmodels.HabitWithProgress
import com.example.ui.viewmodels.TodayViewModel

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    userEmail: String?,
    currentLanguageCode: String,
    onToggleLanguage: () -> Unit,
    onStartFocusForTask: (TaskEntity) -> Unit,
    onSignOutClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateMenu by remember { mutableStateOf(false) }
    var showCreateHabit by remember { mutableStateOf(false) }
    var showCreateTask by remember { mutableStateOf(false) }
    var editingHabitLog by remember { mutableStateOf<HabitWithProgress?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showAgendaMode by remember { mutableStateOf(true) }
    var subtaskParentTask by remember { mutableStateOf<TaskEntity?>(null) }

    val completedHabits = state.habits.count { it.isCompleted }
    val totalHabits = state.habits.size
    val topTasks = state.tasks.filter { it.parentTaskId == null }
    val subtasksByParent = state.tasks.filter { it.parentTaskId != null }.groupBy { it.parentTaskId }
    val scheduledTasks = state.tasks
        .filter { it.scheduledStartMs != null }
        .sortedBy { it.scheduledStartMs }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("today_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add habit or task")
            }
        }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Sync Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (state.selectedDate == DateUtils.todayIso()) {
                                    "${stringResource(R.string.today_label)} · ${state.selectedDate}"
                                } else {
                                    DateUtils.prettyDate(state.selectedDate)
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LanguageToggleChip(
                                currentCode = currentLanguageCode,
                                onToggle = onToggleLanguage
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            SyncStatusChip(
                                isSyncing = state.isManualSyncing || state.syncMetadata.lastSyncState == "syncing",
                                pendingCount = state.pendingChangesCount,
                                hasError = state.syncMetadata.lastSyncState == "error",
                                onClick = { viewModel.triggerSync() }
                            )
                            IconButton(
                                onClick = { showAccountDialog = true },
                                modifier = Modifier.testTag("account_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = stringResource(R.string.sign_out),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Date Navigator + Daily Progress Summary Card
                item {
                    DailySummaryCard(
                        selectedDate = state.selectedDate,
                        completedHabits = completedHabits,
                        totalHabits = totalHabits,
                        doneTasks = topTasks.count { it.status == "done" },
                        totalTasks = topTasks.size,
                        onPrevDay = { viewModel.shiftDate(-1) },
                        onNextDay = { viewModel.shiftDate(1) },
                        onJumpToday = { viewModel.jumpToToday() }
                    )
                }

                // Section: Daily Habits
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.daily_habits),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = { showCreateHabit = true }) {
                            Text(stringResource(R.string.add_habit_short))
                        }
                    }
                }

                if (state.habits.isEmpty()) {
                    item {
                        EmptyTodayCard(
                            onAddHabit = { showCreateHabit = true },
                            onSeedStarter = { viewModel.seedStarterRoutine() }
                        )
                    }
                } else {
                    items(state.habits, key = { it.habit.id }) { item ->
                        HabitCard(
                            item = item,
                            onQuickToggle = { viewModel.toggleOrStepHabit(item.habit) },
                            onOpenDetail = { editingHabitLog = item },
                            onDelete = { viewModel.deleteHabit(item.habit) }
                        )
                    }
                }

                // Section: Tasks & Time-Blocking
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.execution_layer),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = showAgendaMode,
                                onClick = { showAgendaMode = true },
                                label = { Text(stringResource(R.string.timeline_tab)) }
                            )
                            FilterChip(
                                selected = !showAgendaMode,
                                onClick = { showAgendaMode = false },
                                label = { Text(stringResource(R.string.tasks_tab)) }
                            )
                        }
                    }
                }

                val displayedTasks = if (showAgendaMode) scheduledTasks else topTasks
                if (displayedTasks.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (showAgendaMode) {
                                            stringResource(R.string.empty_timeline)
                                        } else {
                                            stringResource(R.string.empty_tasks)
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.empty_tasks_sub),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(onClick = { showCreateTask = true }) {
                                    Text(stringResource(R.string.add_task))
                                }
                            }
                        }
                    }
                } else {
                    items(displayedTasks, key = { it.id }) { task ->
                        TaskExecutionCard(
                            task = task,
                            subtasks = subtasksByParent[task.id].orEmpty(),
                            showTimelineSlot = showAgendaMode,
                            onToggleDone = { viewModel.toggleTaskDone(task) },
                            onToggleSubtask = { sub -> viewModel.toggleTaskDone(sub) },
                            onAddSubtask = { subtaskParentTask = task },
                            onFocusTask = { onStartFocusForTask(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showCreateMenu) {
        AlertDialog(
            onDismissRequest = { showCreateMenu = false },
            title = { Text(stringResource(R.string.create_in_cadence)) },
            text = { Text(stringResource(R.string.create_in_cadence_sub)) },
            confirmButton = {
                Button(onClick = {
                    showCreateMenu = false
                    showCreateHabit = true
                }) { Text(stringResource(R.string.new_habit)) }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showCreateMenu = false
                    showCreateTask = true
                }) { Text(stringResource(R.string.new_task)) }
            }
        )
    }

    if (showCreateHabit) {
        CreateHabitSheet(
            goals = state.goals,
            onDismiss = { showCreateHabit = false },
            onCreate = { title, goalId, icon, color, metric, target, unit ->
                viewModel.createHabit(title, goalId, icon, color, metric, target, unit)
            }
        )
    }

    if (showCreateTask) {
        CreateTaskSheet(
            goals = state.goals,
            onDismiss = { showCreateTask = false },
            onCreate = { title, priority, goalId, estMins, startMs, endMs ->
                viewModel.createTask(title, priority, goalId, null, estMins, startMs, endMs)
            }
        )
    }

    editingHabitLog?.let { item ->
        LogHabitValueDialog(
            habit = item.habit,
            currentValue = item.loggedValue,
            currentNote = item.log?.note,
            onDismiss = { editingHabitLog = null },
            onSave = { value, note ->
                viewModel.setHabitLoggedValue(item.habit, value, note)
            }
        )
    }

    subtaskParentTask?.let { parent ->
        var subTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { subtaskParentTask = null },
            title = { Text(stringResource(R.string.add_subtask)) },
            text = {
                OutlinedTextField(
                    value = subTitle,
                    onValueChange = { subTitle = it.take(160) },
                    label = { Text(parent.title.take(28)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = subTitle.isNotBlank(),
                    onClick = {
                        viewModel.createTask(subTitle, parent.priority, parent.goalId, parent.id, 15, null, null)
                        subtaskParentTask = null
                    }
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { subtaskParentTask = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text(stringResource(R.string.synced_account)) },
            text = {
                Column {
                    Text(userEmail ?: "Google Account", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.pending_local_changes,
                            state.pendingChangesCount,
                            state.syncMetadata.lastSyncState
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showAccountDialog = false
                        onSignOutClick()
                    }
                ) { Text(stringResource(R.string.sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
private fun SyncStatusChip(
    isSyncing: Boolean,
    pendingCount: Int,
    hasError: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        hasError -> MaterialTheme.colorScheme.errorContainer
        pendingCount > 0 || isSyncing -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val fg = when {
        hasError -> MaterialTheme.colorScheme.onErrorContainer
        pendingCount > 0 || isSyncing -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val icon = when {
        hasError -> Icons.Rounded.CloudOff
        pendingCount > 0 || isSyncing -> Icons.Rounded.CloudSync
        else -> Icons.Rounded.CloudDone
    }
    val label = when {
        isSyncing -> stringResource(R.string.sync_status_syncing)
        hasError -> stringResource(R.string.sync_status_retry)
        pendingCount > 0 -> stringResource(R.string.sync_status_pending, pendingCount)
        else -> stringResource(R.string.sync_status_synced)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(100.dp),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .testTag("sync_status_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = "Cloud delta sync status", tint = fg, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
        }
    }
}

@Composable
private fun DailySummaryCard(
    selectedDate: String,
    completedHabits: Int,
    totalHabits: Int,
    doneTasks: Int,
    totalTasks: Int,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onJumpToday: () -> Unit
) {
    val habitRatio = if (totalHabits == 0) 0f else (completedHabits.toFloat() / totalHabits).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val ringColor = MaterialTheme.colorScheme.tertiary

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
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
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * habitRatio,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke)
                    )
                }
                Text(
                    text = "${(habitRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.summary_counts, completedHabits, totalHabits, doneTasks, totalTasks),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevDay, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
                    }
                    TextButton(onClick = onJumpToday) {
                        Text(
                            text = if (selectedDate == DateUtils.todayIso()) stringResource(R.string.today_label) else selectedDate,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    IconButton(onClick = onNextDay, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    item: HabitWithProgress,
    onQuickToggle: () -> Unit,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = parseHexColor(item.habit.colorHex, MaterialTheme.colorScheme.primary)
    val bg by animateColorAsState(
        targetValue = if (item.isCompleted) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(),
        label = "habitCardBg"
    )

    Surface(
        color = bg,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("habit_card_${item.habit.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = habitIconForName(item.habit.iconName),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val subtitle = when (item.habit.metricType) {
                            "boolean" -> if (item.isCompleted) stringResource(R.string.habit_completed) else stringResource(R.string.habit_daily_checkin)
                            else -> "${item.loggedValue.toInt()} / ${item.habit.targetValue.toInt()} ${item.habit.unit}"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.streakDays > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${item.streakDays}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete habit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (item.isCompleted) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .clickable(onClick = onQuickToggle)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.habit.metricType == "boolean" || item.isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Toggle habit",
                            tint = if (item.isCompleted) MaterialTheme.colorScheme.onTertiary
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        val stepText = when {
                            item.habit.targetValue >= 1000 -> "+250"
                            item.habit.targetValue >= 30 -> "+15"
                            else -> "+1"
                        }
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            if (item.habit.metricType != "boolean") {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskExecutionCard(
    task: TaskEntity,
    subtasks: List<TaskEntity>,
    showTimelineSlot: Boolean,
    onToggleDone: () -> Unit,
    onToggleSubtask: (TaskEntity) -> Unit,
    onAddSubtask: () -> Unit,
    onFocusTask: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.status == "done"
    val priorityColor = when (task.priority) {
        4 -> PriorityUrgentColor
        3 -> PriorityHighColor
        2 -> PriorityMediumColor
        else -> PriorityLowColor
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDone) MaterialTheme.colorScheme.tertiary
                            else priorityColor.copy(alpha = 0.16f)
                        )
                        .clickable(onClick = onToggleDone),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Mark task undone",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showTimelineSlot && task.scheduledStartMs != null && task.scheduledEndMs != null) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${DateUtils.formatTimeMs(task.scheduledStartMs)}–${DateUtils.formatTimeMs(task.scheduledEndMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "${task.estimatedMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isDone) {
                    IconButton(onClick = onAddSubtask) {
                        Icon(
                            imageVector = Icons.Rounded.SubdirectoryArrowRight,
                            contentDescription = "Add subtask",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onFocusTask) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Start focus timer on task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            if (subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.padding(start = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subtasks.forEach { sub ->
                        val subDone = sub.status == "done"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(sub) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (subDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (subDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (subDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTodayCard(
    onAddHabit: () -> Unit,
    onSeedStarter: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.start_cadence_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.start_cadence_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddHabit) { Text(stringResource(R.string.create_habit)) }
                OutlinedButton(onClick = onSeedStarter) { Text(stringResource(R.string.load_starter)) }
            }
        }
    }
}
