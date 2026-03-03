package com.offlinegames.games.airhockey

import com.offlinegames.core.*
import com.offlinegames.engine.PaddleSnapshot
import com.offlinegames.engine.PhysicsSnapshot
import com.offlinegames.engine.PuckSnapshot

/**
 * Air Hockey game state.
 *
 * Unlike turn-based games, Air Hockey state is primarily
 * driven by the physics engine. The GameState's boardData
 * holds an [AirHockeyBoard] with score snapshots.
 *
 * @param score1       Player 1 score
 * @param score2       Player 2 score
 * @param winScore     Points needed to win
 * @param physics      Snapshot of physics state (for persistence)
 * @param isPaused     True if the game is paused
 */
data class AirHockeyBoard(
    val score1: Int = 0,
    val score2: Int = 0,
    val winScore: Int = 7,
    val physics: PhysicsSnapshot? = null,
    val isPaused: Boolean = false
) {
    /** Check if a player has won. */
    val isGameOver: Boolean get() = score1 >= winScore || score2 >= winScore

    /** Get the winner (1 or 2), or 0 if game not over. */
    val winnerId: Int get() = when {
        score1 >= winScore -> 1
        score2 >= winScore -> 2
        else -> 0
    }
}

/**
 * Air Hockey MVI state.
 */
data class AirHockeyState(
    val gameState: GameState,
    val showResultDialog: Boolean = false
) {
    val board: AirHockeyBoard get() = gameState.boardData as AirHockeyBoard
    val score1: Int get() = board.score1
    val score2: Int get() = board.score2
    val isOngoing: Boolean get() = gameState.isOngoing
}
