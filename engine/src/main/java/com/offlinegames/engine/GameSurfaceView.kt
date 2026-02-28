package com.offlinegames.engine

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.offlinegames.core.GameState

/**
 * Generic SurfaceView that hosts the game render loop.
 *
 * This view is completely game-agnostic. Concrete renderers and
 * the input handler are injected so any game can reuse this surface.
 *
 * Lifecycle:
 * - [surfaceCreated]  → starts [GameThread]
 * - [surfaceDestroyed] → stops [GameThread]
 *
 * @param context       Android context
 * @param boardRenderer Renders the board grid each frame
 * @param pieceRenderer Renders pieces each frame
 * @param inputHandler  Converts [MotionEvent]s to game intents
 */
class GameSurfaceView(
    context: Context,
    private val boardRenderer: BoardRenderer,
    private val pieceRenderer: PieceRenderer,
    private val inputHandler: InputHandler
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
        if (event.action == MotionEvent.ACTION_UP) {
            inputHandler.onTouch(event.x, event.y, width.toFloat(), height.toFloat())
        }
        return true
    }
}
