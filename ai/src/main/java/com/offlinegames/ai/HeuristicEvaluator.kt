package com.offlinegames.ai

import com.offlinegames.core.GameState
import com.offlinegames.core.Move
import com.offlinegames.core.Player

/**
 * Provides a numeric score for a [GameState] from the perspective of [maximizingPlayer].
 *
 * Positive scores favour [maximizingPlayer]; negative favour the opponent.
 * Concrete implementations are game-specific.
 */
interface HeuristicEvaluator {

    /**
     * Evaluate [state] from [maximizingPlayer]'s perspective.
     *
     * Conventionally:
     * - `+∞ (Int.MAX_VALUE / 2)` = maximizingPlayer has won
     * - `-∞ (Int.MIN_VALUE / 2)` = opponent has won
     * - `0` = neutral / draw
     */
    fun evaluate(state: GameState, maximizingPlayer: Player): Int
}
