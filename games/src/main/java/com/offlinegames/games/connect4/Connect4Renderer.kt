package com.offlinegames.games.connect4

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.offlinegames.core.GameResult
import com.offlinegames.core.GameState
import com.offlinegames.core.GridBoard
import com.offlinegames.engine.BoardRenderer
import com.offlinegames.engine.PieceRenderer

/**
 * Renders the Connect 4 board and pieces using Canvas.
 *
 * The board is drawn as a blue rectangle with circular holes.
 * Player 1 = red, Player 2 = yellow.
 * Includes a simple falling animation driven by the render loop.
 */
class Connect4Renderer : BoardRenderer, PieceRenderer {

    // -- Paint objects (allocated once, reused every frame) --

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }

    private val boardOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D47A1")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
        style = Paint.Style.FILL
    }

    private val player1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF5350")
        style = Paint.Style.FILL
    }

    private val player2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEE58")
        style = Paint.Style.FILL
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // -- Animation state (render thread only, no allocations per frame) --
    @Volatile var dropCol: Int = -1
    @Volatile var dropTargetRow: Int = -1
    private var dropCurrentY: Float = -1f
    private var dropAnimating: Boolean = false

    fun startDropAnimation(col: Int, targetRow: Int) {
        dropCol = col
        dropTargetRow = targetRow
        dropCurrentY = -1f  // start above board
        dropAnimating = true
    }

    // -- BoardRenderer --

    override fun drawBoard(canvas: Canvas, state: GameState) {
        val (left, top, cellSize) = boardGeometry(canvas)
        val cols = Connect4Rules.COLS
        val rows = Connect4Rules.ROWS

        // Board background
        canvas.drawRoundRect(
            left - 8f, top - 8f,
            left + cols * cellSize + 8f,
            top + rows * cellSize + 8f,
            16f, 16f, boardPaint
        )
    }

    // -- PieceRenderer --

    override fun drawPieces(canvas: Canvas, state: GameState) {
        val board = state.boardData as? GridBoard ?: return
        val (left, top, cellSize) = boardGeometry(canvas)
        val cols = Connect4Rules.COLS
        val rows = Connect4Rules.ROWS
        val radius = cellSize * 0.4f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = left + c * cellSize + cellSize / 2f
                val cy = top + r * cellSize + cellSize / 2f
                val owner = board.get(r, c)

                // Skip the currently animating piece
                if (dropAnimating && c == dropCol && r == dropTargetRow) {
                    // Draw the hole as empty, we'll draw the animated piece separately
                    canvas.drawCircle(cx, cy, radius, emptyPaint)
                    continue
                }

                val paint = when (owner) {
                    1 -> player1Paint
                    2 -> player2Paint
                    else -> emptyPaint
                }
                canvas.drawCircle(cx, cy, radius, paint)
            }
        }

        // Draw falling piece animation
        if (dropAnimating && dropCol >= 0 && dropTargetRow >= 0) {
            val cx = left + dropCol * cellSize + cellSize / 2f
            val targetCy = top + dropTargetRow * cellSize + cellSize / 2f

            if (dropCurrentY < 0) dropCurrentY = top - cellSize

            // Animate falling: move down by a fraction of cellSize each frame
            val speed = cellSize * 0.25f
            dropCurrentY += speed

            if (dropCurrentY >= targetCy) {
                dropCurrentY = targetCy
                dropAnimating = false
            }

            // Determine whose piece is falling (last move's player)
            val lastMove = state.moveHistory.lastOrNull()
            val paint = when (lastMove?.move?.playerId) {
                1 -> player1Paint
                2 -> player2Paint
                else -> player1Paint
            }
            canvas.drawCircle(cx, dropCurrentY, radius, paint)
        }

        // Status text below board
        val boardBottom = top + rows * cellSize
        val statusY = boardBottom + 64f
        val statusText = when (state.result) {
            GameResult.WIN -> "${state.winner()?.name ?: "?"} wins! 🎉"
            GameResult.DRAW -> "It's a draw!"
            GameResult.IN_PROGRESS -> "${state.currentPlayer.name}'s turn"
        }
        canvas.drawText(statusText, canvas.width / 2f, statusY, statusPaint)
    }

    /** Compute board geometry: top-left corner and cell size for 7×6 grid. */
    private fun boardGeometry(canvas: Canvas): Triple<Float, Float, Float> {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val cols = Connect4Rules.COLS
        val rows = Connect4Rules.ROWS

        val maxBoardW = w * 0.90f
        val maxBoardH = h * 0.70f
        val cellSize = minOf(maxBoardW / cols, maxBoardH / rows)
        val boardW = cols * cellSize
        val boardH = rows * cellSize
        val left = (w - boardW) / 2f
        val top = (h - boardH) / 2f - boardH * 0.06f
        return Triple(left, top, cellSize)
    }
}
