package com.offlinegames.games.tictactoe

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.offlinegames.core.GameState
import com.offlinegames.core.GridBoard
import com.offlinegames.engine.BoardRenderer
import com.offlinegames.engine.PieceRenderer

/**
 * Draws the TicTacToe board grid — three horizontal and three vertical lines
 * on a padded square area in the centre of the canvas.
 */
class TicTacToeRenderer : BoardRenderer, PieceRenderer {

    // ── Paint objects (allocated once, reused every frame) ─────────────────

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.parseColor("#7C83FD")
        strokeWidth = 6f
        style  = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#7C83FD"))
    }

    private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.parseColor("#FF6B6B")
        strokeWidth = 14f
        style  = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#FF6B6B"))
    }

    private val oPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.parseColor("#96FEFF")
        strokeWidth = 12f
        style  = Paint.Style.STROKE
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#96FEFF"))
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // ── BoardRenderer ───────────────────────────────────────────────────────

    override fun drawBoard(canvas: Canvas, state: GameState) {
        val (left, top, cellSize) = boardGeometry(canvas)

        // Grid lines — draw as 4 lines defining 3 columns / 3 rows
        for (i in 1..2) {
            // Vertical
            val x = left + i * cellSize
            canvas.drawLine(x, top, x, top + 3 * cellSize, linePaint)
            // Horizontal
            val y = top + i * cellSize
            canvas.drawLine(left, y, left + 3 * cellSize, y, linePaint)
        }
    }

    // ── PieceRenderer ───────────────────────────────────────────────────────

    override fun drawPieces(canvas: Canvas, state: GameState) {
        val board = state.boardData as? GridBoard ?: return
        val (left, top, cellSize) = boardGeometry(canvas)
        val padding = cellSize * 0.2f

        for (r in 0..2) {
            for (c in 0..2) {
                val owner = board.get(r, c)
                val cx = left + c * cellSize + cellSize / 2f
                val cy = top  + r * cellSize + cellSize / 2f
                when (owner) {
                    1 -> drawX(canvas, cx, cy, cellSize - padding * 2)
                    2 -> drawO(canvas, cx, cy, (cellSize - padding * 2) / 2f)
                }
            }
        }

        // Status text below board
        val boardBottom = top + 3 * cellSize
        val statusY = boardBottom + 64f
        val statusText = when (state.result) {
            com.offlinegames.core.GameResult.WIN    -> "${state.winner()?.name ?: "?"} wins! 🎉"
            com.offlinegames.core.GameResult.DRAW   -> "It's a draw!"
            com.offlinegames.core.GameResult.IN_PROGRESS -> "${state.currentPlayer.name}'s turn"
        }
        canvas.drawText(statusText, canvas.width / 2f, statusY, statusPaint)
    }

    // ── Drawing helpers ─────────────────────────────────────────────────────

    private fun drawX(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val half = size / 2f
        canvas.drawLine(cx - half, cy - half, cx + half, cy + half, xPaint)
        canvas.drawLine(cx + half, cy - half, cx - half, cy + half, xPaint)
    }

    private fun drawO(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        canvas.drawCircle(cx, cy, radius, oPaint)
    }

    /**
     * Compute the top-left corner and cell size for a square board
     * centred in the canvas with a 10% margin.
     */
    private fun boardGeometry(canvas: Canvas): Triple<Float, Float, Float> {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val size = minOf(w, h) * 0.80f
        val left = (w - size) / 2f
        val top  = (h - size) / 2f - size * 0.08f  // shift slightly up for status text
        val cellSize = size / 3f
        return Triple(left, top, cellSize)
    }
}
