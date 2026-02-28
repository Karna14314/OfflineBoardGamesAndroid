package com.offlinegames.games.tictactoe

import com.offlinegames.core.*

/**
 * TicTacToe-specific MVI state.
 *
 * Wraps [GameState] with a typed board and convenience accessors.
 * The [gameState] is the single source of truth; everything else is derived.
 */
data class TicTacToeState(
    val gameState: GameState,
    val vsAi: Boolean = false,
    val showResultDialog: Boolean = false
) {
    val board: GridBoard get() = gameState.boardData as GridBoard
    val currentPlayer: Player  get() = gameState.currentPlayer
    val result: GameResult     get() = gameState.result
    val isOngoing: Boolean     get() = gameState.isOngoing
    val moveCount: Int         get() = gameState.moveHistory.size
}
