package com.offlinegames.core

/**
 * Base interface for any move in any board game.
 *
 * Implementing classes carry the game-specific move data
 * (e.g. row/column for TicTacToe, from/to squares for Chess).
 * Moves must be value-comparable so the reducer can validate them.
 */
data class Position(val row: Int, val col: Int)

enum class MoveType {
    PLACE, SLIDE, JUMP, COLUMN_DROP, EDGE
}

/**
 * Universal move object for any board game.
 */
data class Move(
    val playerId: Int,
    val position: Position,
    val type: MoveType = MoveType.PLACE,
    val metadata: Map<String, Int> = emptyMap()
)

/**
 * A complete, timestamped record of a move.
 */
data class MoveRecord(
    val move: Move,
    val timestampMs: Long = System.currentTimeMillis()
)
