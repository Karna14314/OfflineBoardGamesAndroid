package com.offlinegames.games.sos

import com.offlinegames.core.*

/**
 * SOS-specific MVI state.
 */
data class SOSState(
    val gameState: GameState,
    val vsAi: Boolean = false,
    val showResultDialog: Boolean = false,
    val selectedPieceType: Int = SOSRules.PIECE_S  // S or O toggle
) {
    val board: GridBoard get() = gameState.boardData as GridBoard
    val currentPlayer: Player get() = gameState.currentPlayer
    val result: GameResult get() = gameState.result
    val isOngoing: Boolean get() = gameState.isOngoing
    val moveCount: Int get() = gameState.moveHistory.size
    val scores: Map<Int, Int> get() = gameState.scores
}
