package com.offlinegames.core

/**
 * Represents a player in any board game.
 *
 * @param id       Unique numeric identifier (1 = first player, 2 = second, etc.)
 * @param name     Display name shown in the UI
 * @param isHuman  True for human-controlled, false for AI-controlled
 */
data class Player(
    val id: Int,
    val name: String,
    val isHuman: Boolean = true
) {
    companion object {
        val PLAYER_ONE = Player(id = 1, name = "Player 1")
        val PLAYER_TWO = Player(id = 2, name = "Player 2")
        val AI = Player(id = 2, name = "CPU", isHuman = false)
    }
}
