package com.offlinegames.games.tictactoe

import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.ai.HeuristicEvaluator
import com.offlinegames.ai.MinimaxEngine
import com.offlinegames.core.GameResult
import com.offlinegames.core.GameState
import com.offlinegames.core.Move
import com.offlinegames.core.Player
import com.offlinegames.core.GridBoard

/**
 * Heuristic evaluator for TicTacToe positions.
 *
 * Since TicTacToe is a small, solved game, the only meaningful scores
 * are terminal (win / loss / draw). For non-terminal nodes the score is 0.
 */
private class TicTacToeHeuristic(private val rules: TicTacToeRules) : HeuristicEvaluator {
    override fun evaluate(state: GameState, maximizingPlayer: Player): Int {
        val board = state.boardData as GridBoard
        return when {
            rules.hasWon(board, maximizingPlayer.id) ->  1000
            state.players.any { it.id != maximizingPlayer.id && rules.hasWon(board, it.id) } -> -1000
            board.isFull() -> 0
            else -> 0
        }
    }
}

/**
 * AI for TicTacToe.
 *
 * Wraps [MinimaxEngine] with the correct rules and heuristic.
 * Exposes a single [selectMove] function for the ViewModel.
 */
class TicTacToeAI(difficulty: DifficultyProfile = DifficultyProfile.HARD) {

    private val rules = TicTacToeRules()
    private val engine = MinimaxEngine(
        rules = rules,
        evaluator = TicTacToeHeuristic(rules),
        difficulty = difficulty
    )

    /**
     * Select the best move for [aiPlayer] in the given [state].
     * Returns null only if the game is already over.
     */
    fun selectMove(state: GameState, aiPlayer: Player): Move? =
        engine.selectMove(state, aiPlayer)
}
