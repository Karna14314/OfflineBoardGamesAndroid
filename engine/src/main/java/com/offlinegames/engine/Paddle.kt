package com.offlinegames.engine

/**
 * 2D paddle entity for Air Hockey.
 *
 * All values are normalised (0.0 – 1.0) relative to the table dimensions.
 * Paddles are constrained to their half of the table.
 *
 * @param playerId   1 (bottom) or 2 (top)
 * @param x          Normalised x position
 * @param y          Normalised y position
 * @param radius     Normalised radius
 */
class Paddle(
    val playerId: Int,
    var x: Float = 0.5f,
    var y: Float = if (playerId == 1) 0.8f else 0.2f,
    val radius: Float = 0.045f
) {
    /** Previous frame positions for velocity estimation. */
    var prevX: Float = x
        private set
    var prevY: Float = y
        private set

    /**
     * Move paddle to target position, clamping to its half of the table.
     *
     * Player 1 is restricted to y ∈ [0.5, 1.0]
     * Player 2 is restricted to y ∈ [0.0, 0.5]
     */
    fun moveTo(targetX: Float, targetY: Float) {
        prevX = x
        prevY = y

        x = targetX.coerceIn(radius, 1f - radius)
        y = when (playerId) {
            1 -> targetY.coerceIn(0.5f + radius, 1f - radius)
            else -> targetY.coerceIn(radius, 0.5f - radius)
        }
    }

    /** Estimated velocity (units/second) based on position delta. */
    fun estimateVelocityX(dt: Float): Float = if (dt > 0f) (x - prevX) / dt else 0f
    fun estimateVelocityY(dt: Float): Float = if (dt > 0f) (y - prevY) / dt else 0f

    /** Reset paddle to starting position. */
    fun resetToStart() {
        x = 0.5f
        y = if (playerId == 1) 0.8f else 0.2f
        prevX = x
        prevY = y
    }

    /** Snapshot for persistence. */
    fun snapshot(): PaddleSnapshot = PaddleSnapshot(playerId, x, y)

    /** Restore from snapshot. */
    fun restore(s: PaddleSnapshot) {
        x = s.x; y = s.y; prevX = x; prevY = y
    }
}

/** Serialisable snapshot of paddle state. */
data class PaddleSnapshot(
    val playerId: Int,
    val x: Float,
    val y: Float
)
