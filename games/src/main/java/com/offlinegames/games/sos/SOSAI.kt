package com.offlinegames.games.sos

import com.offlinegames.core.*

/**
 * Greedy heuristic AI for SOS.
 *
 * Strategy (in priority order):
 * 1. If any move completes an SOS → play it (greedy).
 * 2. If no scoring move, pick a random move that doesn't give the
 *    opponent an easy SOS on their next turn.
 * 3. Fallback: random legal move.
 */
class SOSAI {

    private val rules = SOSRules()

    fun selectMove(state: GameState, aiPlayer: Player): Move? {
        val legalMoves = rules.getLegalMoves(state, aiPlayer)
        if (legalMoves.isEmpty()) return null

        val board = state.boardData as GridBoard

        // 1. Find any move that scores an SOS
        for (move in legalMoves) {
            val piece = move.metadata[SOSRules.META_PIECE_TYPE] ?: continue
            if (rules.countNewSOS(board, move.position.row, move.position.col, pieceOverride = piece) > 0) {
                return move
            }
        }

        // 2. Find moves that don't give the opponent an easy SOS
        val safeMoves = legalMoves.filter { move ->
            !givesOpponentSOS(board, move)
        }

        if (safeMoves.isNotEmpty()) return safeMoves.random()

        // 3. Fallback: random
        return legalMoves.random()
    }

    private val pieces = intArrayOf(SOSRules.PIECE_S, SOSRules.PIECE_O)

    /**
     * Check if the opponent can score an SOS on ANY empty cell
     * after this board state.
     */
    private fun givesOpponentSOS(board: GridBoard, move: Move): Boolean {
        val aiRow = move.position.row
        val aiCol = move.position.col
        val aiPiece = move.metadata[SOSRules.META_PIECE_TYPE] ?: return true

        for (r in 0 until SOSRules.SIZE) {
            for (c in 0 until SOSRules.SIZE) {
                if (r == aiRow && c == aiCol) continue
                if (!board.isEmpty(r, c)) continue
                // Try both S and O
                for (piece in pieces) {
                    if (rules.countNewSOS(board, r, c, pieceOverride = piece,
                            otherRow = aiRow, otherCol = aiCol, otherPiece = aiPiece) > 0) return true
                }
            }
        }
        return false
    }
}
