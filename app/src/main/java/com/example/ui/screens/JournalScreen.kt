package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.DateUtils
import com.example.ui.viewmodels.HeatmapDay
import com.example.ui.viewmodels.JournalViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun JournalScreen(viewModel: JournalViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var moodScore by remember(state.selectedDate, state.currentReview) {
        mutableIntStateOf(state.currentReview?.moodScore ?: 4)
    }
    var energyScore by remember(state.selectedDate, state.currentReview) {
        mutableIntStateOf(state.currentReview?.energyScore ?: 4)
    }
    var winsText by remember(state.selectedDate, state.currentReview) {
        mutableStateOf(state.currentReview?.winsText ?: "")
    }
    var blockersText by remember(state.selectedDate, state.currentReview) {
        mutableStateOf(state.currentReview?.blockersText ?: "")
    }

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
                            text = stringResource(R.string.journal_kicker),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.journal_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Weekly Telemetry Summary Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryTile(
                            label = stringResource(R.string.stat_7d_focus),
                            value = "${state.weeklyFocusMinutes}m",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryTile(
                            label = stringResource(R.string.stat_tasks_done),
                            value = "${state.tasksCompletedCount}",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryTile(
                            label = stringResource(R.string.stat_avg_mood),
                            value = if (state.averageMood == 0f) "—" else String.format(Locale.US, "%.1f", state.averageMood),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 28-Day Habit Consistency Heatmap
                item {
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
                                Text(stringResource(R.string.heatmap_title), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = stringResource(R.string.heatmap_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HabitHeatmapCanvas(days = state.heatmapDays)
                        }
                    }
                }

                // Daily Review Form
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state.selectedDate == DateUtils.todayIso()) {
                                        "${stringResource(R.string.today_label)} · ${state.selectedDate}"
                                    } else {
                                        DateUtils.prettyDate(state.selectedDate)
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Row {
                                    IconButton(onClick = { viewModel.shiftDate(-1) }) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
                                    }
                                    IconButton(onClick = { viewModel.shiftDate(1) }) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(stringResource(R.string.mood_label), style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..5).forEach { score ->
                                    FilterChip(
                                        selected = moodScore == score,
                                        onClick = { moodScore = score },
                                        label = { Text("$score") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(stringResource(R.string.energy_label), style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..5).forEach { score ->
                                    FilterChip(
                                        selected = energyScore == score,
                                        onClick = { energyScore = score },
                                        label = { Text("$score") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = winsText,
                                onValueChange = { winsText = it.take(2000) },
                                label = { Text(stringResource(R.string.wins_label)) },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("journal_wins_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = blockersText,
                                onValueChange = { blockersText = it.take(2000) },
                                label = { Text(stringResource(R.string.blockers_label)) },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("journal_blockers_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            val savedToastMsg = stringResource(R.string.reflection_saved_msg)
                            Button(
                                onClick = {
                                    viewModel.saveReview(moodScore, energyScore, winsText, blockersText)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(savedToastMsg)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("save_journal_button")
                            ) {
                                Icon(Icons.Rounded.BookmarkAdded, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.save_reflection))
                            }
                        }
                    }
                }

                if (state.recentReviews.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.recent_entries), style = MaterialTheme.typography.titleLarge)
                    }
                    items(state.recentReviews.take(10), key = { it.id }) { rev ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(rev.reviewDate, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = stringResource(R.string.mood_energy_line, rev.moodScore, rev.energyScore),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (rev.winsText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(rev.winsText, style = MaterialTheme.typography.bodySmall)
                                }
                                if (rev.blockersText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rev.blockersText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
private fun TelemetryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HabitHeatmapCanvas(days: List<HeatmapDay>) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val baseAccent = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        val cols = 7
        val rows = 4
        val gap = 6.dp.toPx()
        val cellW = (size.width - gap * (cols - 1)) / cols
        val cellH = (size.height - gap * (rows - 1)) / rows

        days.take(28).forEachIndexed { index, day ->
            val r = index / cols
            val c = index % cols
            val left = c * (cellW + gap)
            val top = r * (cellH + gap)
            val cellColor = when {
                day.completionRatio <= 0f -> emptyColor
                day.completionRatio < 0.4f -> baseAccent.copy(alpha = 0.3f)
                day.completionRatio < 0.8f -> baseAccent.copy(alpha = 0.65f)
                else -> baseAccent
            }
            drawRoundRect(
                color = cellColor,
                topLeft = Offset(left, top),
                size = Size(cellW, cellH),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }
    }
}
