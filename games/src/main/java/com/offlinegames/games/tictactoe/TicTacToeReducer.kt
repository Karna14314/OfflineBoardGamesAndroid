package com.offlinegames.games.tictactoe

import com.offlinegames.core.*

/**
 * TicTacToe reducer — delegates to the generic [Reducer] and then
 * updates the [TicTacToeState] wrapper.
 *
 * Adding game-over dialog visibility logic here keeps the ViewModel lean.
 */
class TicTacToeReducer {

    private val rules = TicTacToeRules()
    private val coreReducer = Reducer(rules)

    /**
     * Process [intent] against [currentState] and return (newState, effects).
     */
    fun reduce(currentState: TicTacToeState, intent: GameIntent): Pair<TicTacToeState, List<GameEffect>> {
        return when (intent) {
            is GameIntent.RestartGame -> {
                val freshGame = createInitialGameState(
                    vsAi = currentState.vsAi,
                    sessionId = currentState.gameState.gameId
                )
                val freshState = TicTacToeState(
                    gameState = freshGame,
                    vsAi = currentState.vsAi,
                    showResultDialog = false
                )
                freshState to emptyList()
            }
            else -> {
                val result = coreReducer.reduce(currentState.gameState, intent)
                val showDialog = result.state.result != GameResult.IN_PROGRESS
                val newTttState = currentState.copy(
                    gameState = result.state,
                    showResultDialog = showDialog
                )
                newTttState to result.effects
            }
        }
    }

    companion object {
        /** Factory to create a fresh TicTacToe [GameState]. */
        fun createInitialGameState(
            vsAi: Boolean = false,
            sessionId: String = java.util.UUID.randomUUID().toString()
        ): GameState {
            val p2 = if (vsAi) Player.AI else Player.PLAYER_TWO
            val players = listOf(Player.PLAYER_ONE, p2)
            return GameState(
                gameId = sessionId,
                players = players,
                currentPlayer = Player.PLAYER_ONE,
                boardData = GridBoard(3, 3),
                result = GameResult.IN_PROGRESS
            )
        }
    }
}
