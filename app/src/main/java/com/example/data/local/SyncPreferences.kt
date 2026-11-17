package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "cadence_sync_prefs")

data class SyncMetadata(
    val lastPulledAtMs: Long = 0L,
    val lastSyncState: String = "idle",
    val lastError: String? = null
)

data class CalorieProfile(
    val isMale: Boolean = true,
    val age: Int = 28,
    val weightKg: Float = 72f,
    val heightCm: Float = 175f,
    val activityKey: String = "moderate",
    val goalKey: String = "maintain"
)

class SyncPreferences(private val context: Context) {
    private val appContext = context.applicationContext

    private fun lastPulledKey(userId: String) = longPreferencesKey("last_pulled_ms_$userId")
    private fun syncStateKey(userId: String) = stringPreferencesKey("sync_state_$userId")
    private fun syncErrorKey(userId: String) = stringPreferencesKey("sync_error_$userId")

    private val languageKey = stringPreferencesKey("app_language_tag")
    private val calIsMaleKey = stringPreferencesKey("cal_sex")
    private val calAgeKey = intPreferencesKey("cal_age")
    private val calWeightKey = floatPreferencesKey("cal_weight_kg")
    private val calHeightKey = floatPreferencesKey("cal_height_cm")
    private val calActivityKey = stringPreferencesKey("cal_activity")
    private val calGoalKey = stringPreferencesKey("cal_goal")

    fun observeLanguageTag(): Flow<String> =
        appContext.syncDataStore.data.map { prefs -> prefs[languageKey] ?: "system" }

    suspend fun setLanguageTag(tag: String) {
        appContext.syncDataStore.edit { prefs ->
            prefs[languageKey] = tag
        }
    }

    fun observeCalorieProfile(): Flow<CalorieProfile> =
        appContext.syncDataStore.data.map { prefs ->
            CalorieProfile(
                isMale = (prefs[calIsMaleKey] ?: "male") == "male",
                age = prefs[calAgeKey] ?: 28,
                weightKg = prefs[calWeightKey] ?: 72f,
                heightCm = prefs[calHeightKey] ?: 175f,
                activityKey = prefs[calActivityKey] ?: "moderate",
                goalKey = prefs[calGoalKey] ?: "maintain"
            )
        }

    suspend fun saveCalorieProfile(profile: CalorieProfile) {
        appContext.syncDataStore.edit { prefs ->
            prefs[calIsMaleKey] = if (profile.isMale) "male" else "female"
            prefs[calAgeKey] = profile.age.coerceIn(10, 110)
            prefs[calWeightKey] = profile.weightKg.coerceIn(25f, 300f)
            prefs[calHeightKey] = profile.heightCm.coerceIn(100f, 240f)
            prefs[calActivityKey] = profile.activityKey
            prefs[calGoalKey] = profile.goalKey
        }
    }

    fun observeMetadata(userId: String): Flow<SyncMetadata> =
        appContext.syncDataStore.data.map { prefs ->
            SyncMetadata(
                lastPulledAtMs = prefs[lastPulledKey(userId)] ?: 0L,
                lastSyncState = prefs[syncStateKey(userId)] ?: "idle",
                lastError = prefs[syncErrorKey(userId)]
            )
        }

    suspend fun getLastPulledAtMs(userId: String): Long =
        appContext.syncDataStore.data.first()[lastPulledKey(userId)] ?: 0L

    suspend fun setSyncing(userId: String) {
        appContext.syncDataStore.edit { prefs ->
            prefs[syncStateKey(userId)] = "syncing"
            prefs.remove(syncErrorKey(userId))
        }
    }

    suspend fun setSyncSuccess(userId: String, serverNowMs: Long) {
        appContext.syncDataStore.edit { prefs ->
            prefs[lastPulledKey(userId)] = serverNowMs
            prefs[syncStateKey(userId)] = "synced"
            prefs.remove(syncErrorKey(userId))
        }
    }

    suspend fun setSyncError(userId: String, message: String) {
        appContext.syncDataStore.edit { prefs ->
            prefs[syncStateKey(userId)] = "error"
            prefs[syncErrorKey(userId)] = message
        }
    }
}
