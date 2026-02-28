package com.offlinegames.games.connect4

import com.offlinegames.core.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Connect 4 rules: win detection, gravity, AI blocking.
 */
class Connect4RulesTest {

    private lateinit var rules: Connect4Rules
    private lateinit var baseState: GameState

    @Before
    fun setUp() {
        rules = Connect4Rules()
        baseState = Connect4Reducer.createInitialGameState(vsAi = false)
    }

    // ── Gravity ─────────────────────────────────────────────────────────

    @Test
    fun `piece drops to bottom of empty column`() {
        val board = GridBoard(7, 6)
        val dropRow = rules.findDropRow(board, 3)
        assertEquals("Piece should drop to row 5 (bottom)", 5, dropRow)
    }

    @Test
    fun `piece stacks on top of existing piece`() {
        val board = GridBoard(7, 6).place(5, 3, 1)
        val dropRow = rules.findDropRow(board, 3)
        assertEquals("Piece should drop to row 4", 4, dropRow)
    }

    @Test
    fun `full column returns -1`() {
        var board = GridBoard(7, 6)
        for (r in 0 until 6) board = board.place(r, 0, 1)
        val dropRow = rules.findDropRow(board, 0)
        assertEquals("Full column should return -1", -1, dropRow)
    }

    // ── Vertical Win ────────────────────────────────────────────────────

    @Test
    fun `vertical win detection`() {
        var board = GridBoard(7, 6)
        // Stack 4 pieces in column 3 for player 1
        board = board.place(5, 3, 1).place(4, 3, 1)
            .place(3, 3, 1).place(2, 3, 1)
        assertTrue("Vertical 4 should be a win", rules.hasWon(board, 1))
    }

    @Test
    fun `vertical 3 is not a win`() {
        var board = GridBoard(7, 6)
        board = board.place(5, 3, 1).place(4, 3, 1).place(3, 3, 1)
        assertFalse("3 in a column is not a win", rules.hasWon(board, 1))
    }

    // ── Horizontal Win ──────────────────────────────────────────────────

    @Test
    fun `horizontal win detection`() {
        var board = GridBoard(7, 6)
        board = board.place(5, 0, 2).place(5, 1, 2)
            .place(5, 2, 2).place(5, 3, 2)
        assertTrue("Horizontal 4 should be a win", rules.hasWon(board, 2))
    }

    // ── Diagonal Win ────────────────────────────────────────────────────

    @Test
    fun `diagonal win detection (down-right)`() {
        var board = GridBoard(7, 6)
        // Diagonal from (2,0) to (5,3)
        board = board.place(2, 0, 1).place(3, 1, 1)
            .place(4, 2, 1).place(5, 3, 1)
        assertTrue("Diagonal down-right should be a win", rules.hasWon(board, 1))
    }

    @Test
    fun `diagonal win detection (up-right)`() {
        var board = GridBoard(7, 6)
        // Diagonal from (5,0) to (2,3)
        board = board.place(5, 0, 1).place(4, 1, 1)
            .place(3, 2, 1).place(2, 3, 1)
        assertTrue("Diagonal up-right should be a win", rules.hasWon(board, 1))
    }

    // ── Move Validation ─────────────────────────────────────────────────

    @Test
    fun `COLUMN_DROP move on non-full column is valid`() {
        val move = Move(0, Position(0, 3), MoveType.COLUMN_DROP)
        assertTrue(rules.isValidMove(baseState, move))
    }

    @Test
    fun `PLACE move type is rejected`() {
        val move = Move(0, Position(0, 3), MoveType.PLACE)
        assertFalse(rules.isValidMove(baseState, move))
    }

    @Test
    fun `out-of-range column is rejected`() {
        val move = Move(0, Position(0, 7), MoveType.COLUMN_DROP)
        assertFalse(rules.isValidMove(baseState, move))
    }

    // ── AI Blocking ─────────────────────────────────────────────────────

    @Test
    fun `AI blocks opponent vertical win`() {
        // Player 1 has 3 in column 3. AI (player 2) should block.
        var board = GridBoard(7, 6)
        board = board.place(5, 3, 1).place(4, 3, 1).place(3, 3, 1)
        // Add some player 2 pieces elsewhere
        board = board.place(5, 0, 2).place(5, 1, 2)

        val state = baseState.copy(
            boardData = board,
            currentPlayer = Player.AI
        )

        val ai = Connect4AI()
        val aiMove = ai.selectMove(state, Player.AI)
        assertNotNull("AI should produce a move", aiMove)
        // AI should pick column 3 to block
        assertEquals("AI should block in column 3", 3, aiMove!!.position.col)
    }

    // ── Draw Detection ──────────────────────────────────────────────────

    @Test
    fun `draw when board is full with no winner`() {
        // Create a full board with no 4-in-a-row (alternating columns pattern)
        val cells = Array(6) { r ->
            IntArray(7) { c ->
                // Pattern: alternate 1 and 2 to avoid 4-in-a-row
                val shift = if (c % 2 == 0) 0 else 1
                if ((r + shift) % 2 == 0) 1 else 2
            }
        }
        val board = GridBoard(7, 6, cells)

        // Verify no winner
        assertFalse(rules.hasWon(board, 1))
        assertFalse(rules.hasWon(board, 2))
        assertTrue(board.isFull())
    }
}
