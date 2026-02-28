package com.offlinegames.games.tictactoe

import com.offlinegames.core.GameIntent
import com.offlinegames.core.Move
import com.offlinegames.core.Position
import com.offlinegames.engine.InputHandler

/**
 * Maps touch coordinates on the [GameSurfaceView] to a [TicTacToeMove].
 *
 * The board occupies an 80% square centred on the surface.
 * Tapping outside the board is silently ignored.
 *
 * @param onIntent Callback invoked with [GameIntent.MakeMove] when the
 *                 user taps a valid cell.
 */
class TicTacToeInputHandler(
    private val onIntent: (GameIntent) -> Unit
) : InputHandler {

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        val boardSize = minOf(surfaceW, surfaceH) * 0.80f
        val left = (surfaceW - boardSize) / 2f
        val top  = (surfaceH - boardSize) / 2f - boardSize * 0.08f
        val cellSize = boardSize / 3f

        // Check if the touch is within the board
        if (touchX < left || touchX > left + boardSize) return
        if (touchY < top  || touchY > top  + boardSize) return

        val col = ((touchX - left) / cellSize).toInt().coerceIn(0, 2)
        val row = ((touchY - top)  / cellSize).toInt().coerceIn(0, 2)

        // The reducer handles advancing currentPlayer. The input handler can just pass 0
        // because the reducer will ignore the playerId from the input and use state.currentPlayer.id!
        // But to be clean we just use a default or 0.
        onIntent(GameIntent.MakeMove(Move(0, Position(row, col))))
    }
}
