package com.offlinegames.games.dotsandboxes

import com.offlinegames.core.GameIntent
import com.offlinegames.core.Move
import com.offlinegames.core.MoveType
import com.offlinegames.core.Position
import com.offlinegames.engine.InputHandler
import kotlin.math.abs

/**
 * Maps touch coordinates to edge selections in Dots & Boxes.
 *
 * Finds the nearest undrawn edge to the touch point within a threshold.
 */
class DotsAndBoxesInputHandler(
    private val onIntent: (GameIntent) -> Unit
) : InputHandler {

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        val boxCount = DotsAndBoxesRules.BOX_COUNT
        val gridSize = DotsAndBoxesRules.GRID_SIZE
        val maxSize = minOf(surfaceW, surfaceH) * 0.75f
        val spacing = maxSize / boxCount
        val totalW = boxCount * spacing
        val totalH = boxCount * spacing
        val originX = (surfaceW - totalW) / 2f
        val originY = (surfaceH - totalH) / 2f - totalH * 0.05f

        // Convert touch to grid-relative coordinates
        val relX = touchX - originX
        val relY = touchY - originY

        // Find nearest edge
        var bestDist = Float.MAX_VALUE
        var bestR = -1
        var bestC = -1
        val threshold = spacing * 0.4f

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!DotsAndBoxesRules.isEdge(r, c)) continue

                // Edge center in pixel coordinates
                val edgeCx: Float
                val edgeCy: Float
                if (DotsAndBoxesRules.isHorizontalEdge(r, c)) {
                    val dotRow = r / 2
                    val dotCol = (c - 1) / 2
                    edgeCx = dotCol * spacing + spacing / 2f
                    edgeCy = dotRow * spacing
                } else {
                    val dotCol = c / 2
                    val dotRow = (r - 1) / 2
                    edgeCx = dotCol * spacing
                    edgeCy = dotRow * spacing + spacing / 2f
                }

                val dist = abs(relX - edgeCx) + abs(relY - edgeCy)  // Manhattan distance
                if (dist < bestDist && dist < threshold) {
                    bestDist = dist
                    bestR = r
                    bestC = c
                }
            }
        }

        if (bestR >= 0 && bestC >= 0) {
            onIntent(GameIntent.MakeMove(Move(0, Position(bestR, bestC), MoveType.EDGE)))
        }
    }
}
