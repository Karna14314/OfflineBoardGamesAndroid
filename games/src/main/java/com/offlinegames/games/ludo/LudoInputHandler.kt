package com.offlinegames.games.ludo

import com.offlinegames.core.GameAction
import com.offlinegames.core.Position
import com.offlinegames.engine.InputHandler

/**
 * Converts touch events into Ludo game actions.
 *
 * Two input zones:
 * 1. Dice area → roll the dice
 * 2. Board tokens → select token to move
 *
 * @param renderer Used for hit testing (screen → grid coordinate mapping)
 * @param onRollDice Callback when dice area is tapped
 * @param onSelectToken Callback when a token position is tapped
 */
class LudoInputHandler(
    private val renderer: LudoRenderer,
    private val onRollDice: () -> Unit,
    private val onSelectToken: (Position) -> Unit
) : InputHandler {

    override fun onTouch(touchX: Float, touchY: Float, surfaceW: Float, surfaceH: Float) {
        // Check dice area first
        if (renderer.isTouchInDiceArea(touchX, touchY, surfaceW, surfaceH)) {
            onRollDice()
            return
        }

        // Check for token selection on the board
        val gridPos = renderer.screenToGrid(touchX, touchY, surfaceW, surfaceH)
        if (gridPos != null) {
            onSelectToken(gridPos)
        }
    }
}
