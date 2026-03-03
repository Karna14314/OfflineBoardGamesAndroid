package com.offlinegames.engine

import kotlin.math.sqrt

/**
 * Resolves all collisions in the Air Hockey simulation.
 *
 * All coordinates are normalised (0.0 – 1.0).
 * The table spans x ∈ [0, 1], y ∈ [0, 1].
 * Goals are centred horizontal strips at y = 0 (top goal) and y = 1 (bottom goal).
 *
 * **Performance contract:**
 * - Zero object allocations per frame
 * - No blocking, no I/O
 */
object CollisionResolver {

    /** Width of the goal opening (normalised, centred). */
    const val GOAL_WIDTH = 0.30f

    /** Coefficient of restitution for wall/paddle bounces. */
    const val RESTITUTION = 0.90f

    /** Friction applied per second (velocity multiplier). */
    const val FRICTION = 0.985f

    /** Maximum puck speed to prevent tunneling. */
    const val MAX_SPEED = 3.0f

    // ── Wall collision ───────────────────────────────────────────────────

    /**
     * Bounce puck off table walls and detect goals.
     *
     * @return 0 if no goal, 1 if top goal scored (Player 1 scores),
     *         2 if bottom goal scored (Player 2 scores).
     */
    fun resolveWalls(puck: Puck): Int {
        val r = puck.radius

        // Left wall
        if (puck.x - r < 0f) {
            puck.x = r
            puck.vx = -puck.vx * RESTITUTION
        }
        // Right wall
        if (puck.x + r > 1f) {
            puck.x = 1f - r
            puck.vx = -puck.vx * RESTITUTION
        }

        val goalLeft = 0.5f - GOAL_WIDTH / 2f
        val goalRight = 0.5f + GOAL_WIDTH / 2f
        val inGoalRange = puck.x in goalLeft..goalRight

        // Top wall / goal (y = 0)
        if (puck.y - r < 0f) {
            if (inGoalRange) {
                return 1  // Player 1 scores (puck entered top goal)
            }
            puck.y = r
            puck.vy = -puck.vy * RESTITUTION
        }

        // Bottom wall / goal (y = 1)
        if (puck.y + r > 1f) {
            if (inGoalRange) {
                return 2  // Player 2 scores (puck entered bottom goal)
            }
            puck.y = 1f - r
            puck.vy = -puck.vy * RESTITUTION
        }

        return 0
    }

    // ── Paddle collision ─────────────────────────────────────────────────

    /**
     * Resolve elastic collision between [puck] and [paddle].
     * Transfers paddle velocity to the puck on impact.
     */
    fun resolvePaddle(puck: Puck, paddle: Paddle, dt: Float) {
        val dx = puck.x - paddle.x
        val dy = puck.y - paddle.y
        val dist = sqrt(dx * dx + dy * dy)
        val minDist = puck.radius + paddle.radius

        if (dist >= minDist || dist < 0.0001f) return

        // Normalise collision vector
        val nx = dx / dist
        val ny = dy / dist

        // Push puck out of overlap
        val overlap = minDist - dist
        puck.x += nx * overlap
        puck.y += ny * overlap

        // Relative velocity (puck − paddle)
        val paddleVx = paddle.estimateVelocityX(dt)
        val paddleVy = paddle.estimateVelocityY(dt)
        val relVx = puck.vx - paddleVx
        val relVy = puck.vy - paddleVy

        // Relative velocity along collision normal
        val relVn = relVx * nx + relVy * ny

        // Don't resolve if moving apart
        if (relVn > 0f) return

        // Apply impulse (paddle has infinite mass relative to puck)
        val impulse = -(1f + RESTITUTION) * relVn
        puck.vx += impulse * nx
        puck.vy += impulse * ny

        clampSpeed(puck)
    }

    // ── Friction ─────────────────────────────────────────────────────────

    /** Apply friction to the puck. Call once per frame. */
    fun applyFriction(puck: Puck) {
        puck.vx *= FRICTION
        puck.vy *= FRICTION

        // Stop if very slow (avoid perpetual creep)
        if (puck.speed() < 0.005f) {
            puck.vx = 0f
            puck.vy = 0f
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun clampSpeed(puck: Puck) {
        val speed = puck.speed()
        if (speed > MAX_SPEED) {
            val scale = MAX_SPEED / speed
            puck.vx *= scale
            puck.vy *= scale
        }
    }
}
