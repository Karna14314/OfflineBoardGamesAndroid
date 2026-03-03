package com.offlinegames.games.checkers

import com.offlinegames.core.GameAction
import com.offlinegames.core.Move
import com.offlinegames.core.MoveType
import com.offlinegames.core.Position
import com.offlinegames.engine.InputHandler
import kotlin.math.abs

/**
 * Converts touch input into Checkers game actions.
 *
 * Two-phase input:
 * 1. Tap to select piece
 * 2. Tap valid destination to move
 *
 * @param onSelectPiece Callback when a piece is selected
 * @param onMove Callback when a move is made
 * @param onAction Callback for direct actions
 */
class CheckersInputHandler(
    private val onSelectPiece: (Position) -> Unit,
    private val onMove: (from: Position, to: Position, isCapture: Boolean) -> Unit,
    private val onAction: (GameAction) -> Unit
) : InputHandler {

    companion object {
        const val BOARD_SIZE = 8
    }

    private var selectedPosition: Position? = null

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        val position = screenToBoard(touchX, touchY, surfaceW, surfaceH) ?: return

        if (selectedPosition == null) {
            // Select piece
            selectedPosition = position
            onSelectPiece(position)
        } else {
            // Try to move
            val from = selectedPosition!!
            val to = position

            if (from == to) {
                // Deselect
                selectedPosition = null
                onSelectPiece(position) // Will trigger deselect in reducer
                return
            }

            val isCapture = abs(to.row - from.row) == 2

            if (isCapture) {
                val capturedRow = (from.row + to.row) / 2
                val capturedCol = (from.col + to.col) / 2

                val action = GameAction.CaptureAction(
                    move = Move(
                        playerId = 0,
                        position = from,
                        type = MoveType.JUMP,
                        metadata = mapOf("toRow" to to.row, "toCol" to to.col)
                    ),
                    capturedPiece = Position(capturedRow, capturedCol),
                    continueChain = false
                )
                onAction(action)
            } else {
                onMove(from, to, false)
            }

            selectedPosition = null
        }
    }

    fun resetSelection() {
        selectedPosition = null
    }

    /**
     * Convert screen coordinates to board position.
     */
    private fun screenToBoard(
        touchX: Float,
        touchY: Float,
        surfaceW: Float,
        surfaceH: Float
    ): Position? {
        val margin = 80f
        val availableHeight = surfaceH - margin * 2 - 120f
        val boardSize = minOf(surfaceW - margin * 2, availableHeight)
        val left = (surfaceW - boardSize) / 2f
        val top = margin
        val cellSize = boardSize / BOARD_SIZE

        // Check if touch is within board bounds
        if (touchX < left || touchX > left + boardSize ||
            touchY < top || touchY > top + boardSize
        ) {
            return null
        }

        val col = ((touchX - left) / cellSize).toInt()
        val row = ((touchY - top) / cellSize).toInt()

        // Clamp to valid range
        val validRow = row.coerceIn(0, BOARD_SIZE - 1)
        val validCol = col.coerceIn(0, BOARD_SIZE - 1)

        return Position(validRow, validCol)
    }

    private fun min(a: Float, b: Float): Float = if (a < b) a else b
}
