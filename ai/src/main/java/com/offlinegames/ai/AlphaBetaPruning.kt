package com.offlinegames.ai

import com.offlinegames.core.GameResult
import com.offlinegames.core.GameRules
import com.offlinegames.core.GameState
import com.offlinegames.core.Move
import com.offlinegames.core.Player

/**
 * Alpha-Beta pruning implementation layered on top of Minimax.
 *
 * Eliminates branches that cannot influence the final decision,
 * dramatically reducing the number of nodes evaluated vs plain Minimax.
 *
 * This class is pure Kotlin — no Android APIs used.
 *
 * @param rules     The rules for the game being searched (for move gen & state transitions)
 * @param evaluator Heuristic that scores leaf nodes
 */
class AlphaBetaPruning<S : Any>(
    private val rules: GameRules<S>,
    private val evaluator: HeuristicEvaluator
) {

    private val WIN_SCORE = Int.MAX_VALUE / 2
    private val LOSS_SCORE = Int.MIN_VALUE / 2

    /**
     * Run alpha-beta search from [state] to [depth] plies.
     *
     * @param state            Current game state
     * @param depth            Remaining plies to search
     * @param alpha            Best score maximizer can guarantee so far
     * @param beta             Best score minimizer can guarantee so far
     * @param isMaximizing     True when it's the maximizing player's turn
     * @param maximizingPlayer The AI player we are trying to maximise for
     * @return Heuristic score of the position (not a move)
     */
    @Suppress("UNCHECKED_CAST")
    fun search(
        state: GameState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        maximizingPlayer: Player
    ): Int {
        // Terminal: game over or depth exhausted
        if (depth == 0 || !state.isOngoing) {
            return evaluator.evaluate(state, maximizingPlayer)
        }

        val legalMoves = rules.getLegalMoves(state, state.currentPlayer)
        if (legalMoves.isEmpty()) return evaluator.evaluate(state, maximizingPlayer)

        return if (isMaximizing) {
            var best = LOSS_SCORE
            var a = alpha
            for (move in legalMoves) {
                val nextState = applyMoveToState(state, move)
                val score = search(nextState, depth - 1, a, beta, false, maximizingPlayer)
                best = maxOf(best, score)
                a = maxOf(a, best)
                if (a >= beta) break // β cut-off
            }
            best
        } else {
            var best = WIN_SCORE
            var b = beta
            for (move in legalMoves) {
                val nextState = applyMoveToState(state, move)
                val score = search(nextState, depth - 1, alpha, b, true, maximizingPlayer)
                best = minOf(best, score)
                b = minOf(b, best)
                if (alpha >= b) break // α cut-off
            }
            best
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyMoveToState(state: GameState, move: Move): GameState {
        val currentBoard = state.boardData as S
        val newBoard = rules.applyMove(currentBoard, move, state.currentPlayer)
        // Simple turn rotation for search purposes
        val nextPlayerIndex = (state.players.indexOfFirst { it.id == state.currentPlayer.id } + 1) % state.players.size
        val nextPlayer = state.players[nextPlayerIndex]
        val newState = state.copy(
            boardData = newBoard,
            currentPlayer = nextPlayer
        )
        // Evaluate result after move
        val result = rules.evaluateResult(newState)
        return newState.copy(result = result)
    }
}
