package com.offlinegames.games.ludo

import com.offlinegames.core.*

/**
 * Ludo rules using the ActionBased architecture.
 *
 * Features:
 * - Dice roll (1–6)
 * - Token enters board only on rolling 6
 * - Token movement along fixed path
 * - Extra turn on rolling 6
 * - Safe cells protect from capture
 * - Capture sends opponent token back to base
 * - Exact roll required to reach home
 * - Game ends when one player brings all 4 tokens home
 */
class LudoRules : ActionBasedRules<LudoBoard> {

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy GameRules (minimal — most logic is in ActionBased methods)
    // ═══════════════════════════════════════════════════════════════════════

    override fun isValidMove(state: GameState, move: Move): Boolean = false
    override fun getLegalMoves(state: GameState, player: Player): List<Move> = emptyList()

    override fun applyMove(boardData: LudoBoard, move: Move, player: Player): LudoBoard = boardData

    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as LudoBoard
        if (board.winnerId >= 0) return GameResult.WIN
        // Check if any player has all tokens home
        for (p in 0 until board.playerCount) {
            if (board.allTokensHome(p)) return GameResult.WIN
        }
        return GameResult.IN_PROGRESS
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ActionBasedRules
    // ═══════════════════════════════════════════════════════════════════════

    override fun isValidAction(state: GameState, action: GameAction): Boolean {
        val board = state.boardData as LudoBoard
        return when (action) {
            is GameAction.DiceRollAction -> !board.diceRolled && board.winnerId < 0
            is GameAction.TokenMoveAction -> isValidTokenMove(board, action)
            is GameAction.PassTurnAction -> board.diceRolled && !board.hasMovableTokens(action.playerId, board.diceValue ?: 0)
            is GameAction.RestartAction -> true
            is GameAction.SaveAndExitAction -> true
            else -> false
        }
    }

    override fun applyAction(
        boardData: LudoBoard,
        action: GameAction,
        player: Player
    ): ActionResult<LudoBoard> {
        return when (action) {
            is GameAction.DiceRollAction -> applyDiceRoll(boardData, action, player)
            is GameAction.TokenMoveAction -> applyTokenMove(boardData, action, player)
            is GameAction.PassTurnAction -> {
                val passBoard = boardData.copy(
                    diceRolled = false,
                    diceValue = null,
                    extraTurn = false
                )
                ActionResult(newBoardData = passBoard)
            }
            else -> ActionResult(boardData)
        }
    }

    override fun shouldContinueTurn(state: GameState, lastAction: GameAction): Boolean {
        if (lastAction is GameAction.DiceRollAction) return true
        if (lastAction is GameAction.PassTurnAction) return false
        val board = state.boardData as LudoBoard
        return board.extraTurn
    }

    override fun getValidActions(state: GameState, player: Player): List<GameAction> {
        val board = state.boardData as LudoBoard
        val actions = mutableListOf<GameAction>()

        if (!board.diceRolled) {
            // Can roll dice
            // (actual roll is random — this just indicates the action is available)
            return listOf(GameAction.DiceRollAction(1)) // placeholder
        }

        val diceValue = board.diceValue ?: return emptyList()
        val playerId = player.id - 1  // Convert 1-based to 0-based

        // Find movable tokens
        val playerTokens = board.getPlayerTokens(playerId)
        for (token in playerTokens) {
            if (board.canMoveToken(token, diceValue)) {
                actions.add(GameAction.TokenMoveAction(
                    tokenId = token.id,
                    playerId = playerId,
                    steps = diceValue
                ))
            }
        }

        return actions
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Ludo-specific logic
    // ═══════════════════════════════════════════════════════════════════════

    private fun isValidTokenMove(board: LudoBoard, action: GameAction.TokenMoveAction): Boolean {
        if (!board.diceRolled) return false
        val dice = board.diceValue ?: return false
        if (action.steps != dice) return false

        val token = board.getToken(action.tokenId) ?: return false
        if (token.playerId != action.playerId) return false

        return board.canMoveToken(token, dice)
    }

    private fun applyDiceRoll(
        board: LudoBoard,
        action: GameAction.DiceRollAction,
        player: Player
    ): ActionResult<LudoBoard> {
        val diceValue = action.result.coerceIn(1, 6)
        val playerId = player.id - 1

        val newBoard = board.copy(
            diceValue = diceValue,
            diceRolled = true,
            extraTurn = diceValue == 6
        )

        return ActionResult(newBoardData = newBoard)
    }

    private fun applyTokenMove(
        board: LudoBoard,
        action: GameAction.TokenMoveAction,
        player: Player
    ): ActionResult<LudoBoard> {
        val token = board.getToken(action.tokenId) ?: return ActionResult(board)
        val diceValue = board.diceValue ?: return ActionResult(board)

        var newBoard = board
        var capture = false

        if (token.isAtBase && diceValue == 6) {
            // Enter the board
            val enteredToken = token.enterBoard()
            newBoard = newBoard.updateToken(token.id) { enteredToken }

            // Check for capture at entry position
            val entryTrackIdx = LudoPath.playerEntryIndex[token.playerId]
            val victimToken = newBoard.getTokenAtAbsoluteIndex(entryTrackIdx, token.playerId)
            if (victimToken != null && victimToken.canBeCaptured()) {
                newBoard = newBoard.updateToken(victimToken.id) { it.sendToBase() }
                capture = true
            }
        } else {
            // Move along path
            val movedToken = token.advance(diceValue)
            newBoard = newBoard.updateToken(token.id) { movedToken }

            // Check for capture at destination (only on shared track)
            if (movedToken.isOnTrack && !LudoPath.isSafeCell(movedToken.playerId, movedToken.step)) {
                val destTrackIdx = movedToken.absoluteTrackIndex()
                val victimToken = newBoard.getTokenAtAbsoluteIndex(destTrackIdx, movedToken.playerId)
                if (victimToken != null && victimToken.canBeCaptured()) {
                    newBoard = newBoard.updateToken(victimToken.id) { it.sendToBase() }
                    capture = true
                }
            }
        }

        // Check for win
        val winnerId = if (newBoard.allTokensHome(token.playerId)) token.playerId else -1
        
        // Grant extra turn on reaching home or rolling a 6 or capturing
        val reachedHome = wouldReachHome(board, token, diceValue)
        val shouldGrantExtra = board.extraTurn || capture || reachedHome

        // Reset dice state for next action
        val extraTurn = shouldGrantExtra && winnerId < 0
        newBoard = newBoard.copy(
            diceRolled = false,
            diceValue = null,
            extraTurn = extraTurn,
            winnerId = winnerId
        )

        return ActionResult(
            newBoardData = newBoard,
            gameEnded = winnerId >= 0,
            moveRecord = MoveRecord(
                Move(
                    playerId = player.id,
                    position = Position(action.tokenId, diceValue),
                    type = MoveType.PLACE
                )
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Public helpers for AI and UI
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get all movable token IDs for a player given the current dice value.
     */
    fun getMovableTokenIds(board: LudoBoard, playerId: Int): List<Int> {
        val dice = board.diceValue ?: return emptyList()
        return board.getPlayerTokens(playerId)
            .filter { board.canMoveToken(it, dice) }
            .map { it.id }
    }

    /**
     * Check if moving a token would result in a capture.
     */
    fun wouldCapture(board: LudoBoard, token: LudoToken, diceValue: Int): Boolean {
        if (token.isAtBase && diceValue == 6) {
            val entryIdx = LudoPath.playerEntryIndex[token.playerId]
            return board.getTokenAtAbsoluteIndex(entryIdx, token.playerId) != null
        }
        if (token.isHome) return false

        val newStep = token.step + diceValue
        if (newStep >= LudoPath.TRACK_LENGTH) return false // In home column, no captures

        val newTrackIdx = LudoPath.trackIndexForStep(token.playerId, newStep)
        if (LudoPath.safeCellIndices.contains(newTrackIdx)) return false

        return board.getTokenAtAbsoluteIndex(newTrackIdx, token.playerId) != null
    }

    /**
     * Check if moving a token would bring it home.
     */
    fun wouldReachHome(board: LudoBoard, token: LudoToken, diceValue: Int): Boolean {
        if (token.isAtBase || token.isHome) return false
        return token.step + diceValue == LudoPath.AT_HOME
    }
}
