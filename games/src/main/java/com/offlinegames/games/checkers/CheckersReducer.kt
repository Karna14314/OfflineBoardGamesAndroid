package com.offlinegames.games.checkers

import com.offlinegames.core.*

/**
 * Checkers Reducer using the ActionBased architecture.
 *
 * Handles:
 * - Piece selection and move validation
 * - Forced capture enforcement
 * - Chain capture continuation
 * - King promotion
 */
class CheckersReducer {

    private val rules = CheckersRules()
    private val actionReducer = ActionReducer(rules)

    /**
     * Process [action] against [currentState] and return (newState, effects).
     */
    fun reduce(
        currentState: CheckersState,
        action: GameAction
    ): Pair<CheckersState, List<GameEffect>> {
        return when (action) {
            is GameAction.RestartAction -> handleRestart(currentState)
            is GameAction.UndoAction -> handleUndo(currentState)
            is GameAction.SaveAndExitAction -> handleSaveAndExit(currentState)
            is GameAction.MovePieceAction -> handleMove(currentState, action)
            is GameAction.CaptureAction -> handleCapture(currentState, action)
            else -> currentState to listOf(GameEffect.ShowMessage("Invalid action for Checkers"))
        }
    }

    /**
     * Legacy intent support.
     */
    fun reduce(currentState: CheckersState, intent: GameIntent): Pair<CheckersState, List<GameEffect>> {
        return when (intent) {
            is GameIntent.MakeMove -> {
                val action = GameAction.MovePieceAction(intent.move)
                reduce(currentState, action)
            }
            is GameIntent.RestartGame -> handleRestart(currentState)
            is GameIntent.UndoMove -> handleUndo(currentState)
            is GameIntent.SaveAndExit -> handleSaveAndExit(currentState)
        }
    }

    /**
     * Handle piece selection (for UI highlighting).
     */
    fun selectPiece(currentState: CheckersState, position: Position): CheckersState {
        val board = currentState.board
        val piece = board.getPiece(position.row, position.col)

        // Can only select own pieces
        if (piece == null || piece.playerId != currentState.currentPlayer.id) {
            return currentState.copy(selectedPiece = null, validMoves = emptyList())
        }

        // Get valid moves for this piece
        val validMoves = rules.getValidMovesForPiece(board, piece, currentState.currentPlayer.id)
            .map { it.to }

        return currentState.copy(
            selectedPiece = position,
            validMoves = validMoves
        )
    }

    private fun handleMove(
        currentState: CheckersState,
        action: GameAction.MovePieceAction
    ): Pair<CheckersState, List<GameEffect>> {
        val board = currentState.board

        // Check for forced captures
        if (rules.hasAnyCapture(board, currentState.currentPlayer.id)) {
            return currentState to listOf(GameEffect.ShowMessage("You must capture!"))
        }

        val result = actionReducer.reduce(currentState.gameState, action)
        return processResult(currentState, result, action)
    }

    private fun handleCapture(
        currentState: CheckersState,
        action: GameAction.CaptureAction
    ): Pair<CheckersState, List<GameEffect>> {
        val result = actionReducer.reduce(currentState.gameState, action)

        val newBoard = result.state.boardData as CheckersBoard
        val move = action.move
        val toRow = move.metadata["toRow"] ?: return currentState to emptyList()
        val toCol = move.metadata["toCol"] ?: return currentState to emptyList()

        val movedPiece = newBoard.getPiece(toRow, toCol)
        val canContinue = movedPiece != null && newBoard.canPieceCapture(movedPiece)

        val newState = currentState.copy(
            gameState = result.state,
            selectedPiece = if (canContinue) Position(toRow, toCol) else null,
            validMoves = if (canContinue) {
                newBoard.getCapturePositions(movedPiece!!).map { Position(it.row, it.col) }
            } else emptyList(),
            isChainCapture = canContinue,
            showResultDialog = result.state.result != GameResult.IN_PROGRESS
        )

        return newState to result.effects
    }

    private fun processResult(
        currentState: CheckersState,
        result: ReducerResult,
        action: GameAction
    ): Pair<CheckersState, List<GameEffect>> {
        val newState = currentState.copy(
            gameState = result.state,
            selectedPiece = null,
            validMoves = emptyList(),
            isChainCapture = false,
            showResultDialog = result.state.result != GameResult.IN_PROGRESS
        )

        return newState to result.effects
    }

    private fun handleRestart(currentState: CheckersState): Pair<CheckersState, List<GameEffect>> {
        val freshGame = createInitialGameState(vsAi = currentState.vsAi)
        val freshState = CheckersState(
            gameState = freshGame,
            vsAi = currentState.vsAi,
            selectedPiece = null,
            validMoves = emptyList(),
            isChainCapture = false
        )
        return freshState to emptyList()
    }

    private fun handleUndo(currentState: CheckersState): Pair<CheckersState, List<GameEffect>> {
        // Undo in Checkers is complex due to captures
        // For now, just reset selection
        return currentState.copy(
            selectedPiece = null,
            validMoves = emptyList()
        ) to listOf(GameEffect.ShowMessage("Undo not available in Checkers"))
    }

    private fun handleSaveAndExit(currentState: CheckersState): Pair<CheckersState, List<GameEffect>> {
        return currentState to emptyList()
    }

    companion object {
        fun createInitialGameState(
            vsAi: Boolean = false,
            sessionId: String = java.util.UUID.randomUUID().toString()
        ): GameState {
            val p2 = if (vsAi) Player.AI else Player.PLAYER_TWO
            val players = listOf(Player.PLAYER_ONE, p2)

            val board = CheckersBoard().createInitial()

            return GameState(
                gameId = sessionId,
                players = players,
                currentPlayer = Player.PLAYER_ONE,
                boardData = board,
                result = GameResult.IN_PROGRESS
            )
        }
    }
}
