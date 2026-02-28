package com.offlinegames.games.dotsandboxes

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
 * Renders the Dots & Boxes game:
 * - Dots at grid intersections
 * - Lines for drawn edges
 * - Colored fill for completed boxes
 * - Score and turn display
 */
class DotsAndBoxesRenderer : BoardRenderer, PieceRenderer {

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val edgePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF5350")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val edgePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#42A5F5")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val undrawnEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333355")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val box1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33EF5350")  // semi-transparent red
        style = Paint.Style.FILL
    }

    private val box2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3342A5F5")  // semi-transparent blue
        style = Paint.Style.FILL
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

    private val boxLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun drawBoard(canvas: Canvas, state: GameState) {
        val board = state.boardData as? GridBoard ?: return
        val (originX, originY, spacing) = boardGeometry(canvas)
        val gridSize = DotsAndBoxesRules.GRID_SIZE

        // Draw undrawn edges as faint lines
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (DotsAndBoxesRules.isEdge(r, c) && board.get(r, c) == 0) {
                    drawEdge(canvas, r, c, originX, originY, spacing, undrawnEdgePaint)
                }
            }
        }
    }

    override fun drawPieces(canvas: Canvas, state: GameState) {
        val board = state.boardData as? GridBoard ?: return
        val (originX, originY, spacing) = boardGeometry(canvas)
        val gridSize = DotsAndBoxesRules.GRID_SIZE

        // Draw completed boxes
        for (r in 1 until gridSize step 2) {
            for (c in 1 until gridSize step 2) {
                val owner = board.get(r, c)
                if (owner == 1 || owner == 2) {
                    val paint = if (owner == 1) box1Paint else box2Paint
                    val cx = originX + (c / 2) * spacing
                    val cy = originY + (r / 2) * spacing
                    canvas.drawRect(cx, cy, cx + spacing, cy + spacing, paint)

                    // Box owner label
                    boxLabelPaint.color = if (owner == 1) Color.parseColor("#EF5350") else Color.parseColor("#42A5F5")
                    canvas.drawText(
                        if (owner == 1) "1" else "2",
                        cx + spacing / 2f,
                        cy + spacing / 2f + boxLabelPaint.textSize / 3f,
                        boxLabelPaint
                    )
                }
            }
        }

        // Draw drawn edges
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!DotsAndBoxesRules.isEdge(r, c)) continue
                val value = board.get(r, c)
                if (value == 0) continue
                val paint = if (value == 1) edgePaint1 else edgePaint2
                drawEdge(canvas, r, c, originX, originY, spacing, paint)
            }
        }

        // Draw dots on top
        val dotRadius = spacing * 0.08f
        for (r in 0..DotsAndBoxesRules.BOX_COUNT) {
            for (c in 0..DotsAndBoxesRules.BOX_COUNT) {
                val dx = originX + c * spacing
                val dy = originY + r * spacing
                canvas.drawCircle(dx, dy, dotRadius, dotPaint)
            }
        }

        // Score display
        val scores = state.scores
        val p1 = scores[1] ?: 0
        val p2 = scores[2] ?: 0
        val scoreY = originY - 24f
        canvas.drawText("P1: $p1  |  P2: $p2", canvas.width / 2f, scoreY, scorePaint)

        // Status text
        val boardBottom = originY + DotsAndBoxesRules.BOX_COUNT * spacing
        val statusY = boardBottom + 60f
        val statusText = when (state.result) {
            GameResult.WIN -> {
                val rules = DotsAndBoxesRules()
                val winner = rules.getWinner(state)
                "${winner?.name ?: "?"} wins! 🎉"
            }
            GameResult.DRAW -> "It's a draw!"
            GameResult.IN_PROGRESS -> "${state.currentPlayer.name}'s turn"
        }
        canvas.drawText(statusText, canvas.width / 2f, statusY, statusPaint)
    }

    private fun drawEdge(
        canvas: Canvas, r: Int, c: Int,
        originX: Float, originY: Float, spacing: Float,
        paint: Paint
    ) {
        if (DotsAndBoxesRules.isHorizontalEdge(r, c)) {
            // Horizontal: connects dot (r, c-1) to dot (r, c+1)
            val dotRow = r / 2
            val dotCol1 = (c - 1) / 2
            val dotCol2 = (c + 1) / 2
            val y = originY + dotRow * spacing
            val x1 = originX + dotCol1 * spacing
            val x2 = originX + dotCol2 * spacing
            canvas.drawLine(x1, y, x2, y, paint)
        } else if (DotsAndBoxesRules.isVerticalEdge(r, c)) {
            // Vertical: connects dot (r-1, c) to dot (r+1, c)
            val dotCol = c / 2
            val dotRow1 = (r - 1) / 2
            val dotRow2 = (r + 1) / 2
            val x = originX + dotCol * spacing
            val y1 = originY + dotRow1 * spacing
            val y2 = originY + dotRow2 * spacing
            canvas.drawLine(x, y1, x, y2, paint)
        }
    }

    private fun boardGeometry(canvas: Canvas): Triple<Float, Float, Float> {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val boxCount = DotsAndBoxesRules.BOX_COUNT
        val maxSize = minOf(w, h) * 0.75f
        val spacing = maxSize / boxCount
        val totalW = boxCount * spacing
        val totalH = boxCount * spacing
        val originX = (w - totalW) / 2f
        val originY = (h - totalH) / 2f - totalH * 0.05f
        return Triple(originX, originY, spacing)
    }
}
