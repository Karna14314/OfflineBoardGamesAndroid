package com.offlinegames.games.twenty48

import com.offlinegames.core.*

/**
 * 2048 Reducer using the ActionBased architecture.
 *
 * Handles:
 * - Swipe actions (MergeTilesAction)
 * - Undo (single move)
 * - Restart
 * - Score tracking
 */
class Game2048Reducer {

    private val rules = Game2048Rules()
    private val actionReducer = ActionReducer(rules)

    /**
     * Process [action] against [currentState] and return (newState, effects).
     */
    fun reduce(
        currentState: Game2048State,
        action: GameAction
    ): Pair<Game2048State, List<GameEffect>> {
        return when (action) {
            is GameAction.RestartAction -> handleRestart(currentState)
            is GameAction.UndoAction -> handleUndo(currentState)
            is GameAction.SaveAndExitAction -> handleSaveAndExit(currentState)
            is GameAction.MergeTilesAction -> handleSwipe(currentState, action)
            else -> currentState to listOf(GameEffect.ShowMessage("Invalid action for 2048"))
        }
    }

    /**
     * Legacy intent support for backward compatibility.
     */
    fun reduce(currentState: Game2048State, intent: GameIntent): Pair<Game2048State, List<GameEffect>> {
        return when (intent) {
            is GameIntent.MakeMove -> {
                // 2048 doesn't use traditional moves
                currentState to emptyList()
            }
            is GameIntent.RestartGame -> handleRestart(currentState)
            is GameIntent.UndoMove -> handleUndo(currentState)
            is GameIntent.SaveAndExit -> handleSaveAndExit(currentState)
        }
    }

    private fun handleSwipe(
        currentState: Game2048State,
        action: GameAction.MergeTilesAction
    ): Pair<Game2048State, List<GameEffect>> {
        // Save current state for undo (only keep last move)
        val snapshot = Game2048Snapshot(currentState.board.copy(), currentState.moveCount)
        val newUndoStack = if (currentState.undoStack.size >= currentState.maxUndoDepth) {
            listOf(snapshot)
        } else {
            currentState.undoStack + snapshot
        }

        // Apply the swipe action
        val result = actionReducer.reduce(currentState.gameState, action)

        // Check for win/loss
        val showDialog = result.state.result != GameResult.IN_PROGRESS

        val newState = currentState.copy(
            gameState = result.state,
            showResultDialog = showDialog,
            undoStack = newUndoStack
        )

        val effects = result.effects.toMutableList()

        // Add score change effect if significant
        val scoreDelta = (result.state.boardData as Game2048Board).score - currentState.board.score
        if (scoreDelta > 0) {
            // Could add a score pop effect here
        }

        return newState to effects
    }

    private fun handleUndo(
        currentState: Game2048State
    ): Pair<Game2048State, List<GameEffect>> {
        if (currentState.undoStack.isEmpty()) {
            return currentState to listOf(GameEffect.ShowMessage("Nothing to undo"))
        }

        val lastSnapshot = currentState.undoStack.last()
        val newUndoStack = currentState.undoStack.dropLast(1)

        // Restore the board from snapshot
        val newGameState = currentState.gameState.copy(
            boardData = lastSnapshot.board,
            result = GameResult.IN_PROGRESS
        )

        val newState = currentState.copy(
            gameState = newGameState,
            undoStack = newUndoStack,
            showResultDialog = false
        )

        return newState to listOf(GameEffect.TriggerHaptic)
    }

    private fun handleRestart(
        currentState: Game2048State
    ): Pair<Game2048State, List<GameEffect>> {
        val freshGame = createInitialGameState()
        val freshState = Game2048State(
            gameState = freshGame,
            undoStack = emptyList(),
            showResultDialog = false
        )
        return freshState to emptyList()
    }

    private fun handleSaveAndExit(
        currentState: Game2048State
    ): Pair<Game2048State, List<GameEffect>> {
        // Persistence is handled by the ViewModel/SaveManager
        return currentState to emptyList()
    }

    companion object {
        /**
         * Factory to create a fresh 2048 [GameState].
         */
        fun createInitialGameState(
            sessionId: String = java.util.UUID.randomUUID().toString()
        ): GameState {
            val rules = Game2048Rules()
            val initialBoard = rules.createInitialBoard()

            return GameState(
                gameId = sessionId,
                players = listOf(Player.PLAYER_ONE), // Single player game
                currentPlayer = Player.PLAYER_ONE,
                boardData = initialBoard,
                result = GameResult.IN_PROGRESS,
                scores = mapOf(1 to 0)
            )
        }
    }
}
