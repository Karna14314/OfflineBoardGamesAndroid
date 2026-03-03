package com.offlinegames.games.ludo

import android.graphics.*
import com.offlinegames.core.GameResult
import com.offlinegames.core.GameState
import com.offlinegames.core.Position
import com.offlinegames.engine.BoardRenderer
import com.offlinegames.engine.PieceRenderer

/**
 * Renders the Ludo board with:
 * - 15×15 grid with coloured quadrants
 * - Shared track with safe cell markers
 * - Home columns
 * - Token pieces with player colours
 * - Selection highlighting for movable tokens
 * - Dice display
 */
class LudoRenderer : BoardRenderer, PieceRenderer {

    // ── Player colours ──────────────────────────────────────────────────
    private val playerColors = intArrayOf(
        Color.parseColor("#E53935"),  // Player 0: Red
        Color.parseColor("#43A047"),  // Player 1: Green
        Color.parseColor("#FDD835"),  // Player 2: Yellow
        Color.parseColor("#1E88E5")   // Player 3: Blue
    )

    private val playerColorsDark = intArrayOf(
        Color.parseColor("#B71C1C"),
        Color.parseColor("#1B5E20"),
        Color.parseColor("#F9A825"),
        Color.parseColor("#0D47A1")
    )

    // ── Paints (pre-allocated) ──────────────────────────────────────────
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FAFAFA")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val safeCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.FILL
    }

    private val tokenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val tokenStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C83FD")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val dicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val diceStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val diceDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val playerQuadrantPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // ── Selection state ─────────────────────────────────────────────────
    private var movableTokenIds: List<Int> = emptyList()
    private var diceValue: Int = 1
    private var isDiceRolled: Boolean = false

    fun setMovableTokens(ids: List<Int>) { movableTokenIds = ids }
    fun setDiceState(value: Int, rolled: Boolean) { diceValue = value; isDiceRolled = rolled }

    // ── BoardRenderer ───────────────────────────────────────────────────

    override fun drawBoard(canvas: Canvas, state: GameState) {
        val (boardLeft, boardTop, cellSize) = boardGeometry(canvas)
        val boardSide = cellSize * 15

        // Draw base quadrants
        val activePlayerId = state.currentPlayer.id - 1
        drawQuadrants(canvas, boardLeft, boardTop, boardSide, cellSize, activePlayerId)

        // Draw grid cells
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val left = boardLeft + col * cellSize
                val top = boardTop + row * cellSize
                canvas.drawRect(left, top, left + cellSize, top + cellSize, gridPaint)
            }
        }

        // Draw track cells
        for (i in LudoPath.sharedTrack.indices) {
            val pos = LudoPath.sharedTrack[i]
            val left = boardLeft + pos.col * cellSize
            val top = boardTop + pos.row * cellSize
            val paint = if (i in LudoPath.safeCellIndices) safeCellPaint else trackPaint
            canvas.drawRect(left + 1, top + 1, left + cellSize - 1, top + cellSize - 1, paint)
        }

        // Draw home columns
        val board = state.boardData as? LudoBoard ?: return
        for (p in 0 until board.playerCount) {
            playerQuadrantPaint.color = playerColors[p]
            playerQuadrantPaint.alpha = 100
            for (pos in LudoPath.homeColumns[p]) {
                val left = boardLeft + pos.col * cellSize
                val top = boardTop + pos.row * cellSize
                canvas.drawRect(left + 1, top + 1, left + cellSize - 1, top + cellSize - 1, playerQuadrantPaint)
            }
        }

        // Draw center home area
        playerQuadrantPaint.color = Color.parseColor("#37474F")
        playerQuadrantPaint.alpha = 255
        val cx = boardLeft + 7 * cellSize
        val cy = boardTop + 7 * cellSize
        canvas.drawRect(cx - cellSize * 0.5f, cy - cellSize * 0.5f,
            cx + cellSize * 1.5f, cy + cellSize * 1.5f, playerQuadrantPaint)
    }

    // ── PieceRenderer ───────────────────────────────────────────────────

    override fun drawPieces(canvas: Canvas, state: GameState) {
        val board = state.boardData as? LudoBoard ?: return
        val (boardLeft, boardTop, cellSize) = boardGeometry(canvas)
        val tokenRadius = cellSize * 0.35f

        // Draw tokens
        for (token in board.tokens) {
            val pos = LudoPath.positionForStep(token.playerId, token.step, token.tokenIdx)
            val cx = boardLeft + pos.col * cellSize + cellSize / 2f
            val cy = boardTop + pos.row * cellSize + cellSize / 2f

            // Token fill
            tokenPaint.color = playerColors[token.playerId]
            canvas.drawCircle(cx, cy, tokenRadius, tokenPaint)
            canvas.drawCircle(cx, cy, tokenRadius, tokenStroke)

            // Draw inner marker if home
            if (token.isHome) {
                tokenPaint.color = Color.WHITE
                canvas.drawCircle(cx, cy, tokenRadius * 0.4f, tokenPaint)
            }

            // Highlight movable tokens
            if (token.id in movableTokenIds) {
                canvas.drawCircle(cx, cy, tokenRadius + 6f, highlightPaint)
            }
        }

        // Draw dice
        drawDice(canvas, board, boardLeft, boardTop, cellSize)

        // Draw status
        drawStatus(canvas, state, boardLeft, boardTop, cellSize)
    }

    // ── Dice rendering ──────────────────────────────────────────────────

    private fun drawDice(canvas: Canvas, board: LudoBoard, boardLeft: Float, boardTop: Float, cellSize: Float) {
        val diceSize = cellSize * 1.8f
        val diceCx = boardLeft + 7.5f * cellSize
        val diceCy = boardTop + 15 * cellSize + diceSize * 0.8f

        // Dice background
        val diceRect = RectF(
            diceCx - diceSize / 2, diceCy - diceSize / 2,
            diceCx + diceSize / 2, diceCy + diceSize / 2
        )
        canvas.drawRoundRect(diceRect, 12f, 12f, dicePaint)
        canvas.drawRoundRect(diceRect, 12f, 12f, diceStrokePaint)

        // Dice dots
        val value = diceValue ?: board.diceValue ?: 0
        if (value > 0) {
            drawDiceDots(canvas, diceCx, diceCy, diceSize * 0.7f, value)
        }

        // "TAP TO ROLL" text if dice not yet rolled
        if (!isDiceRolled && !board.diceRolled) {
            textPaint.textSize = cellSize * 0.5f
            textPaint.color = Color.parseColor("#333333")
            canvas.drawText("TAP TO ROLL", diceCx, diceCy + diceSize + cellSize * 0.3f, textPaint)
            textPaint.color = Color.WHITE
        }
    }

    private fun drawDiceDots(canvas: Canvas, cx: Float, cy: Float, area: Float, value: Int) {
        val dotR = area * 0.12f
        val off = area * 0.3f

        when (value) {
            1 -> canvas.drawCircle(cx, cy, dotR, diceDotPaint)
            2 -> {
                canvas.drawCircle(cx - off, cy - off, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy + off, dotR, diceDotPaint)
            }
            3 -> {
                canvas.drawCircle(cx - off, cy - off, dotR, diceDotPaint)
                canvas.drawCircle(cx, cy, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy + off, dotR, diceDotPaint)
            }
            4 -> {
                canvas.drawCircle(cx - off, cy - off, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy - off, dotR, diceDotPaint)
                canvas.drawCircle(cx - off, cy + off, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy + off, dotR, diceDotPaint)
            }
            5 -> {
                canvas.drawCircle(cx - off, cy - off, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy - off, dotR, diceDotPaint)
                canvas.drawCircle(cx, cy, dotR, diceDotPaint)
                canvas.drawCircle(cx - off, cy + off, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy + off, dotR, diceDotPaint)
            }
            6 -> {
                canvas.drawCircle(cx - off, cy - off, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy - off, dotR, diceDotPaint)
                canvas.drawCircle(cx - off, cy, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy, dotR, diceDotPaint)
                canvas.drawCircle(cx - off, cy + off, dotR, diceDotPaint)
                canvas.drawCircle(cx + off, cy + off, dotR, diceDotPaint)
            }
        }
    }

    // ── Quadrant backgrounds ────────────────────────────────────────────

    private fun drawQuadrants(canvas: Canvas, boardLeft: Float, boardTop: Float, boardSide: Float, cellSize: Float, activePlayerId: Int = -1) {
        // Each quadrant is 6×6 cells in the corners
        val quadrantSize = cellSize * 6

        val quadrants = arrayOf(
            RectF(boardLeft, boardTop, boardLeft + quadrantSize, boardTop + quadrantSize),                                   // Top-left (Red)
            RectF(boardLeft, boardTop + boardSide - quadrantSize, boardLeft + quadrantSize, boardTop + boardSide),            // Bottom-left (Green)
            RectF(boardLeft + boardSide - quadrantSize, boardTop + boardSide - quadrantSize, boardLeft + boardSide, boardTop + boardSide), // Bottom-right (Yellow)
            RectF(boardLeft + boardSide - quadrantSize, boardTop, boardLeft + boardSide, boardTop + quadrantSize)             // Top-right (Blue)
        )

        for (i in quadrants.indices) {
            playerQuadrantPaint.color = playerColorsDark[i]
            
            // Highlight the active player's quadrant
            if (i == activePlayerId) {
                playerQuadrantPaint.alpha = 180 // Brighter
                canvas.drawRect(quadrants[i], playerQuadrantPaint)
                
                // Draw a bright colored border stroke
                highlightPaint.color = playerColors[i]
                highlightPaint.strokeWidth = 8f
                canvas.drawRect(quadrants[i], highlightPaint)
                
                // Reset highlight paint for tokens
                highlightPaint.color = Color.parseColor("#7C83FD")
                highlightPaint.strokeWidth = 5f
            } else {
                playerQuadrantPaint.alpha = 80 // Dimmer
                canvas.drawRect(quadrants[i], playerQuadrantPaint)
            }
        }
        playerQuadrantPaint.alpha = 255
    }

    // ── Status text ─────────────────────────────────────────────────────

    private fun drawStatus(canvas: Canvas, state: GameState, boardLeft: Float, boardTop: Float, cellSize: Float) {
        val playerId = state.currentPlayer.id - 1
        val boardSide = cellSize * 15

        val statusText = when (state.result) {
            GameResult.WIN -> "${state.winner()?.name ?: "?"} wins!"
            GameResult.DRAW -> "It's a draw!"
            GameResult.IN_PROGRESS -> "▶  ${state.currentPlayer.name}'s turn  ◀"
        }

        // Draw high-contrast banner background
        val bannerH = cellSize * 1.2f
        val bannerTop = boardTop - bannerH - cellSize * 0.3f
        val bannerBottom = bannerTop + bannerH

        if (playerId in playerColors.indices) {
            playerQuadrantPaint.color = playerColors[playerId]
            playerQuadrantPaint.alpha = 220
            canvas.drawRoundRect(
                RectF(boardLeft, bannerTop, boardLeft + boardSide, bannerBottom),
                16f, 16f, playerQuadrantPaint
            )
            playerQuadrantPaint.alpha = 255
        }

        // Draw status text centered on banner
        textPaint.textSize = cellSize * 0.75f
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.DEFAULT_BOLD
        val textY = bannerTop + bannerH * 0.7f
        canvas.drawText(statusText, boardLeft + boardSide / 2f, textY, textPaint)
    }

    // ── Geometry ─────────────────────────────────────────────────────────

    private fun boardGeometry(canvas: Canvas): Triple<Float, Float, Float> {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        val margin = 40f
        val topReserve = 80f  // For status text
        val bottomReserve = 160f  // For dice
        val availableH = h - topReserve - bottomReserve - margin * 2

        val boardSide = minOf(w - margin * 2, availableH)
        val left = (w - boardSide) / 2f
        val top = topReserve + margin
        val cellSize = boardSide / 15f

        return Triple(left, top, cellSize)
    }

    /**
     * Convert screen coordinates to a logical grid position.
     */
    fun screenToGrid(touchX: Float, touchY: Float, canvasW: Float, canvasH: Float): Position? {
        val margin = 40f
        val topReserve = 80f
        val bottomReserve = 160f
        val availableH = canvasH - topReserve - bottomReserve - margin * 2
        val boardSide = minOf(canvasW - margin * 2, availableH)
        val left = (canvasW - boardSide) / 2f
        val top = topReserve + margin
        val cellSize = boardSide / 15f

        val col = ((touchX - left) / cellSize).toInt()
        val row = ((touchY - top) / cellSize).toInt()

        if (row in 0 until 15 && col in 0 until 15) {
            return Position(row, col)
        }
        return null
    }

    /**
     * Check if a touch is in the dice area.
     */
    fun isTouchInDiceArea(touchX: Float, touchY: Float, canvasW: Float, canvasH: Float): Boolean {
        val margin = 40f
        val topReserve = 80f
        val bottomReserve = 160f
        val availableH = canvasH - topReserve - bottomReserve - margin * 2
        val boardSide = minOf(canvasW - margin * 2, availableH)
        val left = (canvasW - boardSide) / 2f
        val top = topReserve + margin
        val cellSize = boardSide / 15f

        val diceSize = cellSize * 1.8f
        val diceCx = left + 7.5f * cellSize
        val diceCy = top + 15 * cellSize + diceSize * 0.8f

        val dx = touchX - diceCx
        val dy = touchY - diceCy
        return dx * dx + dy * dy < diceSize * diceSize
    }
}
