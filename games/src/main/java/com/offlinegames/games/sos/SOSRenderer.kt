package com.offlinegames.games.sos

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
 * Renders the SOS game board: a 5×5 grid with S and O letters,
 * score display, and turn indicator.
 */
class SOSRenderer : BoardRenderer, PieceRenderer {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C83FD")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val sPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
        textSize = 64f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val oPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
        textSize = 64f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 38f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A5D6A7")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // Selected piece indicator
    @Volatile var selectedPieceType: Int = SOSRules.PIECE_S

    override fun drawBoard(canvas: Canvas, state: GameState) {
        val (left, top, cellSize) = boardGeometry(canvas)
        val size = SOSRules.SIZE

        // Grid lines
        for (i in 0..size) {
            val x = left + i * cellSize
            canvas.drawLine(x, top, x, top + size * cellSize, linePaint)
            val y = top + i * cellSize
            canvas.drawLine(left, y, left + size * cellSize, y, linePaint)
        }
    }

    override fun drawPieces(canvas: Canvas, state: GameState) {
        val board = state.boardData as? GridBoard ?: return
        val (left, top, cellSize) = boardGeometry(canvas)
        val size = SOSRules.SIZE

        // Adjust font size to cell
        sPaint.textSize = cellSize * 0.6f
        oPaint.textSize = cellSize * 0.6f

        for (r in 0 until size) {
            for (c in 0 until size) {
                val value = board.get(r, c)
                if (value == 0) continue
                val cx = left + c * cellSize + cellSize / 2f
                val cy = top + r * cellSize + cellSize / 2f

                val paint = if (value == SOSRules.PIECE_S) sPaint else oPaint
                val text = if (value == SOSRules.PIECE_S) "S" else "O"
                // Center text vertically
                val textOffset = (paint.descent() + paint.ascent()) / 2f
                canvas.drawText(text, cx, cy - textOffset, paint)
            }
        }

        // Score display above board
        val scoreY = top - 20f
        val scores = state.scores
        val p1Score = scores[1] ?: 0
        val p2Score = scores[2] ?: 0
        canvas.drawText(
            "P1: $p1Score  |  P2: $p2Score",
            canvas.width / 2f, scoreY, scorePaint
        )

        // Status text below board
        val boardBottom = top + size * cellSize
        val statusY = boardBottom + 56f
        val statusText = when (state.result) {
            GameResult.WIN -> {
                val rules = SOSRules()
                val winner = rules.getWinner(state)
                "${winner?.name ?: "?"} wins! 🎉"
            }
            GameResult.DRAW -> "It's a draw!"
            GameResult.IN_PROGRESS -> {
                val pieceLabel = if (selectedPieceType == SOSRules.PIECE_S) "S" else "O"
                "${state.currentPlayer.name}'s turn — placing $pieceLabel"
            }
        }
        canvas.drawText(statusText, canvas.width / 2f, statusY, statusPaint)

        // Piece toggle indicator
        if (state.result == GameResult.IN_PROGRESS) {
            val toggleY = boardBottom + 110f
            val toggleText = "Tap below to toggle: [S] / [O]"
            statusPaint.textSize = 30f
            canvas.drawText(toggleText, canvas.width / 2f, toggleY, statusPaint)
            statusPaint.textSize = 38f
        }
    }

    private fun boardGeometry(canvas: Canvas): Triple<Float, Float, Float> {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val size = SOSRules.SIZE
        val boardSize = minOf(w, h) * 0.75f
        val cellSize = boardSize / size
        val left = (w - boardSize) / 2f
        val top = (h - boardSize) / 2f - boardSize * 0.05f
        return Triple(left, top, cellSize)
    }
}
