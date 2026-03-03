package com.offlinegames.games.ludo

import com.offlinegames.core.*

/**
 * Ludo Reducer: processes all Ludo game actions and manages
 * UI-level state (selection, animations, result dialog).
 */
class LudoReducer {

    private val rules = LudoRules()
    private val actionReducer = ActionReducer(rules)

    /**
     * Process [action] and return (newState, effects).
     */
    fun reduce(
        currentState: LudoState,
        action: GameAction
    ): Pair<LudoState, List<GameEffect>> {
        return when (action) {
            is GameAction.RestartAction -> handleRestart(currentState)
            is GameAction.SaveAndExitAction -> currentState to emptyList()
            is GameAction.DiceRollAction -> handleDiceRoll(currentState, action)
            is GameAction.TokenMoveAction -> handleTokenMove(currentState, action)
            is GameAction.PassTurnAction -> handlePassTurn(currentState, action)
            else -> currentState to listOf(GameEffect.ShowMessage("Invalid action for Ludo"))
        }
    }

    private fun handleDiceRoll(
        state: LudoState,
        action: GameAction.DiceRollAction
    ): Pair<LudoState, List<GameEffect>> {
        val result = actionReducer.reduce(state.gameState, action)
        val newBoard = result.state.boardData as LudoBoard
        // Use the current player from the result state (the player who rolled)
        val playerId = result.state.currentPlayer.id - 1

        // Find movable tokens for the current player
        val diceValue = newBoard.diceValue
        val movableIds = if (diceValue != null) {
            rules.getMovableTokenIds(newBoard, playerId)
        } else {
            emptyList()
        }

        val newState = state.copy(
            gameState = result.state,
            movableTokenIds = movableIds,
            selectedTokenId = if (movableIds.size == 1) movableIds.first() else null,
            animatingDiceValue = action.result  // Always show the dice value that was rolled
        )

        val effects = mutableListOf<GameEffect>(GameEffect.TriggerHaptic)

        if (movableIds.isEmpty()) {
            effects.add(GameEffect.ShowMessage("No moves available — passing turn"))
        }

        return newState to effects
    }

    private fun handleTokenMove(
        state: LudoState,
        action: GameAction.TokenMoveAction
    ): Pair<LudoState, List<GameEffect>> {
        val result = actionReducer.reduce(state.gameState, action)
        val newBoard = result.state.boardData as LudoBoard

        val effects = mutableListOf<GameEffect>(
            GameEffect.PlayMoveSound,
            GameEffect.TriggerHaptic
        )

        val gameEnded = result.state.result != GameResult.IN_PROGRESS
        if (gameEnded) {
            effects.add(GameEffect.PlayWinSound)
            effects.add(GameEffect.NavigateToResult)
        }

        val newState = state.copy(
            gameState = result.state,
            movableTokenIds = emptyList(),
            selectedTokenId = null,
            showResultDialog = gameEnded
            // Keep animatingDiceValue as-is so dice doesn't go blank
        )

        return newState to effects
    }

    private fun handlePassTurn(
        state: LudoState,
        action: GameAction.PassTurnAction
    ): Pair<LudoState, List<GameEffect>> {
        // Manually apply pass turn: reset dice, advance to next player
        val board = state.board
        val passedBoard = board.copy(
            diceRolled = false,
            diceValue = null,
            extraTurn = false
        )

        // Advance to next player
        val currentPlayerIdx = state.gameState.players.indexOfFirst { it.id == state.currentPlayer.id }
        val nextPlayerIdx = (currentPlayerIdx + 1) % state.gameState.players.size
        val nextPlayer = state.gameState.players[nextPlayerIdx]

        val newGameState = state.gameState.copy(
            boardData = passedBoard,
            currentPlayer = nextPlayer
        )

        val newState = state.copy(
            gameState = newGameState,
            movableTokenIds = emptyList(),
            selectedTokenId = null
            // Keep animatingDiceValue so dice shows last roll
        )

        return newState to listOf(GameEffect.TriggerHaptic)
    }

    private fun handleRestart(state: LudoState): Pair<LudoState, List<GameEffect>> {
        val freshGame = LudoViewModel.createInitialGameState(state.playerCount, false)
        return LudoState(
            gameState = freshGame,
            playerCount = state.playerCount
        ) to emptyList()
    }

    companion object {
        fun createInitialGameState(
            playerCount: Int = 2,
            sessionId: String = java.util.UUID.randomUUID().toString()
        ): GameState {
            val players = (1..playerCount).map { i ->
                Player(id = i, name = "Player $i")
            }
            val board = LudoBoard.create(playerCount)

            return GameState(
                gameId = sessionId,
                players = players,
                currentPlayer = players.first(),
                boardData = board,
                result = GameResult.IN_PROGRESS
            )
        }
    }
}
