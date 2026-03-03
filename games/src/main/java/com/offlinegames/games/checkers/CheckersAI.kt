package com.offlinegames.games.checkers

import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.ai.HeuristicEvaluator
import com.offlinegames.ai.MinimaxEngine
import com.offlinegames.core.*

/**
 * AI for Checkers using Minimax with alpha-beta pruning.
 *
 * Heuristic factors:
 * - Piece count (men and kings)
 * - King value (kings worth more than men)
 * - Board position (pieces closer to promotion, center control)
 * - Mobility (number of legal moves)
 */
class CheckersAI(difficulty: DifficultyProfile) {

    private val rules = CheckersRules()
    private val engine = MinimaxEngine(
        rules = rules,
        evaluator = CheckersHeuristic(rules),
        difficulty = difficulty
    )

    /**
     * Select the best move for the AI player.
     */
    fun selectMove(state: GameState, aiPlayer: Player): Move? {
        return engine.selectMove(state, aiPlayer)
    }

    /**
     * Select the best action for the AI player (for ActionBased games).
     */
    fun selectAction(state: GameState, aiPlayer: Player): GameAction? {
        val move = selectMove(state, aiPlayer) ?: return null
        return GameAction.MovePieceAction(move)
    }
}

/**
 * Heuristic evaluator for Checkers positions.
 *
 * Evaluation factors (in order of importance):
 * 1. Material (piece count with king bonus)
 * 2. Position (advancement toward king row, center control)
 * 3. Mobility (number of legal moves)
 * 4. Capture threats
 */
private class CheckersHeuristic(private val rules: CheckersRules) : HeuristicEvaluator {

    companion object {
        const val MAN_VALUE = 100
        const val KING_VALUE = 300
        const val MOBILITY_WEIGHT = 5
        const val ADVANCEMENT_WEIGHT = 10
        const val CENTER_CONTROL_WEIGHT = 15
    }

    override fun evaluate(state: GameState, maximizingPlayer: Player): Int {
        val board = state.boardData as CheckersBoard
        val myId = maximizingPlayer.id
        val opponentId = state.players.first { it.id != myId }.id

        // Terminal positions
        if (board.countPieces(myId) == 0) return -100000
        if (board.countPieces(opponentId) == 0) return 100000
        if (!board.hasLegalMoves(myId)) return -50000
        if (!board.hasLegalMoves(opponentId)) return 50000

        var score = 0

        // Material count
        score += evaluateMaterial(board, myId, opponentId)

        // Position evaluation
        score += evaluatePosition(board, myId, opponentId)

        // Mobility
        score += evaluateMobility(state, board, myId, opponentId)

        return score
    }

    /**
     * Evaluate material balance.
     */
    private fun evaluateMaterial(board: CheckersBoard, myId: Int, opponentId: Int): Int {
        val myMen = board.getPlayerPieces(myId).count { !it.isKing }
        val myKings = board.countKings(myId)
        val oppMen = board.getPlayerPieces(opponentId).count { !it.isKing }
        val oppKings = board.countKings(opponentId)

        return (myMen * MAN_VALUE + myKings * KING_VALUE) -
               (oppMen * MAN_VALUE + oppKings * KING_VALUE)
    }

    /**
     * Evaluate positional factors.
     */
    private fun evaluatePosition(board: CheckersBoard, myId: Int, opponentId: Int): Int {
        var score = 0

        val myPieces = board.getPlayerPieces(myId)
        val oppPieces = board.getPlayerPieces(opponentId)

        // Advancement toward king row
        for (piece in myPieces) {
            if (!piece.isKing) {
                // Player 1 advances toward row 7, Player 2 toward row 0
                val advancement = if (myId == 1) piece.row else (7 - piece.row)
                score += advancement * ADVANCEMENT_WEIGHT
            }
        }

        for (piece in oppPieces) {
            if (!piece.isKing) {
                val advancement = if (opponentId == 1) piece.row else (7 - piece.row)
                score -= advancement * ADVANCEMENT_WEIGHT
            }
        }

        // Center control (pieces in center 4x4 are more valuable)
        val centerMin = 2
        val centerMax = 5

        for (piece in myPieces) {
            if (piece.row in centerMin..centerMax && piece.col in centerMin..centerMax) {
                score += CENTER_CONTROL_WEIGHT
            }
        }

        for (piece in oppPieces) {
            if (piece.row in centerMin..centerMax && piece.col in centerMin..centerMax) {
                score -= CENTER_CONTROL_WEIGHT
            }
        }

        // Penalize pieces on the back row (less mobile)
        for (piece in myPieces) {
            if (piece.row == 0 && myId == 1) score -= 10
            if (piece.row == 7 && myId == 2) score -= 10
        }

        return score
    }

    /**
     * Evaluate mobility (number of legal moves).
     */
    private fun evaluateMobility(
        state: GameState,
        board: CheckersBoard,
        myId: Int,
        opponentId: Int
    ): Int {
        val myPlayer = state.players.first { it.id == myId }
        val oppPlayer = state.players.first { it.id == opponentId }

        val myMoves = rules.getLegalMoves(state, myPlayer).size

        // Create a hypothetical state for opponent
        val oppState = state.copy(currentPlayer = oppPlayer)
        val oppMoves = rules.getLegalMoves(oppState, oppPlayer).size

        return (myMoves - oppMoves) * MOBILITY_WEIGHT
    }
}

/**
 * Enhanced AI that supports chain captures and action-based gameplay.
 */
class CheckersActionAI(private val difficulty: DifficultyProfile) {

    private val rules = CheckersRules()

    /**
     * Select the best action, considering chain captures.
     */
    fun selectAction(state: GameState, aiPlayer: Player): GameAction? {
        val board = state.boardData as CheckersBoard
        val playerPieces = board.getPlayerPieces(aiPlayer.id)

        // Check for forced captures
        val captureMoves = rules.getAllCaptureMoves(board, aiPlayer.id)

        if (captureMoves.isNotEmpty()) {
            // Must capture - find the best capture chain
            return findBestCaptureChain(board, aiPlayer, captureMoves)
        }

        // No captures - use Minimax for regular moves
        val ai = CheckersAI(difficulty)
        return ai.selectAction(state, aiPlayer)
    }

    /**
     * Find the capture chain that results in the best position.
     */
    private fun findBestCaptureChain(
        board: CheckersBoard,
        player: Player,
        captureMoves: List<CheckersMove>
    ): GameAction? {
        var bestChain: List<CheckersMove>? = null
        var bestValue = Int.MIN_VALUE

        for (initialMove in captureMoves) {
            // Get chain captures for this piece
            val fromPiece = board.getPiece(initialMove.from.row, initialMove.from.col)!!
            val chains = rules.getChainCaptures(board, fromPiece)

            if (chains.isEmpty()) {
                // Single capture - evaluate it
                val value = evaluateCapture(board, player, listOf(initialMove))
                if (value > bestValue) {
                    bestValue = value
                    bestChain = listOf(initialMove)
                }
            } else {
                // Evaluate each chain
                for (chain in chains) {
                    val value = evaluateCapture(board, player, chain)
                    if (value > bestValue) {
                        bestValue = value
                        bestChain = chain
                    }
                }
            }
        }

        return bestChain?.let { createActionFromChain(it) }
    }

    /**
     * Evaluate a capture chain.
     */
    private fun evaluateCapture(
        board: CheckersBoard,
        player: Player,
        chain: List<CheckersMove>
    ): Int {
        // Simulate the chain
        var currentBoard = board
        var capturesCount = 0

        for (move in chain) {
            // Apply capture
            for (capturedPos in move.capturedPositions) {
                currentBoard = currentBoard.capturePiece(capturedPos.row, capturedPos.col)
                capturesCount++
            }
            currentBoard = currentBoard.movePiece(move.from.row, move.from.col, move.to.row, move.to.col)
        }

        // Value based on captures (each capture is worth roughly a man)
        return capturesCount * CheckersHeuristic.MAN_VALUE
    }

    private fun createActionFromChain(chain: List<CheckersMove>): GameAction {
        if (chain.size == 1) {
            val move = chain[0]
            return GameAction.CaptureAction(
                move = Move(
                    playerId = 0, // Will be set by reducer
                    position = move.from,
                    type = MoveType.JUMP,
                    metadata = mapOf("toRow" to move.to.row, "toCol" to move.to.col)
                ),
                capturedPiece = move.capturedPositions.first(),
                continueChain = false
            )
        }

        // Multi-jump chain
        val moves = chain.map { checkersMove ->
            Move(
                playerId = 0,
                position = checkersMove.from,
                type = MoveType.JUMP,
                metadata = mapOf("toRow" to checkersMove.to.row, "toCol" to checkersMove.to.col)
            )
        }
        val capturedPositions = chain.flatMap { it.capturedPositions }

        return GameAction.ChainMoveAction(moves, capturedPositions)
    }
}
