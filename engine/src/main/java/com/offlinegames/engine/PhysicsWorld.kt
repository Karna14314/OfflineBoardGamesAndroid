package com.offlinegames.engine

import com.offlinegames.core.TickListener

/**
 * 2D physics simulation for the Air Hockey table.
 *
 * Manages puck, paddles, collision resolution, and scoring.
 * Runs on the render thread via [TickListener].
 *
 * **Performance contract:**
 * - No object allocations during [onTick]
 * - No blocking operations
 * - No UI thread access
 */
class PhysicsWorld : TickListener {

    val puck = Puck()
    val paddle1 = Paddle(playerId = 1)
    val paddle2 = Paddle(playerId = 2)

    /** Score: player 1 and player 2. */
    var score1: Int = 0
        private set
    var score2: Int = 0
        private set

    /** Callback for goal events. */
    var onGoalScored: ((scoringPlayer: Int) -> Unit)? = null

    /** Points needed to win. */
    var winScore: Int = 7

    /** True when a goal was just scored and we're in a brief reset pause. */
    @Volatile
    var goalPauseTimer: Float = 0f
        private set

    private val GOAL_PAUSE_DURATION = 1.0f  // seconds

    /** Whether the game is over. */
    val isGameOver: Boolean get() = score1 >= winScore || score2 >= winScore

    // ── TickListener ─────────────────────────────────────────────────────

    override fun onTick(deltaSeconds: Float) {
        if (isGameOver) return

        // Goal pause cooldown
        if (goalPauseTimer > 0f) {
            goalPauseTimer -= deltaSeconds
            if (goalPauseTimer <= 0f) {
                goalPauseTimer = 0f
            }
            return
        }

        // Step physics
        puck.update(deltaSeconds)

        // Resolve paddle collisions
        CollisionResolver.resolvePaddle(puck, paddle1, deltaSeconds)
        CollisionResolver.resolvePaddle(puck, paddle2, deltaSeconds)

        // Resolve wall collisions and detect goals
        val goalResult = CollisionResolver.resolveWalls(puck)

        // Apply friction
        CollisionResolver.applyFriction(puck)

        // Handle goals
        if (goalResult != 0) {
            when (goalResult) {
                1 -> score1++
                2 -> score2++
            }
            onGoalScored?.invoke(goalResult)
            puck.resetToCenter()
            goalPauseTimer = GOAL_PAUSE_DURATION
        }
    }

    // ── Control ──────────────────────────────────────────────────────────

    /** Move player 1's paddle to the given normalised position. */
    fun movePlayer1(x: Float, y: Float) {
        paddle1.moveTo(x, y)
    }

    /** Move player 2's paddle (AI or second touch). */
    fun movePlayer2(x: Float, y: Float) {
        paddle2.moveTo(x, y)
    }

    /** Reset the world for a new game. */
    fun reset() {
        puck.resetToCenter()
        paddle1.resetToStart()
        paddle2.resetToStart()
        score1 = 0
        score2 = 0
        goalPauseTimer = 0f
    }

    /** Give the puck an initial nudge toward a random direction. */
    fun servePuck() {
        val angle = (Math.random() * Math.PI * 0.5 + Math.PI * 0.25).toFloat() // 45°-135°
        val speed = 0.6f
        puck.vx = kotlin.math.cos(angle) * speed
        puck.vy = if (Math.random() > 0.5) speed else -speed
    }

    // ── Snapshot / Restore ───────────────────────────────────────────────

    fun snapshot(): PhysicsSnapshot = PhysicsSnapshot(
        puck = puck.snapshot(),
        paddle1 = paddle1.snapshot(),
        paddle2 = paddle2.snapshot(),
        score1 = score1,
        score2 = score2
    )

    fun restore(s: PhysicsSnapshot) {
        puck.restore(s.puck)
        paddle1.restore(s.paddle1)
        paddle2.restore(s.paddle2)
        score1 = s.score1
        score2 = s.score2
    }
}

/** Serialisable snapshot of the entire physics world. */
data class PhysicsSnapshot(
    val puck: PuckSnapshot,
    val paddle1: PaddleSnapshot,
    val paddle2: PaddleSnapshot,
    val score1: Int,
    val score2: Int
)
