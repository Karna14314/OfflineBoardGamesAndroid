package com.offlinegames.games.sos

import com.offlinegames.core.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SOS game: pattern detection, extra turns, scoring.
 */
class SOSRulesTest {

    private lateinit var rules: SOSRules
    private lateinit var baseState: GameState

    @Before
    fun setUp() {
        rules = SOSRules()
        baseState = SOSReducer.createInitialGameState(vsAi = false)
    }

    // ── SOS Detection ───────────────────────────────────────────────────

    @Test
    fun `detect horizontal SOS`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 0, SOSRules.PIECE_S)
            .place(0, 1, SOSRules.PIECE_O)
            .place(0, 2, SOSRules.PIECE_S)

        // The last piece placed at (0,2) should complete the SOS
        val count = rules.countNewSOS(board, 0, 2)
        assertTrue("Should detect at least one SOS", count >= 1)
    }

    @Test
    fun `detect vertical SOS`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 0, SOSRules.PIECE_S)
            .place(1, 0, SOSRules.PIECE_O)
            .place(2, 0, SOSRules.PIECE_S)

        val count = rules.countNewSOS(board, 2, 0)
        assertTrue("Vertical SOS should be detected", count >= 1)
    }

    @Test
    fun `detect diagonal SOS (down-right)`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 0, SOSRules.PIECE_S)
            .place(1, 1, SOSRules.PIECE_O)
            .place(2, 2, SOSRules.PIECE_S)

        val count = rules.countNewSOS(board, 2, 2)
        assertTrue("Diagonal SOS should be detected", count >= 1)
    }

    @Test
    fun `detect diagonal SOS (down-left)`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 2, SOSRules.PIECE_S)
            .place(1, 1, SOSRules.PIECE_O)
            .place(2, 0, SOSRules.PIECE_S)

        val count = rules.countNewSOS(board, 2, 0)
        assertTrue("Anti-diagonal SOS should be detected", count >= 1)
    }

    @Test
    fun `detect SOS when placing O in the middle`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 0, SOSRules.PIECE_S)
            .place(0, 2, SOSRules.PIECE_S)
            // Place O in the middle
            .place(0, 1, SOSRules.PIECE_O)

        val count = rules.countNewSOS(board, 0, 1)
        assertTrue("O in middle should complete SOS", count >= 1)
    }

    @Test
    fun `detect SOS when placing S at beginning`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 1, SOSRules.PIECE_O)
            .place(0, 2, SOSRules.PIECE_S)
            // Place S at beginning
            .place(0, 0, SOSRules.PIECE_S)

        val count = rules.countNewSOS(board, 0, 0)
        assertTrue("S at start should complete SOS", count >= 1)
    }

    @Test
    fun `no SOS for incomplete pattern`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 0, SOSRules.PIECE_S)
            .place(0, 1, SOSRules.PIECE_S) // SS not SOS

        val count = rules.countNewSOS(board, 0, 1)
        assertEquals("SS is not an SOS", 0, count)
    }

    @Test
    fun `detect SOS in all 8 directions from S`() {
        // Center an S at (2,2), surround with O-S patterns
        var board = GridBoard(5, 5)
        // Right: S(2,2) O(2,3) S(2,4)
        board = board.place(2, 3, SOSRules.PIECE_O).place(2, 4, SOSRules.PIECE_S)
        // Left: S(2,0) O(2,1) S(2,2)
        board = board.place(2, 1, SOSRules.PIECE_O).place(2, 0, SOSRules.PIECE_S)
        // Down: S(2,2) O(3,2) S(4,2)
        board = board.place(3, 2, SOSRules.PIECE_O).place(4, 2, SOSRules.PIECE_S)
        // Up: S(0,2) O(1,2) S(2,2)
        board = board.place(1, 2, SOSRules.PIECE_O).place(0, 2, SOSRules.PIECE_S)
        // Now place S at center
        board = board.place(2, 2, SOSRules.PIECE_S)

        val count = rules.countNewSOS(board, 2, 2)
        assertTrue("S at center should detect multiple SOS patterns", count >= 4)
    }

    // ── Extra Turn ──────────────────────────────────────────────────────

    @Test
    fun `extra turn granted when SOS is formed`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 0, SOSRules.PIECE_S)
            .place(0, 1, SOSRules.PIECE_O)
            .place(0, 2, SOSRules.PIECE_S)

        val move = Move(1, Position(0, 2), MoveType.PLACE,
            mapOf(SOSRules.META_PIECE_TYPE to SOSRules.PIECE_S))
        val record = MoveRecord(move = move)
        val state = baseState.copy(
            boardData = board,
            moveHistory = listOf(record)
        )

        val shouldAdvance = rules.shouldAdvanceTurn(state)
        assertFalse("Player should get extra turn after forming SOS", shouldAdvance)
    }

    @Test
    fun `no extra turn when SOS is not formed`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 0, SOSRules.PIECE_S)

        val move = Move(1, Position(0, 0), MoveType.PLACE,
            mapOf(SOSRules.META_PIECE_TYPE to SOSRules.PIECE_S))
        val record = MoveRecord(move = move)
        val state = baseState.copy(
            boardData = board,
            moveHistory = listOf(record)
        )

        val shouldAdvance = rules.shouldAdvanceTurn(state)
        assertTrue("Turn should advance normally", shouldAdvance)
    }

    // ── Move Validation ─────────────────────────────────────────────────

    @Test
    fun `valid move with S piece type`() {
        val move = Move(0, Position(0, 0), MoveType.PLACE,
            mapOf(SOSRules.META_PIECE_TYPE to SOSRules.PIECE_S))
        assertTrue(rules.isValidMove(baseState, move))
    }

    @Test
    fun `valid move with O piece type`() {
        val move = Move(0, Position(0, 0), MoveType.PLACE,
            mapOf(SOSRules.META_PIECE_TYPE to SOSRules.PIECE_O))
        assertTrue(rules.isValidMove(baseState, move))
    }

    @Test
    fun `move without piece type is rejected`() {
        val move = Move(0, Position(0, 0), MoveType.PLACE)
        assertFalse(rules.isValidMove(baseState, move))
    }

    @Test
    fun `move on occupied cell is rejected`() {
        val board = GridBoard(5, 5).place(0, 0, SOSRules.PIECE_S)
        val state = baseState.copy(boardData = board)
        val move = Move(0, Position(0, 0), MoveType.PLACE,
            mapOf(SOSRules.META_PIECE_TYPE to SOSRules.PIECE_O))
        assertFalse(rules.isValidMove(state, move))
    }

    // ── Scoring ──────────────────────────────────────────────────────────

    @Test
    fun `scores update correctly after SOS`() {
        var board = GridBoard(5, 5)
        board = board.place(0, 0, SOSRules.PIECE_S)
            .place(0, 1, SOSRules.PIECE_O)
            .place(0, 2, SOSRules.PIECE_S)

        val move = Move(1, Position(0, 2), MoveType.PLACE,
            mapOf(SOSRules.META_PIECE_TYPE to SOSRules.PIECE_S))
        val state = baseState.copy(
            boardData = board,
            scores = mapOf(1 to 0, 2 to 0)
        )

        val newScores = rules.computeScores(state, move)
        assertTrue("Player 1 should have scored", (newScores[1] ?: 0) > 0)
    }
}
