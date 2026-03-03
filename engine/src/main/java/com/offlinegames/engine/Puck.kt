package com.offlinegames.engine

/**
 * 2D puck entity for Air Hockey.
 *
 * All values are normalised (0.0 – 1.0) relative to the table dimensions.
 * Actual pixel mapping is done by the renderer.
 *
 * Designed for zero-allocation frame updates: mutate via [update].
 */
class Puck(
    var x: Float = 0.5f,
    var y: Float = 0.5f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 0.025f
) {
    /** Update position from velocity. Call once per physics tick. */
    fun update(dt: Float) {
        x += vx * dt
        y += vy * dt
    }

    /** Reset puck to center with zero velocity. */
    fun resetToCenter() {
        x = 0.5f
        y = 0.5f
        vx = 0f
        vy = 0f
    }

    /** Speed (magnitude of velocity vector). */
    fun speed(): Float {
        return kotlin.math.sqrt(vx * vx + vy * vy)
    }

    /** Snapshot for state persistence. */
    fun snapshot(): PuckSnapshot = PuckSnapshot(x, y, vx, vy)

    /** Restore from a snapshot. */
    fun restore(s: PuckSnapshot) {
        x = s.x; y = s.y; vx = s.vx; vy = s.vy
    }
}

/** Serialisable snapshot of puck state. */
data class PuckSnapshot(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float
)
