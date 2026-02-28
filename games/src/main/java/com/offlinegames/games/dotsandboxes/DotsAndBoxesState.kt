package com.offlinegames.games.dotsandboxes

import com.offlinegames.core.*

/**
 * Dots & Boxes MVI state.
 */
data class DotsAndBoxesState(
    val gameState: GameState,
    val vsAi: Boolean = false,
    val showResultDialog: Boolean = false
) {
    val board: GridBoard get() = gameState.boardData as GridBoard
    val currentPlayer: Player get() = gameState.currentPlayer
    val result: GameResult get() = gameState.result
    val isOngoing: Boolean get() = gameState.isOngoing
    val moveCount: Int get() = gameState.moveHistory.size
    val scores: Map<Int, Int> get() = gameState.scores
}
