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
     */
    @Suppress("UNCHECKED_CAST")
    fun reduce(currentState: GameState, intent: GameIntent): ReducerResult {
        return when (intent) {
            is GameIntent.MakeMove -> handleMove(currentState, intent.move)
            is GameIntent.RestartGame -> handleRestart(currentState)
            is GameIntent.UndoMove -> handleUndo(currentState)
            is GameIntent.SaveAndExit -> ReducerResult(currentState) // persistence handled externally
        }
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
}
