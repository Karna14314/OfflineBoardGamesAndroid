package com.offlinegames.core

/**
 * GameAction subtypes for frame-based (real-time) updates.
 *
 * These actions are dispatched by the game loop every frame
 * (typically 60 FPS) for games that need continuous simulation
 * (e.g. Air Hockey physics, Ludo token animation).
 *
 * Unlike user-initiated actions, frame updates carry timing data
 * and should be processed without generating sound/haptic effects.
 */
sealed class FrameUpdateAction : GameAction() {

    /**
     * A single physics/animation frame tick.
     *
     * @param deltaSeconds Time elapsed since the previous frame, in seconds.
     *                     Capped by GameClock to prevent spiral of death.
     * @param totalElapsedMs Total wall-clock time since the game session started.
     */
    data class PhysicsTick(
        val deltaSeconds: Float,
        val totalElapsedMs: Long
    ) : FrameUpdateAction()

    /**
     * A dice roll animation frame (for Ludo / similar games).
     *
     * @param displayValue The value to show during the roll animation (1-6).
     * @param isFinal      True when the roll animation is complete and [displayValue]
     *                     is the actual result.
     */
    data class DiceAnimationTick(
        val displayValue: Int,
        val isFinal: Boolean
    ) : FrameUpdateAction()

    /**
     * A token movement animation frame.
     *
     * @param tokenId      Identifier of the token being animated.
     * @param progress     Animation progress from 0.0 (start) to 1.0 (arrived).
     * @param fromStep     Path step the token is moving from.
     * @param toStep       Path step the token is moving to.
     */
    data class TokenAnimationTick(
        val tokenId: Int,
        val progress: Float,
        val fromStep: Int,
        val toStep: Int
    ) : FrameUpdateAction()
}
