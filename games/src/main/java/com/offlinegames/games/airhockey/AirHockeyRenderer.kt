package com.offlinegames.games.airhockey

import android.graphics.*
import com.offlinegames.core.GameState
import com.offlinegames.engine.BoardRenderer
import com.offlinegames.engine.PhysicsWorld
import com.offlinegames.engine.PieceRenderer
import com.offlinegames.engine.CollisionResolver

/**
 * High-performance Air Hockey renderer.
 *
 * Renders at 60 FPS with zero allocations per frame:
 * - All Paint objects pre-allocated
 * - Reusable RectF for drawing
 * - Direct PhysicsWorld reads (no copies)
 *
 * The renderer reads positions directly from [physicsWorld]
 * rather than from GameState, for minimal latency.
 */
class AirHockeyRenderer(
    private val physicsWorld: PhysicsWorld
) : BoardRenderer, PieceRenderer {

    // ── Paint objects (allocated once) ───────────────────────────────────
    private val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }

    private val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val strokeColor = Color.parseColor("#0D47A1")
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 8f
        setShadowLayer(15f, 0f, 0f, strokeColor)
    }

    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val strokeColor = Color.parseColor("#42A5F5")
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 4f
        setShadowLayer(12f, 0f, 0f, strokeColor)
    }

    private val centerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val strokeColor = Color.parseColor("#42A5F5")
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 4f
        setShadowLayer(12f, 0f, 0f, strokeColor)
    }

    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val fillColor = Color.parseColor("#B71C1C")
        color = fillColor
        style = Paint.Style.FILL
        setShadowLayer(15f, 0f, 0f, fillColor)
    }

    private val puckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121")
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }

    private val puckHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val highlightColor = Color.parseColor("#424242")
        color = highlightColor
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 0f, highlightColor)
    }

    private val paddle1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val fillColor = Color.parseColor("#E53935")
        color = fillColor
        style = Paint.Style.FILL
        setShadowLayer(20f, 0f, 0f, fillColor)
    }

    private val paddle2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val fillColor = Color.parseColor("#43A047")
        color = fillColor
        style = Paint.Style.FILL
        setShadowLayer(20f, 0f, 0f, fillColor)
    }

    private val paddleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        setShadowLayer(10f, 0f, 0f, Color.WHITE)
    }

    private val paddleInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF")
        style = Paint.Style.FILL
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 72f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(10f, 0f, 0f, Color.WHITE)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val textColor = Color.parseColor("#90CAF9")
        color = textColor
        textSize = 28f
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 0f, textColor)
    }

    private val goalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val textColor = Color.parseColor("#FFCDD2")
        color = textColor
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(20f, 0f, 0f, textColor)
    }

    // Reusable rect for efficiency
    private val tempRect = RectF()

    // Cached table geometry
    private var lastCanvasWidth: Float = -1f
    private var lastCanvasHeight: Float = -1f
    private var cachedTableLeft: Float = 0f
    private var cachedTableTop: Float = 0f
    private var cachedTableWidth: Float = 0f
    private var cachedTableHeight: Float = 0f

    // Pre-allocated score strings
    private val scoreStrings = Array(100) { it.toString() }

    // Goal animation state
    @Volatile
    var goalFlashAlpha: Float = 0f

    // ── BoardRenderer ───────────────────────────────────────────────────

    override fun drawBoard(canvas: Canvas, state: GameState) {
        updateTableGeometry(canvas)
        val tableLeft = cachedTableLeft
        val tableTop = cachedTableTop
        val tableW = cachedTableWidth
        val tableH = cachedTableHeight

        // Table background
        tempRect.set(tableLeft, tableTop, tableLeft + tableW, tableTop + tableH)
        canvas.drawRoundRect(tempRect, 20f, 20f, tablePaint)
        canvas.drawRoundRect(tempRect, 20f, 20f, tableBorderPaint)

        // Center line
        val centerY = tableTop + tableH / 2f
        canvas.drawLine(tableLeft, centerY, tableLeft + tableW, centerY, centerLinePaint)

        // Center circle
        canvas.drawCircle(tableLeft + tableW / 2f, centerY, tableW * 0.12f, centerCirclePaint)

        // Goals
        val goalW = tableW * CollisionResolver.GOAL_WIDTH
        val goalX = tableLeft + (tableW - goalW) / 2f
        val goalH = 16f

        // Top goal
        tempRect.set(goalX, tableTop - goalH / 2, goalX + goalW, tableTop + goalH / 2)
        canvas.drawRoundRect(tempRect, 8f, 8f, goalPaint)

        // Bottom goal
        tempRect.set(goalX, tableTop + tableH - goalH / 2, goalX + goalW, tableTop + tableH + goalH / 2)
        canvas.drawRoundRect(tempRect, 8f, 8f, goalPaint)

        // Scores
        drawScores(canvas, tableLeft, tableTop, tableW, tableH)
    }

    // ── PieceRenderer ───────────────────────────────────────────────────

    override fun drawPieces(canvas: Canvas, state: GameState) {
        updateTableGeometry(canvas)
        val tableLeft = cachedTableLeft
        val tableTop = cachedTableTop
        val tableW = cachedTableWidth
        val tableH = cachedTableHeight

        // Draw puck
        val puckX = tableLeft + physicsWorld.puck.x * tableW
        val puckY = tableTop + physicsWorld.puck.y * tableH
        val puckR = physicsWorld.puck.radius * tableW

        canvas.drawCircle(puckX, puckY, puckR, puckPaint)
        canvas.drawCircle(puckX - puckR * 0.2f, puckY - puckR * 0.2f, puckR * 0.3f, puckHighlightPaint)

        // Draw paddle 1 (bottom, red)
        drawPaddle(canvas, physicsWorld.paddle1.x, physicsWorld.paddle1.y,
            physicsWorld.paddle1.radius, tableLeft, tableTop, tableW, tableH, paddle1Paint)

        // Draw paddle 2 (top, green)
        drawPaddle(canvas, physicsWorld.paddle2.x, physicsWorld.paddle2.y,
            physicsWorld.paddle2.radius, tableLeft, tableTop, tableW, tableH, paddle2Paint)

        // Goal flash animation
        if (goalFlashAlpha > 0f) {
            goalTextPaint.alpha = (goalFlashAlpha * 255).toInt()
            canvas.drawText("GOAL!", tableLeft + tableW / 2f, tableTop + tableH / 2f, goalTextPaint)
            goalFlashAlpha -= 0.02f
            if (goalFlashAlpha < 0f) goalFlashAlpha = 0f
        }
    }

    private fun drawPaddle(
        canvas: Canvas,
        x: Float, y: Float, radius: Float,
        tableLeft: Float, tableTop: Float, tableW: Float, tableH: Float,
        paint: Paint
    ) {
        val px = tableLeft + x * tableW
        val py = tableTop + y * tableH
        val pr = radius * tableW

        canvas.drawCircle(px, py, pr, paint)
        canvas.drawCircle(px, py, pr, paddleStrokePaint)
        canvas.drawCircle(px, py, pr * 0.5f, paddleInnerPaint)
    }

    private fun getScoreString(score: Int): String {
        return if (score in 0..99) scoreStrings[score] else score.toString()
    }

    private fun drawScores(canvas: Canvas, tableLeft: Float, tableTop: Float, tableW: Float, tableH: Float) {
        val centerX = tableLeft + tableW / 2f

        // Player 2 score (top half)
        scorePaint.textSize = tableW * 0.12f
        canvas.drawText(getScoreString(physicsWorld.score2), centerX, tableTop + tableH * 0.25f, scorePaint)

        // Player 1 score (bottom half)
        canvas.drawText(getScoreString(physicsWorld.score1), centerX, tableTop + tableH * 0.78f, scorePaint)

        // Labels
        labelPaint.textSize = tableW * 0.045f
        canvas.drawText("Player 2", centerX, tableTop + tableH * 0.18f, labelPaint)
        canvas.drawText("Player 1", centerX, tableTop + tableH * 0.85f, labelPaint)
    }

    // ── Geometry ─────────────────────────────────────────────────────────

    private fun updateTableGeometry(canvas: Canvas) {
        val cw = canvas.width.toFloat()
        val ch = canvas.height.toFloat()

        if (cw == lastCanvasWidth && ch == lastCanvasHeight) return

        lastCanvasWidth = cw
        lastCanvasHeight = ch

        val margin = 24f
        cachedTableWidth = cw - margin * 2
        cachedTableHeight = minOf(ch - margin * 2, cachedTableWidth * 1.6f) // Tall table ratio
        cachedTableLeft = margin
        cachedTableTop = (ch - cachedTableHeight) / 2f
    }

    /**
     * Convert screen touch coordinates to normalised table coordinates.
     *
     * @return Pair(normX, normY) in 0.0–1.0, or null if outside table
     */
    fun screenToNormalised(touchX: Float, touchY: Float, canvasW: Float, canvasH: Float): Pair<Float, Float>? {
        val margin = 24f
        val tableW = canvasW - margin * 2
        val tableH = minOf(canvasH - margin * 2, tableW * 1.6f)
        val tableLeft = margin
        val tableTop = (canvasH - tableH) / 2f

        val nx = (touchX - tableLeft) / tableW
        val ny = (touchY - tableTop) / tableH

        if (nx < -0.1f || nx > 1.1f || ny < -0.1f || ny > 1.1f) return null
        return nx.coerceIn(0f, 1f) to ny.coerceIn(0f, 1f)
    }
}
