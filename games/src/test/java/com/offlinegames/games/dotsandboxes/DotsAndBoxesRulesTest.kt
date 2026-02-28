package com.offlinegames.games.dotsandboxes

import com.offlinegames.core.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Dots & Boxes rules: box detection, extra turns, scoring.
 */
class DotsAndBoxesRulesTest {

    private lateinit var rules: DotsAndBoxesRules
    private lateinit var baseState: GameState

    @Before
    fun setUp() {
        rules = DotsAndBoxesRules()
        baseState = DotsAndBoxesReducer.createInitialGameState(vsAi = false)
    }

    // ── Box Detection ───────────────────────────────────────────────────

    @Test
    fun `box is not complete with 0 edges`() {
        val board = DotsAndBoxesRules.createInitialBoard()
        assertFalse("Box should not be complete", rules.isBoxComplete(board, 1, 1))
    }

    @Test
    fun `box is not complete with 3 edges`() {
        var board = DotsAndBoxesRules.createInitialBoard()
        // Box at (1,1): top=(0,1), bottom=(2,1), left=(1,0), right=(1,2)
        board = board.place(0, 1, 1) // top edge
            .place(2, 1, 1)          // bottom edge
            .place(1, 0, 1)          // left edge
        assertFalse("3 edges should not complete the box", rules.isBoxComplete(board, 1, 1))
    }

    @Test
    fun `box is complete with 4 edges`() {
        var board = DotsAndBoxesRules.createInitialBoard()
        // Box at (1,1): top=(0,1), bottom=(2,1), left=(1,0), right=(1,2)
        board = board.place(0, 1, 1)
            .place(2, 1, 1)
            .place(1, 0, 1)
            .place(1, 2, 1)
        assertTrue("4 edges should complete the box", rules.isBoxComplete(board, 1, 1))
    }

    @Test
    fun `box detection correctness for multiple boxes`() {
        var board = DotsAndBoxesRules.createInitialBoard()
        // Complete box at (1,1)
        board = board.place(0, 1, 1).place(2, 1, 1)
            .place(1, 0, 1).place(1, 2, 1)
        // Partial box at (1,3) — shares right edge of (1,1)
        // (1,3) edges: top=(0,3), bottom=(2,3), left=(1,2) [shared], right=(1,4)
        board = board.place(0, 3, 2).place(2, 3, 2)
        // (1,3) has 3 edges: top, bottom, left (shared)

        assertTrue("Box (1,1) should be complete", rules.isBoxComplete(board, 1, 1))
        assertFalse("Box (1,3) should not be complete", rules.isBoxComplete(board, 1, 3))
    }

    // ── Edge Count ──────────────────────────────────────────────────────

    @Test
    fun `edge count is 0 for empty box`() {
        val board = DotsAndBoxesRules.createInitialBoard()
        assertEquals(0, rules.countEdges(board, 1, 1))
    }

    @Test
    fun `edge count tracks drawn edges`() {
        var board = DotsAndBoxesRules.createInitialBoard()
        board = board.place(0, 1, 1).place(1, 0, 2)
        assertEquals(2, rules.countEdges(board, 1, 1))
    }

    // ── Extra Turn After Box Capture ────────────────────────────────────

    @Test
    fun `extra turn after box capture`() {
        var board = DotsAndBoxesRules.createInitialBoard()
        // Set up box (1,1) with 3 edges
        board = board.place(0, 1, 1).place(2, 1, 1).place(1, 0, 1)

        // Player 1 draws the 4th edge, completing the box
        val move = Move(1, Position(1, 2), MoveType.EDGE)
        val newBoard = rules.applyMove(board, move, Player.PLAYER_ONE)

        val record = MoveRecord(move = move)
        val state = baseState.copy(
            boardData = newBoard,
            moveHistory = listOf(record)
        )

        val shouldAdvance = rules.shouldAdvanceTurn(state)
        assertFalse("Player should get extra turn after capturing a box", shouldAdvance)
    }

    @Test
    fun `turn advances when no box captured`() {
        val board = DotsAndBoxesRules.createInitialBoard()

        val move = Move(1, Position(0, 1), MoveType.EDGE)
        val newBoard = rules.applyMove(board, move, Player.PLAYER_ONE)

        val record = MoveRecord(move = move)
        val state = baseState.copy(
            boardData = newBoard,
            moveHistory = listOf(record)
        )

        val shouldAdvance = rules.shouldAdvanceTurn(state)
        assertTrue("Turn should advance when no box captured", shouldAdvance)
    }

    // ── Move Validation ─────────────────────────────────────────────────

    @Test
    fun `EDGE move on valid edge is accepted`() {
        val move = Move(0, Position(0, 1), MoveType.EDGE)
        assertTrue(rules.isValidMove(baseState, move))
    }

    @Test
    fun `EDGE move on dot position is rejected`() {
        val move = Move(0, Position(0, 0), MoveType.EDGE) // dot position
        assertFalse(rules.isValidMove(baseState, move))
    }

    @Test
    fun `EDGE move on box center is rejected`() {
        val move = Move(0, Position(1, 1), MoveType.EDGE) // box center
        assertFalse(rules.isValidMove(baseState, move))
    }

    @Test
    fun `PLACE move type is rejected`() {
        val move = Move(0, Position(0, 1), MoveType.PLACE)
        assertFalse(rules.isValidMove(baseState, move))
    }

    @Test
    fun `already drawn edge is rejected`() {
        val board = (baseState.boardData as GridBoard).place(0, 1, 1)
        val state = baseState.copy(boardData = board)
        val move = Move(0, Position(0, 1), MoveType.EDGE)
        assertFalse(rules.isValidMove(state, move))
    }

    // ── Scoring ─────────────────────────────────────────────────────────

    @Test
    fun `score increments on box capture`() {
        var board = DotsAndBoxesRules.createInitialBoard()
        // Set up box (1,1) with 3 edges
        board = board.place(0, 1, 1).place(2, 1, 1).place(1, 0, 1)

        // Complete the box
        val move = Move(1, Position(1, 2), MoveType.EDGE)
        val newBoard = rules.applyMove(board, move, Player.PLAYER_ONE)

        val state = baseState.copy(
            boardData = newBoard,
            scores = mapOf(1 to 0, 2 to 0)
        )

        val newScores = rules.computeScores(state, move)
        assertEquals("Player 1 should have 1 box", 1, newScores[1])
    }

    // ── Legal Moves ─────────────────────────────────────────────────────

    @Test
    fun `initial board has correct number of legal moves`() {
        val moves = rules.getLegalMoves(baseState, Player.PLAYER_ONE)
        // 4x4 grid of boxes → 5 rows of horizontal edges (5*4=20) + 4 rows of vertical edges (5*4=20)
        // Actually: horizontal edges = 5 rows × 4 per row = 20
        //           vertical edges = 4 rows × 5 per row = 20
        // Total = 40
        assertEquals("Should have 40 legal edge moves", 40, moves.size)
    }
}
