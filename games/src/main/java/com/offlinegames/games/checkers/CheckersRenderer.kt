package com.offlinegames.games.checkers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.offlinegames.core.GameResult
import com.offlinegames.core.GameState
import com.offlinegames.engine.BoardRenderer
import com.offlinegames.engine.PieceRenderer

/**
 * Renders the Checkers board with:
 * - 8x8 checkerboard pattern
 * - Pieces (men and kings)
 * - Selection highlighting
 * - Valid move indicators
 * - Captured piece display
 */
class CheckersRenderer : BoardRenderer, PieceRenderer {

    // ── Paint objects (allocated once, reused every frame) ─────────────────

    private val lightSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2A35") // Darker background
        style = Paint.Style.FILL
    }

    private val darkSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A24") // Even darker background
        style = Paint.Style.FILL
    }

    private val player1PiecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4081") // Neon pink
        style = Paint.Style.FILL
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#FF4081"))
    }

    private val player1PieceStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF") // White outline
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val player2PiecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // Neon blue
        style = Paint.Style.FILL
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#00E5FF"))
    }

    private val player2PieceStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF") // White outline
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val kingCrownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700") // Gold crown
        style = Paint.Style.FILL
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#FFD700"))
    }

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEA00") // Bright yellow
        style = Paint.Style.STROKE
        strokeWidth = 6f
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#FFEA00"))
    }

    private val validMovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // Bright green
        style = Paint.Style.FILL
        alpha = 128
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#00E676"))
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // State for highlighting
    private var selectedPosition: com.offlinegames.core.Position? = null
    private var validMoves: List<com.offlinegames.core.Position> = emptyList()

    // ── BoardRenderer ───────────────────────────────────────────────────────

    override fun drawBoard(canvas: Canvas, state: GameState) {
        val (boardLeft, boardTop, cellSize) = boardGeometry(canvas)

        // Draw checkerboard pattern
        for (row in 0 until CheckersBoard.SIZE) {
            for (col in 0 until CheckersBoard.SIZE) {
                val left = boardLeft + col * cellSize
                val top = boardTop + row * cellSize
                val rect = RectF(left, top, left + cellSize, top + cellSize)

                val paint = if ((row + col) % 2 == 0) lightSquarePaint else darkSquarePaint
                canvas.drawRect(rect, paint)
            }
        }
    }

    // ── PieceRenderer ───────────────────────────────────────────────────────

    override fun drawPieces(canvas: Canvas, state: GameState) {
        val board = state.boardData as? CheckersBoard ?: return
        val (boardLeft, boardTop, cellSize) = boardGeometry(canvas)
        val pieceRadius = cellSize * 0.4f

        // Draw valid move indicators first (under pieces)
        for (move in validMoves) {
            val cx = boardLeft + move.col * cellSize + cellSize / 2f
            val cy = boardTop + move.row * cellSize + cellSize / 2f
            canvas.drawCircle(cx, cy, cellSize * 0.15f, validMovePaint)
        }

        // Draw pieces
        for (piece in board.pieces) {
            val cx = boardLeft + piece.col * cellSize + cellSize / 2f
            val cy = boardTop + piece.row * cellSize + cellSize / 2f

            // Determine piece colors
            val (fillPaint, strokePaint) = when (piece.playerId) {
                1 -> Pair(player1PiecePaint, player1PieceStroke)
                else -> Pair(player2PiecePaint, player2PieceStroke)
            }

            // Draw piece body
            canvas.drawCircle(cx, cy, pieceRadius, fillPaint)
            canvas.drawCircle(cx, cy, pieceRadius, strokePaint)

            // Draw selection highlight
            if (selectedPosition?.row == piece.row && selectedPosition?.col == piece.col) {
                canvas.drawCircle(cx, cy, pieceRadius + 8f, selectionPaint)
            }

            // Draw king crown
            if (piece.isKing) {
                drawCrown(canvas, cx, cy, pieceRadius * 0.5f)
            }
        }

        // Draw status
        drawStatus(canvas, state)
    }

    /**
     * Update selection state for highlighting.
     */
    fun setSelection(position: com.offlinegames.core.Position?, moves: List<com.offlinegames.core.Position> = emptyList()) {
        selectedPosition = position
        validMoves = moves
    }

    private fun drawCrown(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        // Draw a simple crown shape (triangle with rounded top)
        val path = android.graphics.Path()
        val halfSize = size * 0.6f

        path.moveTo(cx - halfSize, cy + size * 0.3f)
        path.lineTo(cx - halfSize * 0.5f, cy - size * 0.3f)
        path.lineTo(cx, cy + size * 0.1f)
        path.lineTo(cx + halfSize * 0.5f, cy - size * 0.3f)
        path.lineTo(cx + halfSize, cy + size * 0.3f)
        path.close()

        canvas.drawPath(path, kingCrownPaint)
    }

    private fun drawStatus(canvas: Canvas, state: GameState) {
        val (_, boardTop, cellSize) = boardGeometry(canvas)
        val boardSize = cellSize * CheckersBoard.SIZE
        val statusY = boardTop + boardSize + 60f

        val statusText = when (state.result) {
            GameResult.WIN -> "${state.winner()?.name ?: "?"} wins!"
            GameResult.DRAW -> "It's a draw!"
            GameResult.IN_PROGRESS -> "${state.currentPlayer.name}'s turn"
        }

        canvas.drawText(statusText, canvas.width / 2f, statusY, statusPaint)

        // Draw piece counts
        val board = state.boardData as? CheckersBoard ?: return
        val p1Count = board.countPieces(1)
        val p2Count = board.countPieces(2)

        val countText = "White: $p1Count  Black: $p2Count"
        canvas.drawText(countText, canvas.width / 2f, statusY + 50f, statusPaint)
    }

    /**
     * Compute board geometry for rendering.
     * Returns: (left, top, cellSize)
     */
    private fun boardGeometry(canvas: Canvas): Triple<Float, Float, Float> {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        val margin = 80f
        val availableHeight = h - margin * 2 - 120f // Leave room for status

        val boardSize = minOf(w - margin * 2, availableHeight)
        val left = (w - boardSize) / 2f
        val top = margin
        val cellSize = boardSize / CheckersBoard.SIZE

        return Triple(left, top, cellSize)
    }

    private fun min(a: Float, b: Float): Float = if (a < b) a else b
}
