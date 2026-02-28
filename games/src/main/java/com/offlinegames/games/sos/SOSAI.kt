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
            val newBoard = rules.applyMove(board, move, aiPlayer)
            val sosCount = rules.countNewSOS(newBoard, move.position.row, move.position.col)
            if (sosCount > 0) return move
        }

        // 2. Find moves that don't give the opponent an easy SOS
        val opponentId = state.players.first { it.id != aiPlayer.id }.id
        val safeMoves = legalMoves.filter { move ->
            val newBoard = rules.applyMove(board, move, aiPlayer)
            !givesOpponentSOS(newBoard, opponentId)
        }

        if (safeMoves.isNotEmpty()) return safeMoves.random()

        // 3. Fallback: random
        return legalMoves.random()
    }

    /**
     * Check if the opponent can score an SOS on ANY empty cell
     * after this board state.
     */
    private fun givesOpponentSOS(board: GridBoard, opponentId: Int): Boolean {
        for (r in 0 until SOSRules.SIZE) {
            for (c in 0 until SOSRules.SIZE) {
                if (!board.isEmpty(r, c)) continue
                // Try both S and O
                for (piece in intArrayOf(SOSRules.PIECE_S, SOSRules.PIECE_O)) {
                    val testBoard = board.place(r, c, piece)
                    if (rules.countNewSOS(testBoard, r, c) > 0) return true
                }
            }
        }
        return false
    }
}
