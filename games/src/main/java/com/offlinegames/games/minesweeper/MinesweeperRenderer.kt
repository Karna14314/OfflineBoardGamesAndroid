package com.offlinegames.games.minesweeper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.offlinegames.core.CellVisibility
import com.offlinegames.core.GameResult
import com.offlinegames.core.GameState
import com.offlinegames.engine.BoardRenderer
import com.offlinegames.engine.PieceRenderer

/**
 * Renders the Minesweeper board with:
 * - Grid of cells
 * - Hidden, revealed, and flagged states
 * - Mine and number displays
 * - Game over state
 */
class MinesweeperRenderer : BoardRenderer, PieceRenderer {

    private val sharedPath = android.graphics.Path()

    // ── Paint objects (allocated once, reused every frame) ─────────────────

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7B7B7B")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val hiddenCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C0C0C0")
        style = Paint.Style.FILL
    }

    private val revealedCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
    }

    private val flagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF0000")
        style = Paint.Style.FILL
    }

    private val minePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#000000")
        style = Paint.Style.FILL
    }

    private val explodedMinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF0000")
        style = Paint.Style.FILL
    }

    // Number colors
    private val numberPaints = listOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0000FF") }, // 1 - Blue
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#008000") }, // 2 - Green
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF0000") }, // 3 - Red
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#000080") }, // 4 - Dark Blue
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#800000") }, // 5 - Maroon
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#008080") }, // 6 - Teal
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#000000") }, // 7 - Black
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#808080") }  // 8 - Gray
    ).map {
        it.apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // ── BoardRenderer ───────────────────────────────────────────────────────

    override fun drawBoard(canvas: Canvas, state: GameState) {
        val board = state.boardData as? MinesweeperBoard ?: return
        val (boardLeft, boardTop, cellSize) = boardGeometry(canvas, board)

        // Draw grid background
        val boardWidth = cellSize * board.width
        val boardHeight = cellSize * board.height

        canvas.drawRect(
            boardLeft,
            boardTop,
            boardLeft + boardWidth,
            boardTop + boardHeight,
            gridPaint
        )

        // Draw cell backgrounds
        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                val left = boardLeft + c * cellSize
                val top = boardTop + r * cellSize
                val rect = RectF(left, top, left + cellSize, top + cellSize)

                val cell = board.getCell(r, c)
                val paint = if (cell.isRevealed) revealedCellPaint else hiddenCellPaint
                canvas.drawRect(rect, paint)
            }
        }

        // Draw grid lines
        for (i in 0..board.width) {
            val x = boardLeft + i * cellSize
            canvas.drawLine(x, boardTop, x, boardTop + boardHeight, gridPaint)
        }
        for (i in 0..board.height) {
            val y = boardTop + i * cellSize
            canvas.drawLine(boardLeft, y, boardLeft + boardWidth, y, gridPaint)
        }
    }

    // ── PieceRenderer ───────────────────────────────────────────────────────

    override fun drawPieces(canvas: Canvas, state: GameState) {
        val board = state.boardData as? MinesweeperBoard ?: return
        val (boardLeft, boardTop, cellSize) = boardGeometry(canvas, board)
        val textSize = cellSize * 0.7f

        // Set text size for all number paints
        numberPaints.forEach { it.textSize = textSize }

        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                val cell = board.getCell(r, c)
                val cx = boardLeft + c * cellSize + cellSize / 2f
                val cy = boardTop + r * cellSize + cellSize / 2f

                when (cell.visibility) {
                    CellVisibility.HIDDEN -> {
                        // Draw nothing
                    }
                    CellVisibility.FLAGGED -> {
                        drawFlag(canvas, cx, cy, cellSize * 0.4f)
                    }
                    CellVisibility.QUESTIONED -> {
                        // Could draw question mark
                    }
                    CellVisibility.REVEALED -> {
                        if (cell.isMine) {
                            val isExploded = state.result == GameResult.DRAW
                            drawMine(canvas, cx, cy, cellSize * 0.3f, isExploded)
                        } else if (cell.adjacentMines > 0) {
                            drawNumber(canvas, cx, cy, cell.adjacentMines, numberPaints[cell.adjacentMines - 1])
                        }
                    }
                }
            }
        }

        // Draw status
        drawStatus(canvas, state)
    }

    private fun drawFlag(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        // Draw flag pole
        val polePaint = Paint().apply {
            color = Color.parseColor("#000000")
            strokeWidth = 3f
        }
        canvas.drawLine(cx, cy + size, cx, cy - size, polePaint)

        // Draw flag triangle
        sharedPath.reset()
        sharedPath.moveTo(cx, cy - size)
        sharedPath.lineTo(cx + size, cy - size * 0.3f)
        sharedPath.lineTo(cx, cy + size * 0.2f)
        sharedPath.close()
        canvas.drawPath(sharedPath, flagPaint)

        // Draw base
        canvas.drawRect(cx - size * 0.5f, cy + size, cx + size * 0.5f, cy + size + 5f, polePaint)
    }

    private fun drawMine(canvas: Canvas, cx: Float, cy: Float, radius: Float, isExploded: Boolean) {
        val paint = if (isExploded) explodedMinePaint else minePaint

        // Draw mine body
        canvas.drawCircle(cx, cy, radius, paint)

        // Draw spikes
        val spikePaint = Paint().apply {
            color = if (isExploded) Color.parseColor("#800000") else Color.parseColor("#404040")
            strokeWidth = 3f
        }

        for (i in 0 until 8) {
            val angle = i * 45f * kotlin.math.PI.toFloat() / 180f
            val x1 = cx + kotlin.math.cos(angle) * radius * 0.8f
            val y1 = cy + kotlin.math.sin(angle) * radius * 0.8f
            val x2 = cx + kotlin.math.cos(angle) * radius * 1.4f
            val y2 = cy + kotlin.math.sin(angle) * radius * 1.4f
            canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), spikePaint)
        }
    }

    private fun drawNumber(canvas: Canvas, cx: Float, cy: Float, number: Int, paint: Paint) {
        val textY = cy - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(number.toString(), cx, textY, paint)
    }

    private fun drawStatus(canvas: Canvas, state: GameState) {
        val board = state.boardData as? MinesweeperBoard ?: return
        val (_, boardTop, cellSize) = boardGeometry(canvas, board)
        val boardHeight = cellSize * board.height
        val statusY = boardTop + boardHeight + 60f

        val statusText = when (state.result) {
            GameResult.WIN -> "You Win! 🎉"
            GameResult.DRAW -> "Game Over! 💥"
            else -> "Mines: ${board.mineCount - board.flagsPlaced}"
        }

        canvas.drawText(statusText, canvas.width / 2f, statusY, statusPaint)
    }

    /**
     * Compute board geometry for rendering.
     * Returns: (left, top, cellSize)
     */
    private fun boardGeometry(canvas: Canvas, board: MinesweeperBoard): Triple<Float, Float, Float> {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        val margin = 40f
        val statusHeight = 80f
        val availableHeight = h - margin * 2 - statusHeight

        val cellByWidth = (w - margin * 2) / board.width
        val cellByHeight = availableHeight / board.height
        val cellSize = minOf(cellByWidth, cellByHeight)

        val boardWidth = cellSize * board.width
        val boardHeight = cellSize * board.height

        val left = (w - boardWidth) / 2f
        val top = margin

        return Triple(left, top, cellSize)
    }

    private fun min(a: Float, b: Float): Float = if (a < b) a else b
}
