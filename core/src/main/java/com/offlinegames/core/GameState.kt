package com.offlinegames.core

/**
 * The outcome of a completed game session.
 */
enum class GameResult {
    /** The game is still in progress. */
    IN_PROGRESS,
    /** One of the players has won. */
    WIN,
    /** The game ended without a winner. */
    DRAW
}

/**
 * Immutable snapshot of the entire game at a single point in time.
 *
 * Every action produces a new [GameState]; no mutation ever occurs.
 * Game-specific state (board, pieces, scores) is held in [boardData]
 * as a type-erased Any to keep the core module generic.
 * Concrete game modules cast it to their own typed state.
 *
 * @param gameId        Unique identifier for this game session
 * @param players       Ordered list of participants
 * @param currentPlayer The player whose turn it currently is
 * @param moveHistory   Ordered list of all moves made so far
 * @param result        Current outcome of the game
 * @param boardData     Game-specific board representation (immutable)
 */
data class GameState(
    val gameId: String,
    val players: List<Player>,
    val currentPlayer: Player,
    val moveHistory: List<MoveRecord> = emptyList(),
    val result: GameResult = GameResult.IN_PROGRESS,
    val boardData: Any,
    val scores: Map<Int, Int> = emptyMap()
) {
    /** Convenience: true if the game is still running. */
    val isOngoing: Boolean get() = result == GameResult.IN_PROGRESS

    /** Returns the player who won, or null if the game is not yet decided. */
    fun winner(): Player? {
        if (result != GameResult.WIN) return null
        val lastMove = moveHistory.lastOrNull() ?: return null
        return players.find { it.id == lastMove.move.playerId }
    }
}
