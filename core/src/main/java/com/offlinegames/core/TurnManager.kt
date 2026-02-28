package com.offlinegames.core

/**
 * Manages whose turn it is, advancing through a circular player list.
 *
 * [TurnManager] is a pure, stateless helper. The current player lives in
 * [GameState] — TurnManager is only used by the reducer when computing
 * the next state.
 *
 * @param players Ordered list of players participating in the game.
 */
object TurnManager {

    /**
     * Returns the player who follows the current player in the rotation.
     * Wraps around to the first player after the last.
     */
    fun nextPlayer(state: GameState): Player {
        val players = state.players
        require(players.isNotEmpty()) { "A game must have at least one player." }
        val index = players.indexOfFirst { it.id == state.currentPlayer.id }
        val nextIndex = (index + 1) % players.size
        return players[nextIndex]
    }

    /** Returns the first player in the turn order. */
    fun firstPlayer(state: GameState): Player = state.players.first()
}
