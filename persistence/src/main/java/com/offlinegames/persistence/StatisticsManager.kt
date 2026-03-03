package com.offlinegames.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.statsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "statistics")

/**
 * Tracks per-game win/loss/draw counts for each player profile.
 *
 * Keys follow the pattern:  <gameId>_<statType>   e.g. "ttt_wins_p1"
 */
class StatisticsManager(private val context: Context) {

    // -- Key builders --------------------------------------------------------

    private fun winsKey(gameId: String, playerId: Int) =
        intPreferencesKey("${gameId}_wins_p$playerId")

    private fun lossesKey(gameId: String, playerId: Int) =
        intPreferencesKey("${gameId}_losses_p$playerId")

    private fun drawsKey(gameId: String) =
        intPreferencesKey("${gameId}_draws")

    private fun gamesPlayedKey(gameId: String) =
        intPreferencesKey("${gameId}_played")

    private fun highScoreKey(gameId: String) =
        intPreferencesKey("${gameId}_high_score")

    // -- Public API ----------------------------------------------------------

    fun getWins(gameId: String, playerId: Int): Flow<Int> = context.statsDataStore.data
        .map { prefs -> prefs[winsKey(gameId, playerId)] ?: 0 }

    fun getLosses(gameId: String, playerId: Int): Flow<Int> = context.statsDataStore.data
        .map { prefs -> prefs[lossesKey(gameId, playerId)] ?: 0 }

    fun getDraws(gameId: String): Flow<Int> = context.statsDataStore.data
        .map { prefs -> prefs[drawsKey(gameId)] ?: 0 }

    fun getGamesPlayed(gameId: String): Flow<Int> = context.statsDataStore.data
        .map { prefs -> prefs[gamesPlayedKey(gameId)] ?: 0 }

    fun getHighScore(gameId: String): Flow<Int> = context.statsDataStore.data
        .map { prefs -> prefs[highScoreKey(gameId)] ?: 0 }

    /** Record a win for [winnerPlayerId] in [gameId]. */
    suspend fun recordWin(gameId: String, winnerPlayerId: Int) {
        context.statsDataStore.edit { prefs ->
            val key = winsKey(gameId, winnerPlayerId)
            prefs[key] = (prefs[key] ?: 0) + 1
            val played = gamesPlayedKey(gameId)
            prefs[played] = (prefs[played] ?: 0) + 1
        }
    }

    /** Record a draw for [gameId]. */
    suspend fun recordDraw(gameId: String) {
        context.statsDataStore.edit { prefs ->
            val drawKey = drawsKey(gameId)
            prefs[drawKey] = (prefs[drawKey] ?: 0) + 1
            val played = gamesPlayedKey(gameId)
            prefs[played] = (prefs[played] ?: 0) + 1
        }
    }

    /** Update the high score for [gameId] if [score] is higher than the current high score. */
    suspend fun updateHighScore(gameId: String, score: Int) {
        context.statsDataStore.edit { prefs ->
            val key = highScoreKey(gameId)
            val currentHigh = prefs[key] ?: 0
            if (score > currentHigh) {
                prefs[key] = score
            }
        }
    }

    /** Wipe all stats for [gameId] (useful for testing or reset). */
    suspend fun clearStats(gameId: String) {
        context.statsDataStore.edit { prefs ->
            listOf(1, 2).forEach { pid ->
                prefs.remove(winsKey(gameId, pid))
                prefs.remove(lossesKey(gameId, pid))
            }
            prefs.remove(drawsKey(gameId))
            prefs.remove(gamesPlayedKey(gameId))
            prefs.remove(highScoreKey(gameId))
        }
    }
}
