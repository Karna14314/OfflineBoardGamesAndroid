package com.offlinegames.core

/**
 * Output of the [Reducer]: a new immutable state plus any side effects.
 *
 * @param state   The next [GameState] — always a brand-new object.
 * @param effects Zero or more [GameEffect]s the ViewModel should dispatch.
 */
data class ReducerResult(
    val state: GameState,
    val effects: List<GameEffect> = emptyList()
)

/**
 * Pure function that maps (current state + intent) → next state.
 *
 * All game logic branching happens here. The reducer must:
 * - Never mutate the old [GameState].
 * - Always return a new [GameState].
 * - Have zero side effects of its own (effects are returned, not executed).
 *
 * [S] is the concrete board type the [rules] operate on.
 */
class Reducer<S : Any>(
    private val rules: GameRules<S>
) {

    /**
     * Process [intent] against [currentState] and return the result.
     * Legacy method for backward compatibility with placement-based games.
     */
    @Suppress("UNCHECKED_CAST")
    fun reduce(currentState: GameState, intent: GameIntent): ReducerResult {
        // Check if rules support new ActionBased architecture
        return if (rules is ActionBasedRules) {
            reduceWithAction(currentState, intent.toGameAction())
        } else {
            reduceLegacy(currentState, intent)
        }
    }

    /**
     * Process [action] against [currentState] using the new ActionBased architecture.
     * This enables complex multi-cell mutations and chain reactions.
     */
    @Suppress("UNCHECKED_CAST")
    fun reduceWithAction(currentState: GameState, action: GameAction): ReducerResult {
        val actionRules = rules as? ActionBasedRules<S>
            ?: return ReducerResult(
                currentState,
                listOf(GameEffect.ShowMessage("Game does not support actions."))
            )

        return when (action) {
            is GameAction.RestartAction -> handleRestart(currentState)
            is GameAction.UndoAction -> handleUndo(currentState)
            is GameAction.SaveAndExitAction -> ReducerResult(currentState)
            else -> handleAction(currentState, action, actionRules)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleAction(
        state: GameState,
        action: GameAction,
        actionRules: ActionBasedRules<S>
    ): ReducerResult {
        // Guard: game must still be in progress
        if (!state.isOngoing && action !is GameAction.RestartAction) {
            return ReducerResult(state, listOf(GameEffect.ShowMessage("Game is already over.")))
        }

        // Guard: action must be valid
        if (!actionRules.isValidAction(state, action)) {
            return ReducerResult(state, listOf(GameEffect.ShowMessage("Invalid action.")))
        }

        val currentBoard = state.boardData as S
        val result = actionRules.applyAction(currentBoard, action, state.currentPlayer)

        // Build intermediate state with new board
        val intermediateState = state.copy(
            boardData = result.newBoardData,
            moveHistory = result.moveRecord?.let { state.moveHistory + it } ?: state.moveHistory
        )

        // Compute updated scores
        val newScores = state.scores.toMutableMap()
        if (result.scoreDelta != 0) {
            val playerId = state.currentPlayer.id
            newScores[playerId] = (newScores[playerId] ?: 0) + result.scoreDelta
        }

        // Determine next player — rules decide if turn advances
        // For chain moves, the same player continues
        val shouldContinue = actionRules.shouldContinueTurn(intermediateState, action)
        val nextPlayer = if (shouldContinue) {
            state.currentPlayer
        } else {
            TurnManager.nextPlayer(state)
        }

        val newState = intermediateState.copy(
            currentPlayer = nextPlayer,
            scores = newScores
        )

        // Check for game end
        val gameResult = if (result.gameEnded) {
            rules.evaluateResult(newState)
        } else {
            rules.evaluateResult(newState)
        }

        val finalState = newState.copy(result = gameResult)

        // Build effects
        val effects = mutableListOf<GameEffect>(
            GameEffect.PlayMoveSound,
            GameEffect.TriggerHaptic
        )

        if (gameResult == GameResult.WIN || gameResult == GameResult.DRAW) {
            effects += GameEffect.PlayWinSound
            effects += GameEffect.NavigateToResult
        }

        // Process chain actions if any (for games like Checkers with multi-jump)
        if (result.chainActions.isNotEmpty() && !shouldContinue) {
            // Chain actions will be processed in subsequent reducer calls
            // The caller (ViewModel) should dispatch them
        }

        return ReducerResult(state = finalState, effects = effects)
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleMove(state: GameState, move: Move): ReducerResult {
        // Guard: game must still be in progress
        if (!state.isOngoing) {
            return ReducerResult(state, listOf(GameEffect.ShowMessage("Game is already over.")))
        }

        // Guard: move must be legal
        if (!rules.isValidMove(state, move)) {
            return ReducerResult(state, listOf(GameEffect.ShowMessage("Invalid move.")))
        }

        val currentBoard = state.boardData as S
        val fixedMove = move.copy(playerId = state.currentPlayer.id)
        val newBoard = rules.applyMove(currentBoard, fixedMove, state.currentPlayer)
        val record = MoveRecord(move = fixedMove)

        // Build intermediate state with new board
        val intermediateState = state.copy(
            boardData = newBoard,
            moveHistory = state.moveHistory + record
        )

        // Compute scores (for scoring games like SOS, Dots & Boxes)
        val newScores = rules.computeScores(intermediateState, fixedMove)

        // Determine next player — rules decide if turn advances
        val nextPlayer = if (rules.shouldAdvanceTurn(intermediateState)) {
            TurnManager.nextPlayer(state)
        } else {
            state.currentPlayer
        }

        val newState = intermediateState.copy(
            currentPlayer = nextPlayer,
            scores = newScores
        )

        val result = rules.evaluateResult(newState)
        val finalState = newState.copy(result = result)

        val effects = mutableListOf<GameEffect>(
            GameEffect.PlayMoveSound,
            GameEffect.TriggerHaptic
        )
        if (result == GameResult.WIN || result == GameResult.DRAW) {
            effects += GameEffect.PlayWinSound
            effects += GameEffect.NavigateToResult
        }

        return ReducerResult(state = finalState, effects = effects)
    }

    private fun handleRestart(state: GameState): ReducerResult {
        // Actual board reset is delegated to the game's ViewModel which
        // creates a fresh initial state via GameSessionManager.
        return ReducerResult(state, listOf(GameEffect.ShowMessage("Restarting…")))
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleUndo(state: GameState): ReducerResult {
        // Check if rules support undo via ActionBased interface
        val actionRules = rules as? ActionBasedRules<S>
        val undoAction = actionRules?.getUndoAction(state)

        if (undoAction != null) {
            // Use action-based undo
            return reduceWithAction(state, undoAction)
        }

        // Legacy undo implementation
        if (state.moveHistory.isEmpty()) {
            return ReducerResult(state, listOf(GameEffect.ShowMessage("Nothing to undo.")))
        }

        // Remove the last two moves (current player's and the opponent's last)
        // so the human player is back to their own last turn.
        val movesToRemove = if (state.players.size == 2 && state.moveHistory.size >= 2) 2 else 1
        val trimmedHistory = state.moveHistory.dropLast(movesToRemove)

        // Rebuild board by replaying from empty
        // For simplicity the game's ViewModel can override this — here we
        // restore the previous player by counting moves.
        val previousPlayer = if (movesToRemove == 2) {
            state.currentPlayer // unchanged after 2 undos
        } else {
            TurnManager.nextPlayer(state)
        }

        val newState = state.copy(
            moveHistory = trimmedHistory,
            currentPlayer = previousPlayer,
            result = GameResult.IN_PROGRESS
        )
        return ReducerResult(newState, listOf(GameEffect.TriggerHaptic))
    }

    // Legacy reducer for backward compatibility
    @Suppress("UNCHECKED_CAST")
    private fun reduceLegacy(currentState: GameState, intent: GameIntent): ReducerResult {
        return when (intent) {
            is GameIntent.MakeMove -> handleMove(currentState, intent.move)
            is GameIntent.RestartGame -> handleRestart(currentState)
            is GameIntent.UndoMove -> handleUndo(currentState)
            is GameIntent.SaveAndExit -> ReducerResult(currentState)
        }
    }
}

/**
 * Enhanced reducer specifically for ActionBased games.
 * Provides cleaner API for games using the new GameAction architecture.
 */
class ActionReducer<S : Any>(
    private val actionRules: ActionBasedRules<S>
) {
    private val coreReducer = Reducer(actionRules)

    /**
     * Process a GameAction and return the result.
     */
    fun reduce(state: GameState, action: GameAction): ReducerResult {
        return coreReducer.reduceWithAction(state, action)
    }

    /**
     * Process multiple actions in sequence (for chain reactions).
     */
    fun reduceChain(state: GameState, actions: List<GameAction>): ReducerResult {
        var currentResult = ReducerResult(state)
        for (action in actions) {
            currentResult = coreReducer.reduceWithAction(currentResult.state, action)
        }
        return currentResult
    }
}
