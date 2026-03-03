package com.offlinegames.core

/**
 * Engine-level clock system supporting both turn-based timers
 * and real-time frame-based updates.
 *
 * Features:
 * - Per-turn countdown timers
 * - Animation timing (dice roll delays, token movement)
 * - Real-time physics tick at configurable FPS
 *
 * This class is thread-safe: the render thread calls [tick] each frame,
 * while the main thread can [pause]/[resume]/[reset].
 */
class GameClock(
    /** Target frames per second for real-time mode. */
    val targetFps: Int = 60
) {
    /** Nanoseconds per frame at the target FPS. */
    val frameDurationNanos: Long = 1_000_000_000L / targetFps

    /** Target milliseconds per frame. */
    val frameDurationMs: Long = 1000L / targetFps

    /** Timestamp of the last tick (System.nanoTime). */
    @Volatile
    private var lastTickNanos: Long = 0L

    /** Whether the clock is currently running. */
    @Volatile
    var isRunning: Boolean = false
        private set

    /** Whether this clock is paused (ticks are ignored). */
    @Volatile
    var isPaused: Boolean = false
        private set

    /** Total elapsed time in milliseconds since the clock started. */
    @Volatile
    var elapsedMs: Long = 0L
        private set

    /** Per-turn timer: remaining milliseconds, or -1 if no limit. */
    @Volatile
    var turnTimerMs: Long = -1L
        private set

    /** Registered tick listeners. */
    private val listeners = mutableListOf<TickListener>()

    // ── Lifecycle ────────────────────────────────────────────────────────

    /** Start the clock. Call once when the game session begins. */
    fun start() {
        lastTickNanos = System.nanoTime()
        isRunning = true
        isPaused = false
        elapsedMs = 0L
    }

    /** Pause the clock. Ticks will be ignored until [resume]. */
    fun pause() {
        isPaused = true
    }

    /** Resume after a pause. */
    fun resume() {
        if (isPaused) {
            lastTickNanos = System.nanoTime()  // avoid a huge deltaTime spike
            isPaused = false
        }
    }

    /** Stop the clock entirely. */
    fun stop() {
        isRunning = false
        isPaused = false
    }

    /** Reset elapsed time and turn timer. */
    fun reset() {
        elapsedMs = 0L
        turnTimerMs = -1L
        lastTickNanos = System.nanoTime()
    }

    // ── Turn timer ───────────────────────────────────────────────────────

    /** Set a per-turn countdown timer in milliseconds. */
    fun setTurnTimer(durationMs: Long) {
        turnTimerMs = durationMs
    }

    /** Clear any active turn timer. */
    fun clearTurnTimer() {
        turnTimerMs = -1L
    }

    // ── Tick ──────────────────────────────────────────────────────────────

    /**
     * Called once per frame by the render thread.
     *
     * @return the delta time in seconds since the last tick,
     *         or 0 if paused / not running.
     */
    fun tick(): Float {
        if (!isRunning || isPaused) return 0f

        val now = System.nanoTime()
        val deltaNanos = now - lastTickNanos
        lastTickNanos = now

        // Cap delta to prevent spiral of death (e.g. after debugger pause)
        val cappedNanos = deltaNanos.coerceAtMost(frameDurationNanos * 3)
        val deltaSec = cappedNanos / 1_000_000_000f
        val deltaMs = (cappedNanos / 1_000_000L)

        elapsedMs += deltaMs

        // Update turn timer
        if (turnTimerMs > 0) {
            turnTimerMs = (turnTimerMs - deltaMs).coerceAtLeast(0L)
            if (turnTimerMs == 0L) {
                notifyTurnTimeout()
            }
        }

        // Notify listeners
        for (listener in listeners) {
            listener.onTick(deltaSec)
        }

        return deltaSec
    }

    // ── Listeners ────────────────────────────────────────────────────────

    fun addTickListener(listener: TickListener) {
        synchronized(listeners) { listeners.add(listener) }
    }

    fun removeTickListener(listener: TickListener) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    private fun notifyTurnTimeout() {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onTurnTimeout()
            }
        }
    }
}
