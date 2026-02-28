package com.offlinegames.games.tictactoe

import com.offlinegames.core.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the TicTacToe MVI reducer transitions.
 */
class TicTacToeReducerTest {

    private lateinit var reducer: TicTacToeReducer
    private lateinit var initialState: TicTacToeState

    @Before
    fun setUp() {
        reducer = TicTacToeReducer()
        initialState = TicTacToeState(
            gameState = TicTacToeReducer.createInitialGameState(vsAi = false)
        )
    }

    @Test
    fun `initial state has player one as current player`() {
        assertEquals(Player.PLAYER_ONE.id, initialState.currentPlayer.id)
    }

    @Test
    fun `valid move advances to player two`() {
        val (next, _) = reducer.reduce(initialState, GameIntent.MakeMove(Move(0, Position(0, 0))))
        assertEquals(Player.PLAYER_TWO.id, next.currentPlayer.id)
    }

    @Test
    fun `invalid move on occupied cell does not change state`() {
        // Place at 0,0
        val (afterFirst, _) = reducer.reduce(initialState, GameIntent.MakeMove(Move(0, Position(0, 0))))
        // Try to place again at 0,0 (occupied)
        val (afterSecond, effects) = reducer.reduce(afterFirst, GameIntent.MakeMove(Move(0, Position(0, 0))))
        // State must be unchanged
        assertEquals(afterFirst.board, afterSecond.board)
        // An error message effect must be emitted
        assertTrue(effects.any { it is GameEffect.ShowMessage })
    }

    @Test
    fun `win is detected after three in a row`() {
        // Player 1 wins: (0,0), (0,1), (0,2) — with player 2 playing elsewhere
        var state = initialState
        val moves = listOf(
            Move(0, Position(0, 0)) to Player.PLAYER_ONE,
            Move(0, Position(1, 0)) to Player.PLAYER_TWO,
            Move(0, Position(0, 1)) to Player.PLAYER_ONE,
            Move(0, Position(1, 1)) to Player.PLAYER_TWO,
            Move(0, Position(0, 2)) to Player.PLAYER_ONE  // winning move
        )
        for ((move, _) in moves) {
            val (next, _) = reducer.reduce(state, GameIntent.MakeMove(move))
            state = next
        }
        assertEquals(GameResult.WIN, state.result)
        assertTrue(state.showResultDialog)
    }

    @Test
    fun `draw is detected when board is full`() {
        // X O X
        // X X O
        // O X O  → draw
        val movesInOrder = listOf(
            Move(0, Position(0, 0)), // P1
            Move(0, Position(0, 1)), // P2
            Move(0, Position(0, 2)), // P1
            Move(0, Position(1, 2)), // P2
            Move(0, Position(1, 0)), // P1
            Move(0, Position(2, 0)), // P2
            Move(0, Position(1, 1)), // P1
            Move(0, Position(2, 2)), // P2
            Move(0, Position(2, 1))  // P1
        )
        var state = initialState
        for (move in movesInOrder) {
            val (next, _) = reducer.reduce(state, GameIntent.MakeMove(move))
            state = next
        }
        assertEquals(GameResult.DRAW, state.result)
    }

    @Test
    fun `restart resets board to empty`() {
        val (afterMove, _) = reducer.reduce(initialState, GameIntent.MakeMove(Move(0, Position(1, 1))))
        val (restarted, _) = reducer.reduce(afterMove, GameIntent.RestartGame)
        // Board should be fresh
        assertEquals(GridBoard(3, 3), restarted.board)
        assertEquals(GameResult.IN_PROGRESS, restarted.result)
    }

    @Test
    fun `move history grows by one after each valid move`() {
        val (state1, _) = reducer.reduce(initialState, GameIntent.MakeMove(Move(0, Position(0, 0))))
        val (state2, _) = reducer.reduce(state1, GameIntent.MakeMove(Move(0, Position(1, 1))))
        assertEquals(2, state2.gameState.moveHistory.size)
    }
}
