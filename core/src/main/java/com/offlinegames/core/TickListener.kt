package com.offlinegames.core

/**
 * Listener interface for receiving frame-based tick updates
 * from the [GameClock].
 *
 * Implementors typically update physics simulations, animations,
 * or per-turn countdowns.
 */
interface TickListener {

    /**
     * Called once per frame with the elapsed time since the last tick.
     *
     * @param deltaSeconds Time in seconds since the previous tick.
     *                     Capped to prevent spiral of death.
     *
     * **Performance contract:**
     * - Must not allocate objects
     * - Must not block
     * - Must not touch the UI thread
     */
    fun onTick(deltaSeconds: Float)

    /**
     * Called when the per-turn timer reaches zero.
     * Default is a no-op; override for turn-based games.
     */
    fun onTurnTimeout() {}
}
