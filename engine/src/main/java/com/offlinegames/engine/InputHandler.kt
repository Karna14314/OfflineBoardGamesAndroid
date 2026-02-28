package com.offlinegames.engine

import com.offlinegames.core.GameIntent

/**
 * Converts raw screen-space touch coordinates into a typed [GameIntent].
 *
 * Implementations must map (x, y) within the surface dimensions to a
 * meaningful game move. The result is dispatched to the ViewModel.
 *
 * @param onIntent Callback invoked whenever a valid intent is decoded.
 */
interface InputHandler {

    /**
     * Process a finger-up event at surface position ([touchX], [touchY]).
     *
     * @param touchX     X coordinate of the touch, in pixels
     * @param touchY     Y coordinate of the touch, in pixels
     * @param surfaceW   Total width of the surface in pixels
     * @param surfaceH   Total height of the surface in pixels
     */
    fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float)
}
