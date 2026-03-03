package com.offlinegames.games.minesweeper

import com.offlinegames.core.*

/**
 * Minesweeper Reducer using the ActionBased architecture.
 */
class MinesweeperReducer {

    private val rules = MinesweeperRules()
    private val actionReducer = ActionReducer(rules)

    /**
     * Process [action] against [currentState] and return (newState, effects).
     */
    fun reduce(
        currentState: MinesweeperState,
        action: GameAction
    ): Pair<MinesweeperState, List<GameEffect>> {
        return when (action) {
            is GameAction.RestartAction -> handleRestart(currentState)
            is GameAction.UndoAction -> currentState to listOf(GameEffect.ShowMessage("Undo not available"))
            is GameAction.SaveAndExitAction -> handleSaveAndExit(currentState)
            is GameAction.RevealCellsAction -> handleReveal(currentState, action)
            is GameAction.FlagCellAction -> handleFlag(currentState, action)
            is GameAction.ChordAction -> handleChord(currentState, action)
            else -> currentState to listOf(GameEffect.ShowMessage("Invalid action"))
        }
    }

    /**
     * Legacy intent support.
     */
    fun reduce(
        currentState: MinesweeperState,
        intent: GameIntent
    ): Pair<MinesweeperState, List<GameEffect>> {
        return when (intent) {
            is GameIntent.RestartGame -> handleRestart(currentState)
            is GameIntent.UndoMove -> currentState to listOf(GameEffect.ShowMessage("Undo not available"))
            is GameIntent.SaveAndExit -> handleSaveAndExit(currentState)
            else -> currentState to emptyList()
        }
    }

    private fun handleReveal(
        currentState: MinesweeperState,
        action: GameAction.RevealCellsAction
    ): Pair<MinesweeperState, List<GameEffect>> {
        val result = actionReducer.reduce(currentState.gameState, action)

        val newState = currentState.copy(
            gameState = result.state,
            showResultDialog = result.state.result != GameResult.IN_PROGRESS
        )

        return newState to result.effects
    }

    private fun handleFlag(
        currentState: MinesweeperState,
        action: GameAction.FlagCellAction
    ): Pair<MinesweeperState, List<GameEffect>> {
        val result = actionReducer.reduce(currentState.gameState, action)

        val newState = currentState.copy(
            gameState = result.state
        )

        return newState to result.effects
    }

    private fun handleChord(
        currentState: MinesweeperState,
        action: GameAction.ChordAction
    ): Pair<MinesweeperState, List<GameEffect>> {
        val result = actionReducer.reduce(currentState.gameState, action)

        val newState = currentState.copy(
            gameState = result.state,
            showResultDialog = result.state.result != GameResult.IN_PROGRESS
        )

        return newState to result.effects
    }

    private fun handleRestart(currentState: MinesweeperState): Pair<MinesweeperState, List<GameEffect>> {
        val difficulty = when (currentState.board.width) {
            16 -> if (currentState.board.height == 16) MinesweeperDifficulty.INTERMEDIATE else MinesweeperDifficulty.EXPERT
            else -> MinesweeperDifficulty.BEGINNER
        }

        val freshGame = createInitialGameState(difficulty)
        val freshState = MinesweeperState(
            gameState = freshGame,
            timerSeconds = 0
        )
        return freshState to emptyList()
    }

    private fun handleSaveAndExit(currentState: MinesweeperState): Pair<MinesweeperState, List<GameEffect>> {
        return currentState to emptyList()
    }

    companion object {
        fun createInitialGameState(
            difficulty: MinesweeperDifficulty = MinesweeperDifficulty.BEGINNER,
            sessionId: String = java.util.UUID.randomUUID().toString()
        ): GameState {
            val board = MinesweeperRules().createInitialBoard(difficulty)

            return GameState(
                gameId = sessionId,
                players = listOf(Player.PLAYER_ONE), // Single player
                currentPlayer = Player.PLAYER_ONE,
                boardData = board,
                result = GameResult.IN_PROGRESS
            )
        }
    }
}
