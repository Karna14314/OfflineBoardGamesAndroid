package com.offlinegames.engine

import android.view.SurfaceHolder
import com.offlinegames.core.GameState
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Dedicated render thread that drives the game loop at a target 60 FPS.
 *
 * The thread reads the latest [GameState] atomically, so the ViewModel
 * can post new states from the main thread without locking.
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

    /** Signals the loop to keep running. */
    private val running = AtomicBoolean(false)

    /** Latest state posted by the ViewModel. Null until first update. */
    private val latestState = AtomicReference<GameState?>(null)

    /** Post a new state from any thread; it will be picked up next frame. */
    fun updateState(state: GameState) {
        latestState.set(state)
    }

    /** Start the render loop. */
    fun startLoop() {
        running.set(true)
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
            val frameStart = System.currentTimeMillis()
            val state = latestState.get()
            if (state == null) {
                sleepForRemainder(frameStart)
                continue
            }

            val canvas = holder.lockCanvas()
            if (canvas == null) {
                sleepForRemainder(frameStart)
                continue
            }

            try {
                synchronized(holder) {
                    canvas.drawColor(android.graphics.Color.parseColor("#1A1A2E")) // dark bg
                    boardRenderer.drawBoard(canvas, state)
                    pieceRenderer.drawPieces(canvas, state)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }

            sleepForRemainder(frameStart)
        }
    }

    private fun sleepForRemainder(frameStart: Long) {
        val elapsed = System.currentTimeMillis() - frameStart
        val remaining = targetFrameMillis - elapsed
        if (remaining > 0) {
            try {
                sleep(remaining)
            } catch (_: InterruptedException) {
                interrupt()
            }
        }
    }
}
