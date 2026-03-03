package com.offlinegames.games.ludo

import com.offlinegames.core.*

/**
 * Immutable Ludo board state.
 *
 * @param tokens       All tokens across all players (16 total for 4 players, 8 for 2)
 * @param playerCount  Number of players (2–4)
 * @param diceValue    Last dice roll result (1–6), or null if not yet rolled
 * @param diceRolled   True if the current player has rolled the dice this turn
 * @param turnPlayerId Current player's ID (0-based)
 * @param extraTurn    True if the current player gets an extra turn (rolled 6)
 * @param winnerId     ID of the player who won, or -1 if game ongoing
 */
data class LudoBoard(
    val tokens: List<LudoToken>,
    val playerCount: Int,
    val diceValue: Int? = null,
    val diceRolled: Boolean = false,
    val turnPlayerId: Int = 0,
    val extraTurn: Boolean = false,
    val winnerId: Int = -1
) {
    companion object {
        const val TOKENS_PER_PLAYER = 4

        /**
         * Create initial board with all tokens at base.
         */
        fun create(playerCount: Int): LudoBoard {
            val tokens = mutableListOf<LudoToken>()
            for (p in 0 until playerCount) {
                for (t in 0 until TOKENS_PER_PLAYER) {
                    tokens.add(LudoToken(
                        id = p * TOKENS_PER_PLAYER + t,
                        playerId = p,
                        tokenIdx = t
                    ))
                }
            }
            return LudoBoard(tokens = tokens, playerCount = playerCount)
        }
    }

    /** Get all tokens belonging to a player. */
    fun getPlayerTokens(playerId: Int): List<LudoToken> =
        tokens.filter { it.playerId == playerId }

    /** Get a specific token by ID. */
    fun getToken(tokenId: Int): LudoToken? = tokens.find { it.id == tokenId }

    /** Check if a player has brought all tokens home. */
    fun allTokensHome(playerId: Int): Boolean =
        getPlayerTokens(playerId).all { it.isHome }

    /** Replace a token by ID with a new one. */
    fun updateToken(tokenId: Int, updater: (LudoToken) -> LudoToken): LudoBoard {
        val newTokens = tokens.map { if (it.id == tokenId) updater(it) else it }
        return copy(tokens = newTokens)
    }

    /** Replace a token at a specific absolute track index. */
    fun getTokenAtAbsoluteIndex(trackIndex: Int, excludePlayerId: Int): LudoToken? {
        return tokens.find {
            it.isOnTrack &&
            it.playerId != excludePlayerId &&
            it.absoluteTrackIndex() == trackIndex
        }
    }

    /** Check if a player has any movable tokens given a dice value. */
    fun hasMovableTokens(playerId: Int, diceValue: Int): Boolean {
        val playerTokens = getPlayerTokens(playerId)
        return playerTokens.any { canMoveToken(it, diceValue) }
    }

    /** Check if a specific token can be moved with the given dice value. */
    fun canMoveToken(token: LudoToken, diceValue: Int): Boolean {
        if (token.isHome) return false

        // Token at base can only move if dice is 6
        if (token.isAtBase) return diceValue == 6

        // Check if move would overshoot home
        val newStep = token.step + diceValue
        return newStep <= LudoPath.AT_HOME
    }
}

/**
 * Ludo-specific MVI state (wraps core GameState).
 */
data class LudoState(
    val gameState: GameState,
    val playerCount: Int = 2,
    val showResultDialog: Boolean = false,
    val selectedTokenId: Int? = null,
    val movableTokenIds: List<Int> = emptyList(),
    val isDiceAnimating: Boolean = false,
    val animatingDiceValue: Int = 1
) {
    val board: LudoBoard get() = gameState.boardData as LudoBoard
    val currentPlayer: Player get() = gameState.currentPlayer
    val result: GameResult get() = gameState.result
    val isOngoing: Boolean get() = gameState.isOngoing
}
