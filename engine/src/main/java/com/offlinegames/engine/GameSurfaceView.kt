package com.offlinegames.engine

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.offlinegames.core.GameState
import com.offlinegames.core.TickListener

/**
 * Generic SurfaceView that hosts the game render loop.
 *
 * This view is completely game-agnostic. Concrete renderers and
 * the input handler are injected so any game can reuse this surface.
 *
 * Supports two input modes:
 * - **Tap-only** (default): fires on ACTION_UP for turn-based games
 * - **Continuous**: fires on every ACTION_MOVE for real-time games (Air Hockey)
 *
 * @param context          Android context
 * @param boardRenderer    Renders the board grid each frame
 * @param pieceRenderer    Renders pieces each frame
 * @param inputHandler     Converts [MotionEvent]s to game intents
 * @param continuousTouch  If true, delivers drag/move events (real-time input)
 */
class GameSurfaceView(
    context: Context,
    private val boardRenderer: BoardRenderer,
    private val pieceRenderer: PieceRenderer,
    private val inputHandler: InputHandler,
    private val continuousTouch: Boolean = false
) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private var currentState: GameState? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    /** Called by the ViewModel whenever the state changes. */
    fun updateState(state: GameState) {
        currentState = state
        gameThread?.updateState(state)
    }

    /** Register a tick listener on the render thread (for real-time games). */
    fun addTickListener(listener: TickListener) {
        gameThread?.addTickListener(listener)
    }

    /** Remove a tick listener from the render thread. */
    fun removeTickListener(listener: TickListener) {
        gameThread?.removeTickListener(listener)
    }

    /** Get the current game thread (for registering listeners after surface creation). */
    fun getGameThread(): GameThread? = gameThread

    // ── SurfaceHolder.Callback ──────────────────────────────────────────────

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread(holder, boardRenderer, pieceRenderer).also { thread ->
            currentState?.let { thread.updateState(it) }
            thread.startLoop()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Resize events — renderers can query canvas.width/height themselves
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.stopLoop()
        gameThread = null
    }

    // ── Touch Input ────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (continuousTouch) {
            // Real-time mode: process ALL pointers for multi-touch (2-player)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    for (i in 0 until event.pointerCount) {
                        inputHandler.onTouch(
                            event.getX(i), event.getY(i),
                            width.toFloat(), height.toFloat()
                        )
                    }
                }
            }
        } else {
            // Turn-based mode: fire only on up
            if (event.action == MotionEvent.ACTION_UP) {
                inputHandler.onTouch(event.x, event.y, width.toFloat(), height.toFloat())
            }
        }
        return true
    }
}
