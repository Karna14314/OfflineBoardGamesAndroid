package com.offlinegames.engine

import android.graphics.Canvas
import com.offlinegames.core.GameState

/**
 * Contract that every game's board renderer must satisfy.
 *
 * Implementations are completely decoupled from UI framework;
 * they receive only a [Canvas] and the current [GameState].
 */
interface BoardRenderer {
    /**
     * Draw the board background and grid onto [canvas].
     * Called once per frame by [GameThread].
     */
    fun drawBoard(canvas: Canvas, state: GameState)
}
