package com.offlinegames.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore singleton attached to the application context. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_games_prefs")

/**
 * Repository for all user-facing settings stored in DataStore.
 *
 * Keys:
 * - [KEY_SOUND_ENABLED]   : boolean, default true
 * - [KEY_HAPTICS_ENABLED] : boolean, default true
 * - [KEY_DARK_THEME]      : boolean, default true
 * - [KEY_PLAYER_ONE_NAME] : string
 * - [KEY_PLAYER_TWO_NAME] : string
 * - [KEY_AI_DIFFICULTY]   : string (name of DifficultyProfile enum)
 */
class PreferencesRepository(private val context: Context) {

    companion object {
        val KEY_SOUND_ENABLED   = booleanPreferencesKey("sound_enabled")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_DARK_THEME      = booleanPreferencesKey("dark_theme")
        val KEY_PLAYER_ONE_NAME = stringPreferencesKey("player_one_name")
        val KEY_PLAYER_TWO_NAME = stringPreferencesKey("player_two_name")
        val KEY_AI_DIFFICULTY   = stringPreferencesKey("ai_difficulty")
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_SOUND_ENABLED] ?: true }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_HAPTICS_ENABLED] ?: true }

    val darkTheme: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_DARK_THEME] ?: true }

    val playerOneName: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_PLAYER_ONE_NAME] ?: "Player 1" }

    val playerTwoName: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_PLAYER_TWO_NAME] ?: "Player 2" }

    val aiDifficulty: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_AI_DIFFICULTY] ?: "MEDIUM" }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTICS_ENABLED] = enabled }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = dark }
    }

    suspend fun setPlayerOneName(name: String) {
        context.dataStore.edit { it[KEY_PLAYER_ONE_NAME] = name }
    }

    suspend fun setPlayerTwoName(name: String) {
        context.dataStore.edit { it[KEY_PLAYER_TWO_NAME] = name }
    }

    suspend fun setAiDifficulty(difficulty: String) {
        context.dataStore.edit { it[KEY_AI_DIFFICULTY] = difficulty }
    }
}
