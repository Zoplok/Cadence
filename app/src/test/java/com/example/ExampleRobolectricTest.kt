package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.ActivityMultiplier
import com.example.core.CalorieCalculator
import com.example.core.CalorieGoalMode
import com.example.core.DateUtils
import com.example.core.ExerciseActivity
import com.example.core.UuidV7
import com.example.data.local.CadenceDatabase
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.ui.components.withLocaleTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: CadenceDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CadenceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cadence", appName)
    }

    @Test
    fun `mongolian locale resources resolve accurately`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val mnContext = baseContext.withLocaleTag("mn")
        assertEquals("Ккал тооцоолуур", mnContext.getString(R.string.kcal_title))
        assertEquals("Өнөөдөр", mnContext.getString(R.string.nav_today))
        assertEquals("Calorie (kcal) Calculator", baseContext.withLocaleTag("en").getString(R.string.kcal_title))
    }

    @Test
    fun `calorie calculator computes Mifflin-St Jeor BMR TDEE and MET burn`() {
        // Male, 30y, 70kg, 175cm -> BMR = 10*70 + 6.25*175 - 5*30 + 5 = 700 + 1093.75 - 150 + 5 = 1648.75 -> 1649
        val maleMaintain = CalorieCalculator.calculate(
            isMale = true,
            age = 30,
            weightKg = 70f,
            heightCm = 175f,
            activity = ActivityMultiplier.MODERATE,
            goal = CalorieGoalMode.MAINTAIN
        )
        assertEquals(1649, maleMaintain.bmrKcal)
        assertEquals(2556, maleMaintain.tdeeKcal)
        assertEquals(2556, maleMaintain.targetKcal)
        assertTrue(maleMaintain.proteinGrams > 0 && maleMaintain.carbsGrams > 0 && maleMaintain.fatGrams > 0)

        // Female, 28y, 60kg, 165cm -> BMR = 600 + 1031.25 - 140 - 161 = 1330.25 -> 1330
        val femaleCut = CalorieCalculator.calculate(
            isMale = false,
            age = 28,
            weightKg = 60f,
            heightCm = 165f,
            activity = ActivityMultiplier.LIGHT,
            goal = CalorieGoalMode.CUT
        )
        assertEquals(1330, femaleCut.bmrKcal)
        assertEquals(femaleCut.tdeeKcal - 500, femaleCut.targetKcal)

        // Running (MET 9.8) at 70kg for 30 mins -> 9.8 * 70 * 0.5 = 343 kcal
        val burn = CalorieCalculator.estimateExerciseBurnKcal(
            activity = ExerciseActivity.RUNNING,
            weightKg = 70f,
            durationMinutes = 30
        )
        assertEquals(343, burn)
    }

    @Test
    fun `uuidv7 sorts chronologically`() {
        val older = UuidV7.generate(1_700_000_000_000L)
        val newer = UuidV7.generate(1_700_000_005_000L)
        assertTrue(older < newer)
    }

    @Test
    fun `dynamic SQL CTE streak updates retroactively`() = runTest {
        val uid = "user_test"
        val now = System.currentTimeMillis()
        val habitId = UuidV7.generate(now)
        db.habitDao().upsert(
            HabitEntity(
                id = habitId,
                userId = uid,
                title = "Hydrate",
                metricType = "numeric",
                targetValue = 2000.0,
                unit = "ml",
                createdAtMs = now,
                updatedAtMs = now
            )
        )

        val today = DateUtils.todayIso()
        val yesterday = DateUtils.isoDaysAgo(1)
        val twoDaysAgo = DateUtils.isoDaysAgo(2)

        // Log today only -> streak is 1
        db.habitLogDao().upsert(
            HabitLogEntity(
                id = "${habitId}_$today",
                userId = uid,
                habitId = habitId,
                logDate = today,
                loggedValue = 2500.0,
                createdAtMs = now,
                updatedAtMs = now
            )
        )
        val streakAfterToday = db.habitLogDao().observeCurrentStreaks(uid, today, yesterday).first()
            .associate { it.habitId to it.currentStreak }
        assertEquals(1, streakAfterToday[habitId])

        // Retroactively log yesterday & 2 days ago -> SQL CTE immediately yields 3
        db.habitLogDao().upsert(
            HabitLogEntity(
                id = "${habitId}_$yesterday",
                userId = uid,
                habitId = habitId,
                logDate = yesterday,
                loggedValue = 2000.0,
                createdAtMs = now,
                updatedAtMs = now
            )
        )
        db.habitLogDao().upsert(
            HabitLogEntity(
                id = "${habitId}_$twoDaysAgo",
                userId = uid,
                habitId = habitId,
                logDate = twoDaysAgo,
                loggedValue = 2200.0,
                createdAtMs = now,
                updatedAtMs = now
            )
        )
        val streakAfterRetroactive = db.habitLogDao().observeCurrentStreaks(uid, today, yesterday).first()
            .associate { it.habitId to it.currentStreak }
        assertEquals(3, streakAfterRetroactive[habitId])
    }
}
