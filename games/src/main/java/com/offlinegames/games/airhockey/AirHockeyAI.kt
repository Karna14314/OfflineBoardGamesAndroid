package com.offlinegames.games.airhockey

import com.offlinegames.engine.PhysicsWorld

/**
 * AI opponent for Air Hockey.
 *
 * Strategy:
 * - Aggressively intercept the puck when it's heading toward the AI's goal
 * - Track the puck when it's on the AI's side
 * - Return to defensive position when puck is on player's side
 * - Push puck forward by moving through it when opportunity arises
 *
 * No object allocations per frame.
 */
class AirHockeyAI(
    /** AI controls player 2 (top half). */
    private val playerId: Int = 2
) {
    /** How aggressively the AI tracks the puck (0.0 – 1.0). Higher = faster. */
    var trackingSpeed: Float = 0.18f

    /** Small random offset to make AI imperfect. */
    private var jitterX: Float = 0f
    private var jitterCounter: Int = 0

    /**
     * Called every frame. Moves the AI paddle toward the predicted
     * intercept position.
     *
     * @param physics The physics world to read puck/paddle state from
     */
    fun update(physics: PhysicsWorld) {
        val puck = physics.puck
        val paddle = physics.paddle2

        // Regenerate jitter periodically
        jitterCounter++
        if (jitterCounter > 30) {
            jitterCounter = 0
            jitterX = (Math.random().toFloat() - 0.5f) * 0.06f
        }

        val targetX: Float
        val targetY: Float
        val speed: Float

        if (puck.y < 0.5f && puck.vy < 0) {
            // Puck is on AI's side AND moving toward AI's goal — intercept aggressively
            speed = 0.25f // Fast reaction

            val timeToReach = if (puck.vy != 0f) {
                (paddle.y - puck.y) / puck.vy
            } else Float.MAX_VALUE

            if (timeToReach > 0 && timeToReach < 2f) {
                // Predict X position at interception time
                targetX = (puck.x + puck.vx * timeToReach).coerceIn(0.1f, 0.9f) + jitterX
                // Move forward to intercept — don't just camp at the goal
                targetY = (puck.y - 0.05f).coerceIn(0.08f, 0.42f)
            } else {
                targetX = puck.x.coerceIn(0.1f, 0.9f) + jitterX
                targetY = 0.2f
            }
        } else if (puck.y < 0.5f) {
            // Puck is on AI's side but moving away or stationary — push it!
            speed = 0.20f
            targetX = puck.x.coerceIn(0.1f, 0.9f) + jitterX
            // Move behind the puck to push it toward the player's goal
            targetY = (puck.y - 0.06f).coerceIn(0.08f, 0.42f)
        } else if (puck.y < 0.65f) {
            // Puck just crossed into player's side — stay near center line ready
            speed = 0.15f
            targetX = puck.x.coerceIn(0.15f, 0.85f) + jitterX
            targetY = 0.35f
        } else {
            // Puck is deep on player's side — return to defensive center
            speed = 0.12f
            targetX = 0.5f + jitterX
            targetY = 0.25f
        }

        // Smoothly move toward target with variable speed
        val newX = paddle.x + (targetX - paddle.x) * speed
        val newY = paddle.y + (targetY - paddle.y) * speed

        physics.movePlayer2(newX, newY)
    }
}
