package com.offlinegames.games.sos

import com.offlinegames.core.GameIntent
import com.offlinegames.core.Move
import com.offlinegames.core.MoveType
import com.offlinegames.core.Position
import com.offlinegames.engine.InputHandler

/**
 * Maps touch coordinates to SOS moves.
 *
 * Upper area: board cell touch → place selected piece type.
 * Lower area (below board): toggle piece type between S and O.
 */
class SOSInputHandler(
    private val onIntent: (GameIntent) -> Unit,
    private val getPieceType: () -> Int,
    private val onTogglePiece: () -> Unit
) : InputHandler {

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        val size = SOSRules.SIZE
        val boardSize = minOf(surfaceW, surfaceH) * 0.75f
        val cellSize = boardSize / size
        val left = (surfaceW - boardSize) / 2f
        val top = (surfaceH - boardSize) / 2f - boardSize * 0.05f
        val boardBottom = top + boardSize

        // Toggle zone: below the board
        if (touchY > boardBottom) {
            onTogglePiece()
            return
        }

        // Board zone
        if (touchX < left || touchX > left + boardSize) return
        if (touchY < top || touchY > boardBottom) return

        val col = ((touchX - left) / cellSize).toInt().coerceIn(0, size - 1)
        val row = ((touchY - top) / cellSize).toInt().coerceIn(0, size - 1)

        val pieceType = getPieceType()
        onIntent(
            GameIntent.MakeMove(
                Move(
                    playerId = 0,
                    position = Position(row, col),
                    type = MoveType.PLACE,
                    metadata = mapOf(SOSRules.META_PIECE_TYPE to pieceType)
                )
            )
        )
    }
}
