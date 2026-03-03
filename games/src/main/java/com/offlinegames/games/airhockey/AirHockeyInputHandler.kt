package com.offlinegames.games.airhockey

import android.view.MotionEvent
import com.offlinegames.engine.InputHandler

/**
 * Multi-touch input handler for Air Hockey.
 *
 * In 2-Player mode, this handler tracks two separate finger pointers:
 * - Touches in the bottom half (y > 0.5) control Player 1's paddle
 * - Touches in the top half (y <= 0.5) control Player 2's paddle
 *
 * In 1-Player (vs AI) mode, only Player 1's paddle is moved by touch.
 *
 * @param renderer Used for coordinate transformation
 * @param onPlayer1Move Callback with normalised paddle position for Player 1
 * @param onPlayer2Move Callback with normalised paddle position for Player 2 (null in AI mode)
 */
class AirHockeyInputHandler(
    private val renderer: AirHockeyRenderer,
    private val onPaddleMove: (Float, Float) -> Unit,
    private val onPlayer2Move: ((Float, Float) -> Unit)? = null
) : InputHandler {

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        val normalised = renderer.screenToNormalised(touchX, touchY, surfaceW, surfaceH)
            ?: return
        
        val nx = normalised.first
        val ny = normalised.second
        
        if (ny > 0.5f) {
            // Bottom half → Player 1
            onPaddleMove(nx, ny)
        } else if (onPlayer2Move != null) {
            // Top half → Player 2 (only if 2P mode)
            onPlayer2Move.invoke(nx, ny)
        }
    }

    /**
     * Handle raw multi-touch MotionEvent for simultaneous 2-player input.
     * Call this directly from the Activity's touch listener for proper multi-touch.
     */
    fun onMultiTouch(event: MotionEvent, surfaceW: Float, surfaceH: Float) {
        for (i in 0 until event.pointerCount) {
            val touchX = event.getX(i)
            val touchY = event.getY(i)
            val normalised = renderer.screenToNormalised(touchX, touchY, surfaceW, surfaceH)
                ?: continue
            
            val nx = normalised.first
            val ny = normalised.second
            
            if (ny > 0.5f) {
                onPaddleMove(nx, ny)
            } else if (onPlayer2Move != null) {
                onPlayer2Move.invoke(nx, ny)
            }
        }
    }
}
