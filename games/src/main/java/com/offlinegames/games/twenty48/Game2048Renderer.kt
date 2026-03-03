package com.offlinegames.games.twenty48

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.offlinegames.core.GameState
import com.offlinegames.engine.BoardRenderer
import com.offlinegames.engine.PieceRenderer

/**
 * Renders the 2048 game board with:
 * - Grid background and cells
 * - Tiles with rounded corners and appropriate colors
 * - Score display
 * - Sliding animations via interpolation
 *
 * No Compose animations - all rendering is done on Canvas.
 */
class Game2048Renderer : BoardRenderer, PieceRenderer {

    // ── Paint objects (allocated once, reused every frame) ─────────────────

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BBADA0")
        style = Paint.Style.FILL
    }

    private val emptyCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CDC1B4")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#776E65")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val lightTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F9F6F2")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#776E65")
        textSize = 48f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
    }

    private val gameOverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EDC22E")
        textSize = 72f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // Tile colors by value (background, text)
    private val tileColors = mapOf(
        0 to Pair("#CDC1B4", "#776E65"),
        2 to Pair("#EEE4DA", "#776E65"),
        4 to Pair("#EDE0C8", "#776E65"),
        8 to Pair("#F2B179", "#F9F6F2"),
        16 to Pair("#F59563", "#F9F6F2"),
        32 to Pair("#F67C5F", "#F9F6F2"),
        64 to Pair("#F65E3B", "#F9F6F2"),
        128 to Pair("#EDCF72", "#F9F6F2"),
        256 to Pair("#EDCC61", "#F9F6F2"),
        512 to Pair("#EDC850", "#F9F6F2"),
        1024 to Pair("#EDC53F", "#F9F6F2"),
        2048 to Pair("#EDC22E", "#F9F6F2"),
        4096 to Pair("#3C3A32", "#F9F6F2"),
        8192 to Pair("#3C3A32", "#F9F6F2")
    )

    // Animation state (no allocations during render loop)
    private var animProgress: Float = 1.0f
    private var previousBoard: Game2048Board? = null
    private var animationStartTime: Long = 0
    private val animationDuration = 150L // ms

    // ── BoardRenderer ───────────────────────────────────────────────────────

    override fun drawBoard(canvas: Canvas, state: GameState) {
        val (boardLeft, boardTop, cellSize, padding) = boardGeometry(canvas)
        val boardSize = cellSize * 4 + padding * 5

        // Draw background
        val bgRect = RectF(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize)
        canvas.drawRoundRect(bgRect, 12f, 12f, backgroundPaint)

        // Draw empty cells
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val cellLeft = boardLeft + padding + c * (cellSize + padding)
                val cellTop = boardTop + padding + r * (cellSize + padding)
                val cellRect = RectF(cellLeft, cellTop, cellLeft + cellSize, cellTop + cellSize)
                canvas.drawRoundRect(cellRect, 8f, 8f, emptyCellPaint)
            }
        }
    }

    // ── PieceRenderer ───────────────────────────────────────────────────────

    override fun drawPieces(canvas: Canvas, state: GameState) {
        val board = state.boardData as? Game2048Board ?: return
        val (boardLeft, boardTop, cellSize, padding) = boardGeometry(canvas)

        // Update animation progress
        updateAnimationProgress()

        // Draw score
        drawScore(canvas, board.score)

        // Draw tiles
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val value = board.get(r, c)
                if (value == 0) continue

                val cellLeft = boardLeft + padding + c * (cellSize + padding)
                val cellTop = boardTop + padding + r * (cellSize + padding)

                drawTile(canvas, cellLeft, cellTop, cellSize, value)
            }
        }

        // Draw game over overlay if applicable
        if (state.result != com.offlinegames.core.GameResult.IN_PROGRESS) {
            drawGameOverOverlay(canvas, state)
        }
    }

    /**
     * Called to trigger a new animation when the board changes.
     */
    fun onBoardChanged(oldBoard: Game2048Board?, newBoard: Game2048Board) {
        if (oldBoard != null && oldBoard != newBoard) {
            previousBoard = oldBoard
            animationStartTime = System.currentTimeMillis()
            animProgress = 0.0f
        }
    }

    private fun updateAnimationProgress() {
        if (animProgress < 1.0f) {
            val elapsed = System.currentTimeMillis() - animationStartTime
            animProgress = (elapsed / animationDuration.toFloat()).coerceIn(0.0f, 1.0f)
        }
    }

    private fun drawTile(canvas: Canvas, left: Float, top: Float, size: Float, value: Int) {
        val colors = tileColors[value] ?: Pair("#3C3A32", "#F9F6F2")
        val bgColor = Color.parseColor(colors.first)
        val textColor = if (value <= 4) Color.parseColor("#776E65") else Color.parseColor("#F9F6F2")

        // Tile background
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }

        val tileRect = RectF(left, top, left + size, top + size)
        canvas.drawRoundRect(tileRect, 8f, 8f, tilePaint)

        // Tile text
        val textPaintToUse = if (value <= 4) textPaint else lightTextPaint
        textPaintToUse.textSize = calculateTextSize(value, size)

        val textY = top + size / 2f - (textPaintToUse.descent() + textPaintToUse.ascent()) / 2f
        canvas.drawText(value.toString(), left + size / 2f, textY, textPaintToUse)
    }

    private fun calculateTextSize(value: Int, cellSize: Float): Float {
        return when {
            value < 100 -> cellSize * 0.55f
            value < 1000 -> cellSize * 0.45f
            value < 10000 -> cellSize * 0.35f
            else -> cellSize * 0.3f
        }
    }

    private fun drawScore(canvas: Canvas, score: Int) {
        val margin = 48f
        canvas.drawText("Score: $score", margin, margin, scorePaint)
    }

    private fun drawGameOverOverlay(canvas: Canvas, state: GameState) {
        val overlayPaint = Paint().apply {
            color = Color.parseColor("#80FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlayPaint)

        val message = when (state.result) {
            com.offlinegames.core.GameResult.WIN -> "You Win!"
            com.offlinegames.core.GameResult.DRAW -> "Game Over!"
            else -> ""
        }

        if (message.isNotEmpty()) {
            canvas.drawText(
                message,
                canvas.width / 2f,
                canvas.height / 2f,
                gameOverPaint
            )
        }
    }

    /**
     * Compute board geometry for rendering.
     * Returns: (left, top, cellSize, padding)
     */
    private fun boardGeometry(canvas: Canvas): Quad<Float, Float, Float, Float> {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        // Leave room for score at top
        val scoreHeight = 120f
        val availableHeight = h - scoreHeight - 80f

        val boardSize = minOf(w * 0.9f, availableHeight)
        val left = (w - boardSize) / 2f
        val top = scoreHeight

        val padding = boardSize * 0.02f
        val cellSize = (boardSize - padding * 5) / 4f

        return Quad(left, top, cellSize, padding)
    }

    /**
     * Simple 4-tuple for geometry values.
     */
    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
