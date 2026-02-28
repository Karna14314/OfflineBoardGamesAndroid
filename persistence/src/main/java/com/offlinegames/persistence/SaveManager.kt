package com.offlinegames.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.saveDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "game_saves")

/**
 * Persists in-progress game sessions so the player can resume after
 * leaving the app.
 *
 * Storage format: keyed JSON strings per game ID.
 * We keep it minimal — only the last active match per game type.
 */
class SaveManager(private val context: Context) {

    companion object {
        fun saveKey(gameId: String) = stringPreferencesKey("save_$gameId")
        fun saveTimestampKey(gameId: String) = longPreferencesKey("save_ts_$gameId")
    }

    /** Retrieve the last saved state JSON for [gameId], or null. */
    fun loadSave(gameId: String): Flow<String?> = context.saveDataStore.data
        .map { prefs -> prefs[saveKey(gameId)] }

    /** Persist [stateJson] as the current save for [gameId]. */
    suspend fun writeSave(gameId: String, stateJson: String) {
        context.saveDataStore.edit { prefs ->
            prefs[saveKey(gameId)] = stateJson
            prefs[saveTimestampKey(gameId)] = System.currentTimeMillis()
        }
    }

    /** Delete the save for [gameId] (e.g. after the game ends). */
    suspend fun clearSave(gameId: String) {
        context.saveDataStore.edit { prefs ->
            prefs.remove(saveKey(gameId))
            prefs.remove(saveTimestampKey(gameId))
        }
    }
}
