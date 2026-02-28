package com.offlinegames.games.connect4

import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.ai.HeuristicEvaluator
import com.offlinegames.ai.MinimaxEngine
import com.offlinegames.core.*

/**
 * Heuristic evaluator for Connect 4.
 *
 * Evaluates windows of 4 cells across the board:
 * - 4 own pieces = huge bonus (win)
 * - 3 own + 1 empty = strong
 * - 2 own + 2 empty = moderate
 * - 3 opponent + 1 empty = strong penalty (must block)
 *
 * Also adds a center-column bias so the AI prefers center control.
 */
private class Connect4Heuristic(private val rules: Connect4Rules) : HeuristicEvaluator {

    override fun evaluate(state: GameState, maximizingPlayer: Player): Int {
        val board = state.boardData as GridBoard
        val opponentId = state.players.first { it.id != maximizingPlayer.id }.id

        // Terminal states
        if (rules.hasWon(board, maximizingPlayer.id)) return 100_000
        if (rules.hasWon(board, opponentId)) return -100_000

        var score = 0

        // Center column preference
        val centerCol = Connect4Rules.COLS / 2
        for (r in 0 until Connect4Rules.ROWS) {
            if (board.get(r, centerCol) == maximizingPlayer.id) score += 6
        }

        // Evaluate all windows of 4
        score += evaluateAllWindows(board, maximizingPlayer.id, opponentId)

        return score
    }

    private fun evaluateAllWindows(board: GridBoard, myId: Int, oppId: Int): Int {
        var score = 0
        val rows = Connect4Rules.ROWS
        val cols = Connect4Rules.COLS

        // Horizontal
        for (r in 0 until rows) {
            for (c in 0..cols - 4) {
                score += evaluateWindow(board, myId, oppId, r, c, 0, 1)
            }
        }

        // Vertical
        for (c in 0 until cols) {
            for (r in 0..rows - 4) {
                score += evaluateWindow(board, myId, oppId, r, c, 1, 0)
            }
        }

        // Diagonal down-right
        for (r in 0..rows - 4) {
            for (c in 0..cols - 4) {
                score += evaluateWindow(board, myId, oppId, r, c, 1, 1)
            }
        }

        // Diagonal up-right
        for (r in 3 until rows) {
            for (c in 0..cols - 4) {
                score += evaluateWindow(board, myId, oppId, r, c, -1, 1)
            }
        }

        return score
    }

    private fun evaluateWindow(
        board: GridBoard, myId: Int, oppId: Int,
        startR: Int, startC: Int, dr: Int, dc: Int
    ): Int {
        var mine = 0
        var opp = 0
        var empty = 0
        for (i in 0 until 4) {
            when (board.get(startR + i * dr, startC + i * dc)) {
                myId -> mine++
                oppId -> opp++
                0 -> empty++
            }
        }
        return when {
            mine == 4 -> 100_000
            mine == 3 && empty == 1 -> 50
            mine == 2 && empty == 2 -> 10
            opp == 3 && empty == 1 -> -80  // must block
            opp == 4 -> -100_000
            else -> 0
        }
    }
}

/**
 * AI for Connect 4.
 *
 * Uses alpha-beta minimax with center-column preference heuristic.
 */
class Connect4AI(difficulty: DifficultyProfile = DifficultyProfile.MEDIUM) {

    private val rules = Connect4Rules()
    private val engine = MinimaxEngine(
        rules = rules,
        evaluator = Connect4Heuristic(rules),
        difficulty = difficulty
    )

    fun selectMove(state: GameState, aiPlayer: Player): Move? =
        engine.selectMove(state, aiPlayer)
}
