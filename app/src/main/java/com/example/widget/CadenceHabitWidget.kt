package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.core.DateUtils
import com.example.data.local.CadenceDatabase
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.repository.CadenceRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HabitIdParamKey = ActionParameters.Key<String>("habit_id")

data class WidgetHabitItem(
    val habit: HabitEntity,
    val loggedValue: Double
) {
    val isDone: Boolean get() = loggedValue >= habit.targetValue
}

class CadenceHabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val uid = Firebase.auth.currentUser?.uid
        val items: List<WidgetHabitItem> = if (uid.isNullOrBlank()) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                val db = CadenceDatabase.getInstance(context.applicationContext)
                val today = DateUtils.todayIso()
                val habits = db.habitDao().getActiveHabitsSnapshot(uid)
                val logs = db.habitLogDao().getLogsForDateSnapshot(uid, today)
                    .associateBy { it.habitId }
                habits.take(4).map { habit ->
                    WidgetHabitItem(
                        habit = habit,
                        loggedValue = logs[habit.id]?.loggedValue ?: 0.0
                    )
                }
            }
        }

        provideContent {
            GlanceTheme {
                WidgetContent(isSignedIn = !uid.isNullOrBlank(), items = items)
            }
        }
    }

    @Composable
    private fun WidgetContent(
        isSignedIn: Boolean,
        items: List<WidgetHabitItem>
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF161A22)))
                .cornerRadius(20.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CADENCE · TODAY",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF7D9EFF)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                val doneCount = items.count { it.isDone }
                if (isSignedIn && items.isNotEmpty()) {
                    Text(
                        text = "$doneCount/${items.size}",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF4FD69F)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (!isSignedIn) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to sign in & sync habits",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF2F1EC)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to add your first daily habit",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFA0A8B8)),
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                items.forEach { item ->
                    val bgColor = if (item.isDone) Color(0xFF1A2E28) else Color(0xFF212733)
                    val indicatorColor = if (item.isDone) Color(0xFF4FD69F) else Color(0xFF7D9EFF)
                    val statusLabel = when (item.habit.metricType) {
                        "boolean" -> if (item.isDone) "Done" else "Tap"
                        else -> "${item.loggedValue.toInt()}/${item.habit.targetValue.toInt()} ${item.habit.unit}"
                    }

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(ColorProvider(bgColor))
                            .cornerRadius(12.dp)
                            .clickable(
                                actionRunCallback<ToggleHabitAction>(
                                    actionParametersOf(HabitIdParamKey to item.habit.id)
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(10.dp)
                                .background(ColorProvider(indicatorColor))
                                .cornerRadius(5.dp)
                        ) {}
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = item.habit.title,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFF2F1EC)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            style = TextStyle(
                                color = ColorProvider(indicatorColor),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HabitIdParamKey] ?: return
        val uid = Firebase.auth.currentUser?.uid ?: return
        withContext(Dispatchers.IO) {
            val db = CadenceDatabase.getInstance(context.applicationContext)
            val habit = db.habitDao().getById(habitId) ?: return@withContext
            CadenceRepository(context.applicationContext).toggleOrStepHabit(
                userId = uid,
                habit = habit,
                logDate = DateUtils.todayIso()
            )
            CadenceHabitWidget().updateAll(context.applicationContext)
        }
    }
}

class CadenceHabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CadenceHabitWidget()
}
