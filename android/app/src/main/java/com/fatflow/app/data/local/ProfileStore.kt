package com.fatflow.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "fatflow_profile",
)

class ProfileStore(private val context: Context) {
    private object Keys {
        val START_WEIGHT_LBS = floatPreferencesKey("start_weight_lbs")
        val START_BF_PCT = floatPreferencesKey("start_bf_pct")
        val START_FAT_MASS_LBS = floatPreferencesKey("start_fat_mass_lbs")
        val START_DATE = stringPreferencesKey("start_date")
        val GOAL_WEIGHT_LBS = floatPreferencesKey("goal_weight_lbs")
        val RELEASE_RATE = intPreferencesKey("release_rate")
        val DIET_BREAK_ANCHOR_WEIGHT_LBS = floatPreferencesKey("diet_break_anchor_weight_lbs")
        val DIET_BREAK_ANCHOR_BF_PCT = floatPreferencesKey("diet_break_anchor_bf_pct")
        val DIET_BREAK_START = stringPreferencesKey("diet_break_start")
        val USDA_API_KEY = stringPreferencesKey("usda_api_key")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val profile: Flow<Profile> = context.profileDataStore.data.map { prefs ->
        Profile(
            startWeightLbs = prefs[Keys.START_WEIGHT_LBS] ?: 0f,
            startBfPct = prefs[Keys.START_BF_PCT] ?: 0f,
            startFatMassLbs = prefs[Keys.START_FAT_MASS_LBS] ?: 0f,
            startDate = prefs[Keys.START_DATE]?.let(LocalDate::parse) ?: LocalDate.EPOCH,
            goalWeightLbs = prefs[Keys.GOAL_WEIGHT_LBS] ?: 0f,
            releaseRate = prefs[Keys.RELEASE_RATE] ?: 25,
            dietBreakAnchorWeightLbs = prefs[Keys.DIET_BREAK_ANCHOR_WEIGHT_LBS] ?: 0f,
            dietBreakAnchorBfPct = prefs[Keys.DIET_BREAK_ANCHOR_BF_PCT] ?: 0f,
            dietBreakStart = prefs[Keys.DIET_BREAK_START] ?: "",
            usdaApiKey = prefs[Keys.USDA_API_KEY] ?: "",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
        )
    }

    suspend fun update(transform: (Preferences) -> Preferences) {
        context.profileDataStore.edit { prefs ->
            transform(prefs)
        }
    }

    suspend fun setOnboardingComplete(value: Boolean = true) {
        update { it.toMutablePreferences().apply { this[Keys.ONBOARDING_COMPLETE] = value } }
    }

    suspend fun updateReleaseRate(rate: Int) {
        update { it.toMutablePreferences().apply { this[Keys.RELEASE_RATE] = rate } }
    }

    suspend fun updateGoal(goalLbs: Float) {
        update { it.toMutablePreferences().apply { this[Keys.GOAL_WEIGHT_LBS] = goalLbs } }
    }

    suspend fun updateUsdaKey(key: String) {
        update { it.toMutablePreferences().apply { this[Keys.USDA_API_KEY] = key } }
    }

    suspend fun updateNotifications(enabled: Boolean) {
        update { it.toMutablePreferences().apply { this[Keys.NOTIFICATIONS_ENABLED] = enabled } }
    }

    suspend fun setDietBreakStart(date: LocalDate) {
        update { it.toMutablePreferences().apply { this[Keys.DIET_BREAK_START] = date.toString() } }
    }

    suspend fun clearDietBreakStart() {
        update { it.toMutablePreferences().apply { this[Keys.DIET_BREAK_START] = "" } }
    }

    suspend fun updateDietBreakAnchors(weightLbs: Float, bfPct: Float) {
        update {
            it.toMutablePreferences().apply {
                this[Keys.DIET_BREAK_ANCHOR_WEIGHT_LBS] = weightLbs
                this[Keys.DIET_BREAK_ANCHOR_BF_PCT] = bfPct
            }
        }
    }

    suspend fun saveOnboardingProfile(
        weightLbs: Float,
        bfPct: Float,
        fatMassLbs: Float,
        goalLbs: Float,
        releaseRate: Int,
        usdaKey: String,
        startDate: LocalDate,
    ) {
        update {
            it.toMutablePreferences().apply {
                this[Keys.START_WEIGHT_LBS] = weightLbs
                this[Keys.START_BF_PCT] = bfPct
                this[Keys.START_FAT_MASS_LBS] = fatMassLbs
                this[Keys.START_DATE] = startDate.toString()
                this[Keys.GOAL_WEIGHT_LBS] = goalLbs
                this[Keys.RELEASE_RATE] = releaseRate
                this[Keys.USDA_API_KEY] = usdaKey
                this[Keys.DIET_BREAK_ANCHOR_WEIGHT_LBS] = weightLbs
                this[Keys.DIET_BREAK_ANCHOR_BF_PCT] = bfPct
                this[Keys.ONBOARDING_COMPLETE] = true
            }
        }
    }
}