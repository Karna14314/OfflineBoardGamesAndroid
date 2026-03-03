package com.offlinegames.games.airhockey

import com.offlinegames.core.*

/**
 * Air Hockey rules.
 *
 * This is a real-time game, so most logic lives in PhysicsWorld.
 * The rules interface exists for compatibility with the engine's
 * GameState framework and for evaluating win conditions.
 */
class AirHockeyRules : ActionBasedRules<AirHockeyBoard> {

    // Legacy GameRules — not used for real-time games
    override fun isValidMove(state: GameState, move: Move): Boolean = false
    override fun getLegalMoves(state: GameState, player: Player): List<Move> = emptyList()
    override fun applyMove(boardData: AirHockeyBoard, move: Move, player: Player) = boardData

    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as AirHockeyBoard
        return when {
            board.score1 >= board.winScore -> GameResult.WIN
            board.score2 >= board.winScore -> GameResult.WIN
            else -> GameResult.IN_PROGRESS
        }
    }

    // ActionBasedRules
    override fun isValidAction(state: GameState, action: GameAction): Boolean {
        return when (action) {
            is GameAction.PaddleMoveAction -> true
            is GameAction.PuckUpdateAction -> true
            is GameAction.RestartAction -> true
            is GameAction.SaveAndExitAction -> true
            else -> false
        }
    }

    override fun applyAction(
        boardData: AirHockeyBoard,
        action: GameAction,
        player: Player
    ): ActionResult<AirHockeyBoard> {
        // Real-time updates happen through PhysicsWorld, not the reducer.
        // Board state is synced periodically for persistence.
        return ActionResult(boardData)
    }
}
