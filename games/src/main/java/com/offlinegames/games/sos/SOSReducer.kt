package com.offlinegames.games.sos

import com.offlinegames.core.*

/**
 * SOS reducer — wraps the generic [Reducer] and manages
 * piece-type toggle and game-specific state.
 */
class SOSReducer {

    private val rules = SOSRules()
    private val coreReducer = Reducer(rules)

    fun reduce(currentState: SOSState, intent: GameIntent): Pair<SOSState, List<GameEffect>> {
        return when (intent) {
            is GameIntent.RestartGame -> {
                val freshGame = createInitialGameState(
                    vsAi = currentState.vsAi,
                    sessionId = currentState.gameState.gameId
                )
                val freshState = SOSState(
                    gameState = freshGame,
                    vsAi = currentState.vsAi,
                    showResultDialog = false
                )
                freshState to emptyList()
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
                boardData = GridBoard(SOSRules.SIZE, SOSRules.SIZE),
                result = GameResult.IN_PROGRESS,
                scores = mapOf(1 to 0, 2 to 0)
            )
        }
    }
}
