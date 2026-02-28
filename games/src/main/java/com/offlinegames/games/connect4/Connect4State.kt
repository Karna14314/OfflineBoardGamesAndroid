package com.offlinegames.games.connect4

import com.offlinegames.core.*

/**
 * Connect 4-specific MVI state.
 * Wraps [GameState] with typed board access and convenience properties.
 */
data class Connect4State(
    val gameState: GameState,
    val vsAi: Boolean = false,
    val showResultDialog: Boolean = false,
    val lastDropCol: Int = -1,
    val lastDropRow: Int = -1,
    val animatingDrop: Boolean = false
) {
    val board: GridBoard get() = gameState.boardData as GridBoard
    val currentPlayer: Player get() = gameState.currentPlayer
    val result: GameResult get() = gameState.result
    val isOngoing: Boolean get() = gameState.isOngoing
    val moveCount: Int get() = gameState.moveHistory.size
}
