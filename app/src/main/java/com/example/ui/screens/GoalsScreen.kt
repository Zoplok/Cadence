package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.DateUtils
import com.example.data.local.GoalEntity
import com.example.ui.viewmodels.GoalBundle
import com.example.ui.viewmodels.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var statusFilter by remember { mutableStateOf("active") }
    var showCreateGoal by remember { mutableStateOf(false) }
    var addMilestoneForGoal by remember { mutableStateOf<GoalEntity?>(null) }

    val filtered = state.goals.filter { it.goal.status == statusFilter }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGoal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("goals_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create goal")
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
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.goals_kicker),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.goals_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                item {
                    val statusTabs = listOf(
                        "active" to stringResource(R.string.status_active),
                        "completed" to stringResource(R.string.status_completed),
                        "archived" to stringResource(R.string.status_archived)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        statusTabs.forEach { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { statusFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        val currentStatusLabel = when (statusFilter) {
                            "completed" -> stringResource(R.string.status_completed).lowercase()
                            "archived" -> stringResource(R.string.status_archived).lowercase()
                            else -> stringResource(R.string.status_active).lowercase()
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(22.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Flag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.empty_goals_title, currentStatusLabel),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.empty_goals_sub),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (statusFilter == "active") {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = { showCreateGoal = true }) {
                                        Text(stringResource(R.string.create_goal))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.goal.id }) { bundle ->
                        GoalCard(
                            bundle = bundle,
                            onAddMilestone = { addMilestoneForGoal = bundle.goal },
                            onToggleMilestone = { m -> viewModel.toggleMilestone(m) },
                            onIncrementMilestone = { m -> viewModel.incrementMilestone(m, 1.0) },
                            onMarkStatus = { next -> viewModel.updateGoalStatus(bundle.goal, next) },
                            onDelete = { viewModel.deleteGoal(bundle.goal) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showCreateGoal) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Health") }
        var targetDays by remember { mutableStateOf("30") }
        val categories = listOf("Health", "Career", "Finance", "Learning", "Personal")

        ModalBottomSheet(onDismissRequest = { showCreateGoal = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.new_goal), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text(stringResource(R.string.goal_title_label)) },
                    placeholder = { Text(stringResource(R.string.goal_title_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.category_label), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = targetDays,
                    onValueChange = { targetDays = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.target_horizon_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        val days = (targetDays.toIntOrNull() ?: 30).coerceIn(1, 3650)
                        val targetIso = DateUtils.shiftIsoDate(DateUtils.todayIso(), days)
                        viewModel.createGoal(title, category, targetIso)
                        showCreateGoal = false
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_goal_button")
                ) {
                    Text(stringResource(R.string.save_goal))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    addMilestoneForGoal?.let { goal ->
        var mTitle by remember { mutableStateOf("") }
        var targetValText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addMilestoneForGoal = null },
            title = { Text(stringResource(R.string.add_milestone)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mTitle,
                        onValueChange = { mTitle = it.take(120) },
                        label = { Text(stringResource(R.string.milestone_title_label)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetValText,
                        onValueChange = { targetValText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.milestone_numeric_target)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = mTitle.isNotBlank(),
                    onClick = {
                        viewModel.createMilestone(goal.id, mTitle, targetValText.toDoubleOrNull())
                        addMilestoneForGoal = null
                    }
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { addMilestoneForGoal = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun GoalCard(
    bundle: GoalBundle,
    onAddMilestone: () -> Unit,
    onToggleMilestone: (com.example.data.local.MilestoneEntity) -> Unit,
    onIncrementMilestone: (com.example.data.local.MilestoneEntity) -> Unit,
    onMarkStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val goal = bundle.goal
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = goal.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (goal.targetDate != null) {
                        Text(
                            text = stringResource(R.string.due_date_prefix, goal.targetDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete goal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(text = goal.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.goal_counts_summary,
                    bundle.linkedHabitsCount,
                    bundle.linkedTasksCount,
                    bundle.milestones.size
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { bundle.completionFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (bundle.milestones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bundle.milestones.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleMilestone(m) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (m.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = "Toggle milestone",
                                tint = if (m.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = m.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (m.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (m.targetValue != null) {
                                    Text(
                                        text = "${m.currentValue.toInt()} / ${m.targetValue.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (m.targetValue != null && !m.isCompleted) {
                                TextButton(onClick = { onIncrementMilestone(m) }) {
                                    Text("+1")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddMilestone) {
                    Text(stringResource(R.string.add_milestone_btn))
                }
                if (goal.status == "active") {
                    TextButton(onClick = { onMarkStatus("completed") }) {
                        Text(stringResource(R.string.complete_btn))
                    }
                    IconButton(onClick = { onMarkStatus("archived") }) {
                        Icon(Icons.Rounded.Archive, contentDescription = "Archive goal")
                    }
                } else {
                    TextButton(onClick = { onMarkStatus("active") }) {
                        Text(stringResource(R.string.reopen_btn))
                    }
                }
            }
        }
    }
}
