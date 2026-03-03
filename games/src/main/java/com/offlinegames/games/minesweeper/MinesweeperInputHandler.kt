package com.offlinegames.games.minesweeper

import com.offlinegames.core.GameAction
import com.offlinegames.core.Position
import com.offlinegames.engine.InputHandler

/**
 * Converts touch input into Minesweeper game actions.
 *
 * Supports two interaction modes controlled by [flagMode]:
 * - **Dig mode** (default): Tap to reveal, long press to flag
 * - **Flag mode**: Tap to flag, long press to reveal
 *
 * Additionally, tapping on an already-revealed numbered cell triggers
 * a **chord** action that auto-reveals unflagged neighbors when the
 * adjacent flag count matches the cell's number.
 *
 * @param onAction Callback for game actions
 * @param getBoardState Provider for the current board state (needed for chord detection)
 */
class MinesweeperInputHandler(
    private val onAction: (GameAction) -> Unit,
    private val getBoardState: () -> MinesweeperBoard? = { null }
) : InputHandler {

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var startTime: Long = 0L
    private var isLongPress: Boolean = false

    /** When true, single tap = flag; when false, single tap = reveal. */
    var flagMode: Boolean = false

    companion object {
        private const val LONG_PRESS_THRESHOLD = 500L // ms
        private const val MOVE_THRESHOLD = 20f // pixels
    }

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        // This handles ACTION_UP via the gesture system
    }

    /**
     * Called on ACTION_DOWN.
     */
    fun onTouchDown(x: Float, y: Float) {
        startX = x
        startY = y
        startTime = System.currentTimeMillis()
        isLongPress = false
    }

    /**
     * Called on ACTION_MOVE.
     */
    fun onTouchMove(x: Float, y: Float): Boolean {
        val distance = kotlin.math.hypot(x - startX, y - startY)
        return distance > MOVE_THRESHOLD
    }

    /**
     * Called on ACTION_UP.
     */
    fun onTouchUp(
        x: Float,
        y: Float,
        surfaceW: Float,
        surfaceH: Float,
        boardWidth: Int,
        boardHeight: Int
    ) {
        val duration = System.currentTimeMillis() - startTime
        val distance = kotlin.math.hypot(x - startX, y - startY)

        if (distance > MOVE_THRESHOLD) return

        val position = screenToBoard(x, y, surfaceW, surfaceH, boardWidth, boardHeight)
            ?: return

        // Check if tapping an already-revealed cell -> chord action
        val board = getBoardState()
        if (board != null) {
            val cell = board.getCell(position.row, position.col)
            if (cell.isRevealed && cell.adjacentMines > 0 && !cell.isMine) {
                onAction(GameAction.ChordAction(position))
                return
            }
        }

        val isLongTap = duration >= LONG_PRESS_THRESHOLD || isLongPress

        if (flagMode) {
            // Flag mode: tap = flag, long press = reveal
            if (isLongTap) {
                onAction(GameAction.RevealCellsAction(position))
            } else {
                onAction(GameAction.FlagCellAction(position))
            }
        } else {
            // Dig mode: tap = reveal, long press = flag
            if (isLongTap) {
                onAction(GameAction.FlagCellAction(position))
            } else {
                onAction(GameAction.RevealCellsAction(position))
            }
        }
    }

    /**
     * Mark that a long press was detected.
     */
    fun onLongPress() {
        isLongPress = true
    }

    /**
     * Convert screen coordinates to board position.
     */
    private fun screenToBoard(
        touchX: Float,
        touchY: Float,
        surfaceW: Float,
        surfaceH: Float,
        boardWidth: Int,
        boardHeight: Int
    ): Position? {
        val margin = 40f
        val statusHeight = 80f
        val availableHeight = surfaceH - margin * 2 - statusHeight

        val cellByWidth = (surfaceW - margin * 2) / boardWidth
        val cellByHeight = availableHeight / boardHeight
        val cellSize = minOf(cellByWidth, cellByHeight)

        val actualBoardWidth = cellSize * boardWidth
        val actualBoardHeight = cellSize * boardHeight

        val boardLeft = (surfaceW - actualBoardWidth) / 2f
        val boardTop = margin

        // Check if touch is within board bounds
        if (touchX < boardLeft || touchX > boardLeft + actualBoardWidth ||
            touchY < boardTop || touchY > boardTop + actualBoardHeight
        ) {
            return null
        }

        val col = ((touchX - boardLeft) / cellSize).toInt()
        val row = ((touchY - boardTop) / cellSize).toInt()

        return Position(
            row.coerceIn(0, boardHeight - 1),
            col.coerceIn(0, boardWidth - 1)
        )
    }
}
