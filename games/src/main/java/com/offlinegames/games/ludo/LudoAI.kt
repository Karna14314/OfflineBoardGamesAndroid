package com.offlinegames.games.ludo

import com.offlinegames.core.GameAction
import com.offlinegames.core.GameState
import com.offlinegames.core.Player

/**
 * Simple heuristic AI for Ludo.
 *
 * Priority order:
 * 1. Move a token home if exact roll available
 * 2. Capture an opponent token
 * 3. Move the most advanced token on the track
 * 4. Enter a new token from base (on 6)
 * 5. Any available move
 *
 * No minimax — purely greedy heuristic evaluation.
 */
class LudoAI {

    private val rules = LudoRules()

    /**
     * Choose the best action for the AI player.
     * Returns null if no valid move exists.
     *
     * @param board Current Ludo board state
     * @param playerId 0-based player ID
     */
    fun selectAction(board: LudoBoard, playerId: Int): GameAction.TokenMoveAction? {
        val diceValue = board.diceValue ?: return null
        val tokens = board.getPlayerTokens(playerId)
        val movable = tokens.filter { board.canMoveToken(it, diceValue) }

        if (movable.isEmpty()) return null

        // Priority 1: Move a token to exact home
        val homeToken = movable.find { rules.wouldReachHome(board, it, diceValue) }
        if (homeToken != null) {
            return createMoveAction(homeToken, playerId, diceValue)
        }

        // Priority 2: Capture an opponent
        val captureToken = movable.find { rules.wouldCapture(board, it, diceValue) }
        if (captureToken != null) {
            return createMoveAction(captureToken, playerId, diceValue)
        }

        // Priority 3: Move the most advanced on-track token
        val onTrack = movable.filter { it.isOnTrack || it.isInHomeColumn }
            .sortedByDescending { it.step }
        if (onTrack.isNotEmpty()) {
            return createMoveAction(onTrack.first(), playerId, diceValue)
        }

        // Priority 4: Enter from base (should only happen on 6)
        val atBase = movable.find { it.isAtBase }
        if (atBase != null) {
            return createMoveAction(atBase, playerId, diceValue)
        }

        // Priority 5: Any movable token
        return createMoveAction(movable.first(), playerId, diceValue)
    }

    /**
     * Roll the dice (returns random result 1–6).
     */
    fun rollDice(): GameAction.DiceRollAction {
        return GameAction.DiceRollAction((1..6).random())
    }

    private fun createMoveAction(
        token: LudoToken,
        playerId: Int,
        diceValue: Int
    ): GameAction.TokenMoveAction {
        return GameAction.TokenMoveAction(
            tokenId = token.id,
            playerId = playerId,
            steps = diceValue
        )
    }
}
