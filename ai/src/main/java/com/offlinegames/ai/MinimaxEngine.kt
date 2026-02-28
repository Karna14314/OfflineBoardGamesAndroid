package com.offlinegames.ai

import com.offlinegames.core.GameRules
import com.offlinegames.core.GameState
import com.offlinegames.core.Move
import com.offlinegames.core.Player

/**
 * High-level AI engine that selects the best move for the AI player.
 *
 * Uses [AlphaBetaPruning] internally. The depth is controlled by
 * the [DifficultyProfile] provided at construction time.
 *
 * Usage:
 * ```kotlin
 * val engine = MinimaxEngine(rules, evaluator, DifficultyProfile.HARD)
 * val bestMove = engine.selectMove(currentState, aiPlayer)
 * ```
 *
 * @param rules      Game-specific move generation and application logic
 * @param evaluator  Position evaluator for leaf nodes
 * @param difficulty Search depth profile
 */
class MinimaxEngine<S : Any>(
    private val rules: GameRules<S>,
    private val evaluator: HeuristicEvaluator,
    private val difficulty: DifficultyProfile = DifficultyProfile.HARD
) {

    private val alphaBeta = AlphaBetaPruning(rules, evaluator)

    /**
     * Select the best legal move for [aiPlayer] in [state].
     *
     * Returns null if there are no legal moves (game is over).
     */
    @Suppress("UNCHECKED_CAST")
    fun selectMove(state: GameState, aiPlayer: Player): Move? {
        val legalMoves = rules.getLegalMoves(state, aiPlayer)
        if (legalMoves.isEmpty()) return null

        var bestScore = Int.MIN_VALUE
        var bestMove = legalMoves.first()

        for (move in legalMoves) {
            val currentBoard = state.boardData as S
            val newBoard = rules.applyMove(currentBoard, move, aiPlayer)
            val nextPlayerIndex = (state.players.indexOfFirst { it.id == aiPlayer.id } + 1) % state.players.size
            val nextPlayer = state.players[nextPlayerIndex]
            val nextState = state.copy(
                boardData = newBoard,
                currentPlayer = nextPlayer,
                result = rules.evaluateResult(
                    state.copy(boardData = newBoard, currentPlayer = nextPlayer)
                )
            )

            val score = alphaBeta.search(
                state = nextState,
                depth = difficulty.maxDepth - 1,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
                isMaximizing = false, // opponent's turn next
                maximizingPlayer = aiPlayer
            )

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        return bestMove
    }
}
