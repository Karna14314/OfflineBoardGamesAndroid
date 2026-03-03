package com.offlinegames.games.twenty48

import com.offlinegames.core.GameAction
import com.offlinegames.core.SwipeDirection
import com.offlinegames.engine.InputHandler
import kotlin.math.abs

/**
 * Converts touch gestures into 2048 game actions.
 *
 * Detects swipe gestures (up/down/left/right) and dispatches
 * [GameAction.MergeTilesAction] to the ViewModel.
 *
 * @param onAction Callback invoked when a valid swipe is detected.
 */
class Game2048InputHandler(
    private val onAction: (GameAction) -> Unit
) : InputHandler {

    companion object {
        private const val SWIPE_THRESHOLD = 50f  // Minimum distance for swipe
        private const val SWIPE_VELOCITY_THRESHOLD = 100f
    }

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var startTime: Long = 0L

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        // This method handles ACTION_UP events
        // We need to track ACTION_DOWN separately via gesture detection
    }

    /**
     * Called when a touch gesture begins (ACTION_DOWN).
     */
    fun onTouchDown(x: Float, y: Float) {
        startX = x
        startY = y
        startTime = System.currentTimeMillis()
    }

    /**
     * Called when a touch gesture ends (ACTION_UP).
     * Determines swipe direction and dispatches appropriate action.
     */
    fun onTouchUp(endX: Float, endY: Float) {
        val deltaX = endX - startX
        val deltaY = endY - startY
        val duration = System.currentTimeMillis() - startTime

        // Calculate velocity
        val velocityX = abs(deltaX) / duration * 1000f
        val velocityY = abs(deltaY) / duration * 1000f

        // Determine if this is a valid swipe
        val isHorizontalSwipe = abs(deltaX) > abs(deltaY)
        val isValidSwipe = if (isHorizontalSwipe) {
            abs(deltaX) > SWIPE_THRESHOLD && velocityX > SWIPE_VELOCITY_THRESHOLD
        } else {
            abs(deltaY) > SWIPE_THRESHOLD && velocityY > SWIPE_VELOCITY_THRESHOLD
        }

        if (!isValidSwipe) return

        // Determine direction
        val direction = when {
            isHorizontalSwipe && deltaX > 0 -> SwipeDirection.RIGHT
            isHorizontalSwipe && deltaX < 0 -> SwipeDirection.LEFT
            !isHorizontalSwipe && deltaY > 0 -> SwipeDirection.DOWN
            !isHorizontalSwipe && deltaY < 0 -> SwipeDirection.UP
            else -> return
        }

        onAction(GameAction.MergeTilesAction(direction))
    }

    /**
     * Process a complete swipe gesture in one call.
     * Alternative API for gesture detectors.
     */
    fun onSwipe(direction: SwipeDirection) {
        onAction(GameAction.MergeTilesAction(direction))
    }
}
