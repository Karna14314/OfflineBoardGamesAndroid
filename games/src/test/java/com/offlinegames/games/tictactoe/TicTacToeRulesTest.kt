package com.offlinegames.games.tictactoe

import com.offlinegames.core.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TicTacToe win detection and move validation.
 */
class TicTacToeRulesTest {

    private lateinit var rules: TicTacToeRules
    private lateinit var baseState: GameState

    @Before
    fun setUp() {
        rules = TicTacToeRules()
        baseState = TicTacToeReducer.createInitialGameState(vsAi = false)
    }

    // ── Win Detection ────────────────────────────────────────────────────────

    @Test
    fun `player 1 wins on top row`() {
        val board = GridBoard(3, 3)
            .place(0, 0, 1).place(0, 1, 1).place(0, 2, 1)
        assertTrue("Top row should be a win for player 1", rules.hasWon(board, 1))
    }

    @Test
    fun `player 2 wins on middle column`() {
        val board = GridBoard(3, 3)
            .place(0, 1, 2).place(1, 1, 2).place(2, 1, 2)
        assertTrue("Middle column should be a win for player 2", rules.hasWon(board, 2))
    }

    @Test
    fun `player 1 wins on main diagonal`() {
        val board = GridBoard(3, 3)
            .place(0, 0, 1).place(1, 1, 1).place(2, 2, 1)
        assertTrue("Main diagonal should be a win", rules.hasWon(board, 1))
    }

    @Test
    fun `player 1 wins on anti-diagonal`() {
        val board = GridBoard(3, 3)
            .place(0, 2, 1).place(1, 1, 1).place(2, 0, 1)
        assertTrue("Anti-diagonal should be a win", rules.hasWon(board, 1))
    }

    @Test
    fun `no win on empty board`() {
        val board = GridBoard(3, 3)
        assertFalse(rules.hasWon(board, 1))
        assertFalse(rules.hasWon(board, 2))
    }

    @Test
    fun `partial row is not a win`() {
        val board = GridBoard(3, 3)
            .place(0, 0, 1).place(0, 1, 1)
        assertFalse("Partial row should not be a win", rules.hasWon(board, 1))
    }

    @Test
    fun `draw detected when board full and no winner`() {
        // X O X
        // X X O
        // O X O  → draw
        val board = GridBoard(3, 3)
            .place(0, 0, 1).place(0, 1, 2).place(0, 2, 1)
            .place(1, 0, 1).place(1, 1, 1).place(1, 2, 2)
            .place(2, 0, 2).place(2, 1, 1).place(2, 2, 2)
        assertTrue("Full board with no winner should be a draw", board.isFull())
        assertFalse(rules.hasWon(board, 1))
        assertFalse(rules.hasWon(board, 2))
    }

    // ── Move Validation ──────────────────────────────────────────────────────

    @Test
    fun `valid move on empty cell is accepted`() {
        val move = Move(0, Position(1, 1))
        assertTrue(rules.isValidMove(baseState, move))
    }

    @Test
    fun `invalid move on occupied cell is rejected`() {
        val board = GridBoard(3, 3).place(1, 1, 1)
        val occupiedState = baseState.copy(boardData = board)
        assertFalse(rules.isValidMove(occupiedState, Move(0, Position(1, 1))))
    }

    @Test
    fun `getLegalMoves returns 9 moves on empty board`() {
        val moves = rules.getLegalMoves(baseState, Player.PLAYER_ONE)
        assertEquals(9, moves.size)
    }

    @Test
    fun `getLegalMoves decreases after each placement`() {
        val board = GridBoard(3, 3).place(0, 0, 1)
        val state = baseState.copy(boardData = board)
        val moves = rules.getLegalMoves(state, Player.PLAYER_ONE)
        assertEquals(8, moves.size)
    }
}
