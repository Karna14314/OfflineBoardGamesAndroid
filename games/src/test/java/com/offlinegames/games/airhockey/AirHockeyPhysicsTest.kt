package com.offlinegames.games.airhockey

import com.offlinegames.engine.CollisionResolver
import com.offlinegames.engine.Paddle
import com.offlinegames.engine.Puck
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Air Hockey physics.
 *
 * Tests cover:
 * - Wall collision / boundary reflection
 * - Paddle-puck elastic collision
 * - Goal detection
 * - Friction
 * - Speed clamping
 */
class AirHockeyPhysicsTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Wall Collision Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `wall - puck bounces off left wall`() {
        val puck = Puck(x = 0.01f, y = 0.5f, vx = -1f, vy = 0f, radius = 0.025f)
        puck.update(0.016f) // Move into wall

        val result = CollisionResolver.resolveWalls(puck)

        assertEquals("No goal should be scored", 0, result)
        assertTrue("Puck x should be >= radius", puck.x >= puck.radius)
        assertTrue("Velocity should be reversed", puck.vx > 0)
    }

    @Test
    fun `wall - puck bounces off right wall`() {
        val puck = Puck(x = 0.99f, y = 0.5f, vx = 1f, vy = 0f, radius = 0.025f)
        puck.update(0.016f)

        val result = CollisionResolver.resolveWalls(puck)

        assertEquals("No goal should be scored", 0, result)
        assertTrue("Puck x should be <= 1-radius", puck.x <= 1f - puck.radius)
        assertTrue("Velocity should be reversed", puck.vx < 0)
    }

    @Test
    fun `wall - puck bounces off top wall outside goal`() {
        // Place puck outside goal zone
        val puck = Puck(x = 0.1f, y = 0.01f, vx = 0f, vy = -1f, radius = 0.025f)
        puck.update(0.016f)

        val result = CollisionResolver.resolveWalls(puck)

        assertEquals("Should bounce, no goal", 0, result)
        assertTrue("Puck y should be >= radius", puck.y >= puck.radius)
        assertTrue("Velocity should be reversed", puck.vy > 0)
    }

    @Test
    fun `wall - puck bounces off bottom wall outside goal`() {
        val puck = Puck(x = 0.1f, y = 0.99f, vx = 0f, vy = 1f, radius = 0.025f)
        puck.update(0.016f)

        val result = CollisionResolver.resolveWalls(puck)

        assertEquals("Should bounce, no goal", 0, result)
        assertTrue("Puck y should be <= 1-radius", puck.y <= 1f - puck.radius)
        assertTrue("Velocity should be reversed", puck.vy < 0)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Goal Detection Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `goal - puck enters top goal (Player 1 scores)`() {
        // Place puck in goal zone, heading into goal
        val puck = Puck(x = 0.5f, y = 0.01f, vx = 0f, vy = -1f, radius = 0.025f)
        puck.update(0.016f) // Push puck past top edge

        val result = CollisionResolver.resolveWalls(puck)

        assertEquals("Player 1 should score", 1, result)
    }

    @Test
    fun `goal - puck enters bottom goal (Player 2 scores)`() {
        val puck = Puck(x = 0.5f, y = 0.99f, vx = 0f, vy = 1f, radius = 0.025f)
        puck.update(0.016f)

        val result = CollisionResolver.resolveWalls(puck)

        assertEquals("Player 2 should score", 2, result)
    }

    @Test
    fun `goal - puck outside goal zone does not score`() {
        // Puck at far left, outside goal zone
        val puck = Puck(x = 0.1f, y = 0.01f, vx = 0f, vy = -1f, radius = 0.025f)
        puck.update(0.016f)

        val result = CollisionResolver.resolveWalls(puck)

        assertEquals("Should not score at edge", 0, result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Paddle Collision Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `paddle - puck bounces off paddle`() {
        val puck = Puck(x = 0.5f, y = 0.76f, vx = 0f, vy = 0.5f, radius = 0.025f)
        val paddle = Paddle(playerId = 1, x = 0.5f, y = 0.8f, radius = 0.045f)

        CollisionResolver.resolvePaddle(puck, paddle, 0.016f)

        assertTrue("Puck should bounce away from paddle (vy should be negative)",
            puck.vy < 0 || puck.y < 0.76f)
    }

    @Test
    fun `paddle - puck not affected when far from paddle`() {
        val puck = Puck(x = 0.5f, y = 0.3f, vx = 0f, vy = 0.5f, radius = 0.025f)
        val paddle = Paddle(playerId = 1, x = 0.5f, y = 0.8f, radius = 0.045f)

        val vyBefore = puck.vy
        CollisionResolver.resolvePaddle(puck, paddle, 0.016f)

        assertEquals("Puck velocity should be unchanged", vyBefore, puck.vy, 0.001f)
    }

    @Test
    fun `paddle - player constrained to half`() {
        val paddle1 = Paddle(playerId = 1)
        paddle1.moveTo(0.5f, 0.3f) // Try to move into top half

        assertTrue("Player 1 should be clamped to bottom half",
            paddle1.y >= 0.5f)

        val paddle2 = Paddle(playerId = 2)
        paddle2.moveTo(0.5f, 0.7f) // Try to move into bottom half

        assertTrue("Player 2 should be clamped to top half",
            paddle2.y <= 0.5f)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Friction Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `friction - puck slows down over time`() {
        val puck = Puck(x = 0.5f, y = 0.5f, vx = 1f, vy = 1f, radius = 0.025f)
        val speedBefore = puck.speed()

        repeat(10) {
            CollisionResolver.applyFriction(puck)
        }

        assertTrue("Puck should slow down", puck.speed() < speedBefore)
    }

    @Test
    fun `friction - very slow puck stops completely`() {
        val puck = Puck(x = 0.5f, y = 0.5f, vx = 0.001f, vy = 0.001f, radius = 0.025f)

        CollisionResolver.applyFriction(puck)

        assertEquals("Puck vx should be 0", 0f, puck.vx, 0.001f)
        assertEquals("Puck vy should be 0", 0f, puck.vy, 0.001f)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Speed Clamping Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `speed - puck speed is clamped after paddle hit`() {
        val puck = Puck(x = 0.5f, y = 0.75f, vx = 0f, vy = 5f, radius = 0.025f) // Very fast
        val paddle = Paddle(playerId = 1, x = 0.5f, y = 0.8f, radius = 0.045f)
        // Bring puck close and simulate collision
        puck.y = paddle.y - paddle.radius - puck.radius + 0.01f

        CollisionResolver.resolvePaddle(puck, paddle, 0.016f)

        assertTrue("Puck speed should be clamped",
            puck.speed() <= CollisionResolver.MAX_SPEED + 0.01f)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Integration Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `full frame - physics step does not crash`() {
        val puck = Puck()
        puck.vx = 0.5f
        puck.vy = -0.5f

        val paddle1 = Paddle(1)
        val paddle2 = Paddle(2)

        // Simulate 100 frames
        repeat(100) {
            puck.update(0.016f)
            CollisionResolver.resolvePaddle(puck, paddle1, 0.016f)
            CollisionResolver.resolvePaddle(puck, paddle2, 0.016f)
            CollisionResolver.resolveWalls(puck)
            CollisionResolver.applyFriction(puck)
        }

        // Should not throw, and puck should be within bounds
        assertTrue("Puck x in range", puck.x in -0.1f..1.1f)
        assertTrue("Puck y in range", puck.y in -0.1f..1.1f)
    }
}
