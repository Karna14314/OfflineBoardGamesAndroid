package com.offlinegames.engine

import android.view.SurfaceHolder
import com.offlinegames.core.GameState
import com.offlinegames.core.TickListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Dedicated render thread that drives the game loop at a target 60 FPS.
 *
 * The thread reads the latest [GameState] atomically, so the ViewModel
 * can post new states from the main thread without locking.
 *
 * Supports both:
 * - State-based rendering (turn-based games)
 * - Frame-based tick callbacks (real-time games via [TickListener])
 *
 * @param holder         The [SurfaceHolder] from [GameSurfaceView]
 * @param boardRenderer  Draws the board grid / background
 * @param pieceRenderer  Draws pieces on top of the board
 */
class GameThread(
    private val holder: SurfaceHolder,
    private val boardRenderer: BoardRenderer,
    private val pieceRenderer: PieceRenderer
) : Thread("GameRenderThread") {

    /** Target interval between frames (≈ 16.67 ms for 60 FPS). */
    private val targetFrameMillis: Long = 1000L / 60L

    /** Cached background color to avoid parsing per frame */
    private val bgColor = android.graphics.Color.parseColor("#1A1A2E")

    /** Signals the loop to keep running. */
    private val running = AtomicBoolean(false)

    /** Latest state posted by the ViewModel. Null until first update. */
    private val latestState = AtomicReference<GameState?>(null)

    /** Frame-based tick listeners for real-time games. */
    private val tickListeners = CopyOnWriteArrayList<TickListener>()

    /** Last frame timestamp in nanos for delta time calculation. */
    @Volatile
    private var lastFrameNanos: Long = 0L

    /** Post a new state from any thread; it will be picked up next frame. */
    fun updateState(state: GameState) {
        latestState.set(state)
    }

    /** Register a [TickListener] that gets called every frame. */
    fun addTickListener(listener: TickListener) {
        tickListeners.add(listener)
    }

    /** Remove a previously registered [TickListener]. */
    fun removeTickListener(listener: TickListener) {
        tickListeners.remove(listener)
    }

    /** Start the render loop. */
    fun startLoop() {
        running.set(true)
        lastFrameNanos = System.nanoTime()
        start()
    }

    /** Gracefully stop the render loop and wait for the thread to finish. */
    fun stopLoop() {
        running.set(false)
        try {
            join(500)
        } catch (_: InterruptedException) {
            interrupt()
        }
    }

    override fun run() {
        while (running.get()) {
            val frameStartNanos = System.nanoTime()

            // Calculate delta time
            val deltaNanos = frameStartNanos - lastFrameNanos
            lastFrameNanos = frameStartNanos
            // Cap to 3 frames to prevent spiral of death
            val cappedNanos = deltaNanos.coerceAtMost((targetFrameMillis * 3) * 1_000_000L)
            val deltaSeconds = cappedNanos / 1_000_000_000f

            // Notify tick listeners (physics, animation, etc.)
            for (i in 0 until tickListeners.size) {
                val listener = tickListeners[i]
                listener.onTick(deltaSeconds)
            }

            val state = latestState.get()
            if (state == null) {
                sleepForRemainder(frameStartNanos)
                continue
            }

            val canvas = holder.lockCanvas()
            if (canvas == null) {
                sleepForRemainder(frameStartNanos)
                continue
            }

            try {
                synchronized(holder) {
                    canvas.drawColor(bgColor) // dark bg
                    boardRenderer.drawBoard(canvas, state)
                    pieceRenderer.drawPieces(canvas, state)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }

            sleepForRemainder(frameStartNanos)
        }
    }

    private fun sleepForRemainder(frameStartNanos: Long) {
        val elapsedNanos = System.nanoTime() - frameStartNanos
        val remainingNanos = (targetFrameMillis * 1_000_000L) - elapsedNanos
        if (remainingNanos > 0) {
            try {
                val remainingMillis = remainingNanos / 1_000_000L
                val remainingNanosPart = (remainingNanos % 1_000_000L).toInt()
                sleep(remainingMillis, remainingNanosPart)
            } catch (_: InterruptedException) {
                interrupt()
            }
        }
    }
}
