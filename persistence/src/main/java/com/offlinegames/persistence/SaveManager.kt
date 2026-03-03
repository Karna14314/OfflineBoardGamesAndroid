package com.offlinegames.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Persists in-progress game sessions so the player can resume after
 * leaving the app.
 *
 * Storage format: keyed JSON strings per game ID.
 * We keep it minimal — only the last active match per game type.
 */
class SaveManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "game_saves_encrypted"
        fun saveKey(gameId: String) = "save_$gameId"
        fun saveTimestampKey(gameId: String) = "save_ts_$gameId"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Retrieve the last saved state JSON for [gameId], or null. */
    fun loadSave(gameId: String): Flow<String?> = callbackFlow {
        val key = saveKey(gameId)

        // Emit initial value
        trySend(sharedPreferences.getString(key, null))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, changedKey ->
            if (changedKey == key) {
                trySend(sharedPrefs.getString(key, null))
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    /** Persist [stateJson] as the current save for [gameId]. */
    suspend fun writeSave(gameId: String, stateJson: String) {
        sharedPreferences.edit()
            .putString(saveKey(gameId), stateJson)
            .putLong(saveTimestampKey(gameId), System.currentTimeMillis())
            .apply()
    }

    /** Delete the save for [gameId] (e.g. after the game ends). */
    suspend fun clearSave(gameId: String) {
        sharedPreferences.edit()
            .remove(saveKey(gameId))
            .remove(saveTimestampKey(gameId))
            .apply()
    }
}
