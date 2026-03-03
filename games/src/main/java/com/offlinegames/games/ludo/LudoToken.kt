package com.offlinegames.games.ludo

/**
 * Represents a single Ludo token.
 *
 * @param id       Unique token identifier (0–15 across all players)
 * @param playerId Owner player (0–3)
 * @param tokenIdx Index within the player's set (0–3)
 * @param step     Current logical step on the path:
 *                 - [LudoPath.AT_BASE] (-1): sitting in base
 *                 - 0..[LudoPath.TRACK_LENGTH]-1: on the shared track
 *                 - [LudoPath.TRACK_LENGTH]..[LudoPath.TOTAL_STEPS]-1: in home column
 *                 - [LudoPath.AT_HOME]: reached home (finished)
 */
data class LudoToken(
    val id: Int,
    val playerId: Int,
    val tokenIdx: Int,
    val step: Int = LudoPath.AT_BASE
) {
    /** True if the token is still in the starting base area. */
    val isAtBase: Boolean get() = step == LudoPath.AT_BASE

    /** True if the token has reached home (finished). */
    val isHome: Boolean get() = step == LudoPath.AT_HOME

    /** True if the token is on the shared track (not base, home column, or home). */
    val isOnTrack: Boolean get() = step in 0 until LudoPath.TRACK_LENGTH

    /** True if the token is in the home column. */
    val isInHomeColumn: Boolean get() =
        step in LudoPath.TRACK_LENGTH until LudoPath.AT_HOME

    /**
     * Check if this token can be captured by another player.
     * Tokens at base, home, in the home column, or on safe cells cannot be captured.
     */
    fun canBeCaptured(): Boolean {
        if (isAtBase || isHome || isInHomeColumn) return false
        return !LudoPath.isSafeCell(playerId, step)
    }

    /**
     * Return the absolute track index on the shared track,
     * or -1 if not on the shared track.
     */
    fun absoluteTrackIndex(): Int {
        if (!isOnTrack) return -1
        return LudoPath.trackIndexForStep(playerId, step)
    }

    /** Move forward by [steps]. Returns a new token. */
    fun advance(steps: Int): LudoToken {
        val newStep = step + steps
        return copy(step = newStep.coerceAtMost(LudoPath.AT_HOME))
    }

    /** Send back to base. */
    fun sendToBase(): LudoToken = copy(step = LudoPath.AT_BASE)

    /** Enter the board (step becomes 0). */
    fun enterBoard(): LudoToken = copy(step = 0)
}
