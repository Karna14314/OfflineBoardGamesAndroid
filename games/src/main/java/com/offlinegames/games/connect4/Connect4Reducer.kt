package com.offlinegames.games.connect4

import com.offlinegames.core.*

/**
 * Connect 4 reducer — delegates to the generic [Reducer] and wraps
 * the result in [Connect4State].
 */
class Connect4Reducer {

    private val rules = Connect4Rules()
    private val coreReducer = Reducer(rules)

    fun reduce(currentState: Connect4State, intent: GameIntent): Pair<Connect4State, List<GameEffect>> {
        return when (intent) {
            is GameIntent.RestartGame -> {
                val freshGame = createInitialGameState(
                    vsAi = currentState.vsAi,
                    sessionId = currentState.gameState.gameId
                )
                val freshState = Connect4State(
                    gameState = freshGame,
                    vsAi = currentState.vsAi,
                    showResultDialog = false
                )
                freshState to emptyList()
            }
            is GameIntent.MakeMove -> {
                val result = coreReducer.reduce(currentState.gameState, intent)
                val showDialog = result.state.result != GameResult.IN_PROGRESS
                // Track the drop location for animation
                val move = intent.move
                val col = move.position.col
                val dropRow = rules.findDropRow(currentState.board, col)
                val newState = currentState.copy(
                    gameState = result.state,
                    showResultDialog = showDialog,
                    lastDropCol = col,
                    lastDropRow = dropRow,
                    animatingDrop = true
                )
                newState to result.effects
            }
            else -> {
                val result = coreReducer.reduce(currentState.gameState, intent)
                val showDialog = result.state.result != GameResult.IN_PROGRESS
                val newState = currentState.copy(
                    gameState = result.state,
                    showResultDialog = showDialog
                )
                newState to result.effects
            }
        }
    }

    companion object {
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
                boardData = GridBoard(Connect4Rules.COLS, Connect4Rules.ROWS),
                result = GameResult.IN_PROGRESS
            )
        }
    }
}
