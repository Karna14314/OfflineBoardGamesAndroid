package com.offlinegames.games.tictactoe

import com.offlinegames.ai.DifficultyProfile
import com.offlinegames.core.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying that [TicTacToeAI] selects optimal moves.
 */
class TicTacToeAITest {

    private val ai = TicTacToeAI(DifficultyProfile.HARD)

    private fun stateWithBoard(board: GridBoard, currentPlayer: Player): GameState {
        return TicTacToeReducer.createInitialGameState(vsAi = true).copy(
            boardData = board,
            currentPlayer = currentPlayer
        )
    }

    @Test
    fun `AI takes the winning move when available`() {
        // O O _
        // X X _
        // _ _ _
        // AI (player 2/O) can win by playing (0,2)
        val board = GridBoard(3, 3)
            .place(0, 0, 2).place(0, 1, 2)
            .place(1, 0, 1).place(1, 1, 1)

        val state = stateWithBoard(board, Player.AI)
        val move = ai.selectMove(state, Player.AI) as? Move

        assertNotNull("AI should return a move", move)
        assertEquals("AI should take the winning position (0,2)", 0, move!!.position.row)
        assertEquals(2, move.position.col)
    }

    @Test
    fun `AI blocks opponent winning move`() {
        // X X _
        // O O _
        // _ _ _
        // Player 1 can win at (0,2). AI (Player 2) should block it.
        val board = GridBoard(3, 3)
            .place(0, 0, 1).place(0, 1, 1)
            .place(1, 0, 2).place(1, 1, 2)

        // It's player 1's turn in this state for realistic blocking scenario.
        // Rephrase: it's AI's turn, we add one more X elsewhere to make it AI's turn
        // and still test blocking by giving AI only blocking option
        // X X _
        // O O _
        // X _ _   (player1 played (2,0), now AI must block at (0,2))
        val board2 = board.place(2, 0, 1)
        val state = stateWithBoard(board2, Player.AI)
        val move = ai.selectMove(state, Player.AI) as? Move

        assertNotNull(move)
        assertEquals("AI should block at row 0, col 2", 0, move!!.position.row)
        assertEquals(2, move.position.col)
    }

    @Test
    fun `AI returns null when board is full`() {
        // Completely full board (draw state)
        val board = GridBoard(3, 3)
            .place(0, 0, 1).place(0, 1, 2).place(0, 2, 1)
            .place(1, 0, 2).place(1, 1, 1).place(1, 2, 2)
            .place(2, 0, 2).place(2, 1, 1).place(2, 2, 2)

        val state = stateWithBoard(board, Player.AI).copy(
            result = GameResult.DRAW,
            moveHistory = listOf(MoveRecord(Move(1, Position(2, 2))))
        )
        val move = ai.selectMove(state, Player.AI)
        assertNull("No moves on full board", move)
    }

    @Test
    fun `AI picks centre on empty board`() {
        val board = GridBoard(3, 3)
        val state = stateWithBoard(board, Player.AI)
        val move = ai.selectMove(state, Player.AI) as? Move
        assertNotNull(move)
        // Centre (1,1) is the optimal first move in TicTacToe
        assertEquals(1, move!!.position.row)
        assertEquals(1, move.position.col)
    }
}
