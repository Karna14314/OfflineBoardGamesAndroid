package com.offlinegames.engine

import android.graphics.Canvas
import com.offlinegames.core.GameState

/**
 * Contract for rendering individual pieces / marks on the board.
 *
 * Separated from [BoardRenderer] so the board grid and piece drawing
 * can evolve independently (e.g. animated pieces on a static board).
 */
interface PieceRenderer {
    /**
     * Draw all pieces for the given [state] on top of the already-drawn board.
     */
    fun drawPieces(canvas: Canvas, state: GameState)
}
