package com.offlinegames.games.connect4

import com.offlinegames.core.GameIntent
import com.offlinegames.core.Move
import com.offlinegames.core.MoveType
import com.offlinegames.core.Position
import com.offlinegames.engine.InputHandler

/**
 * Maps touch coordinates to a column drop in Connect 4.
 *
 * Tapping anywhere in a column triggers a COLUMN_DROP move.
 */
class Connect4InputHandler(
    private val onIntent: (GameIntent) -> Unit
) : InputHandler {

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        val cols = Connect4Rules.COLS
        val rows = Connect4Rules.ROWS

        val maxBoardW = surfaceW * 0.90f
        val maxBoardH = surfaceH * 0.70f
        val cellSize = minOf(maxBoardW / cols, maxBoardH / rows)
        val boardW = cols * cellSize
        val left = (surfaceW - boardW) / 2f

        // Check if touch is within board horizontal range
        if (touchX < left || touchX > left + boardW) return

        val col = ((touchX - left) / cellSize).toInt().coerceIn(0, cols - 1)

        onIntent(GameIntent.MakeMove(Move(0, Position(0, col), MoveType.COLUMN_DROP)))
    }
}
