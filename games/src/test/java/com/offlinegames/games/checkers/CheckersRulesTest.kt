package com.offlinegames.games.checkers

import com.offlinegames.core.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Checkers game rules.
 *
 * Tests cover:
 * - Forced capture enforcement
 * - King promotion
 * - Chain capture correctness
 * - Move validation
 */
class CheckersRulesTest {

    private val rules = CheckersRules()

    // ═══════════════════════════════════════════════════════════════════════
    // Forced Capture Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `forced capture - must capture when available`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 5, 1),  // Player 1 piece
            CheckersPiece(2, PieceType.MAN, 4, 2),  // Player 2 piece (can be captured)
            CheckersPiece(2, PieceType.MAN, 2, 4)   // Another Player 2 piece
        )

        val state = createGameState(board, 1)

        // Player 1 must capture the piece at (4, 2)
        val captureMoves = rules.getAllCaptureMoves(board, 1)
        assertTrue("Should have capture moves", captureMoves.isNotEmpty())

        // Regular moves should not be available
        val allMoves = rules.getLegalMoves(state, Player(1, "", true))
        assertTrue("All moves should be captures", allMoves.all {
            val toRow = it.metadata["toRow"] ?: return@all false
 kotlin.math.abs(toRow - it.position.row) == 2
        })
    }

    @Test
    fun `forced capture - invalid if capture available but not taken`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 5, 1),
            CheckersPiece(2, PieceType.MAN, 4, 2),  // Can be captured
            CheckersPiece(1, PieceType.MAN, 3, 3)   // Can make regular move
        )

        // Try to make regular move while capture is available
        val regularMove = CheckersMove(
            from = Position(3, 3),
            to = Position(2, 4)
        )

        val state = createGameState(board, 1)
        val isValid = rules.isValidCheckersMove(board, board.getPiece(3, 3)!!, regularMove, 1)

        assertFalse("Should not allow regular move when capture available", isValid)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // King Promotion Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `king promotion - player 1 reaches opposite end`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 1, 1)  // One step from promotion
        )

        val state = createGameState(board, 1)
        val move = CheckersMove(from = Position(1, 1), to = Position(0, 0))

        val newBoard = applyMove(board, move, 1)

        val piece = newBoard.getPiece(0, 0)
        assertNotNull("Piece should exist", piece)
        assertTrue("Piece should be king", piece!!.isKing)
    }

    @Test
    fun `king promotion - player 2 reaches opposite end`() {
        val board = createBoardWithPieces(
            CheckersPiece(2, PieceType.MAN, 6, 6)  // One step from promotion
        )

        val move = CheckersMove(from = Position(6, 6), to = Position(7, 7))
        val newBoard = applyMove(board, move, 2)

        val piece = newBoard.getPiece(7, 7)
        assertNotNull("Piece should exist", piece)
        assertTrue("Piece should be king", piece!!.isKing)
    }

    @Test
    fun `king promotion during capture chain`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 2, 2),
            CheckersPiece(2, PieceType.MAN, 1, 3),  // Can be captured
            CheckersPiece(2, PieceType.MAN, 1, 1)   // Can be captured
        )

        // Capture to promotion row
        val move = CheckersMove(
            from = Position(2, 2),
            to = Position(0, 4),
            capturedPositions = listOf(Position(1, 3))
        )

        var newBoard = board.capturePiece(1, 3)
        newBoard = newBoard.movePiece(2, 2, 0, 4)

        val piece = newBoard.getPiece(0, 4)
        assertTrue("Piece should be king after promotion", piece?.isKing == true)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Chain Capture Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `chain capture - can continue after first capture`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 5, 1),
            CheckersPiece(2, PieceType.MAN, 4, 2),  // First capture
            CheckersPiece(2, PieceType.MAN, 2, 4)   // Second capture available
        )

        val piece = board.getPiece(5, 1)!!
        val chains = rules.getChainCaptures(board, piece)

        assertTrue("Should have chain captures", chains.isNotEmpty())

        // Find longest chain
        val longestChain = chains.maxByOrNull { it.size }
        assertTrue("Should be able to capture both pieces", longestChain!!.size >= 2)
    }

    @Test
    fun `chain capture - king can capture backwards`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.KING, 3, 3),
            CheckersPiece(2, PieceType.MAN, 4, 4),  // Forward capture
            CheckersPiece(2, PieceType.MAN, 2, 2)   // Backward capture
        )

        val piece = board.getPiece(3, 3)!!
        val chains = rules.getChainCaptures(board, piece)

        assertTrue("King should have chain captures", chains.isNotEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Move Validation Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `valid move - diagonal forward for man`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 5, 1)
        )

        val piece = board.getPiece(5, 1)!!
        val moves = rules.getPieceMoves(board, piece)

        assertEquals("Should have 2 valid moves", 2, moves.size)
        assertTrue("Should be able to move to (4, 0)",
            moves.any { it.to == Position(4, 0) })
        assertTrue("Should be able to move to (4, 2)",
            moves.any { it.to == Position(4, 2) })
    }

    @Test
    fun `invalid move - backwards for man`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 5, 1)
        )

        val piece = board.getPiece(5, 1)!!
        val moves = rules.getPieceMoves(board, piece)

        assertFalse("Should not move backwards",
            moves.any { it.to.row > piece.row })
    }

    @Test
    fun `valid move - any diagonal for king`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.KING, 4, 4)
        )

        val piece = board.getPiece(4, 4)!!
        val moves = rules.getPieceMoves(board, piece)

        assertEquals("King should have 4 moves", 4, moves.size)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Win/Loss Detection Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `win when opponent has no pieces`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 5, 1)
            // Player 2 has no pieces
        )

        val state = createGameState(board, 1)
        val result = rules.evaluateResult(state)

        assertEquals(GameResult.WIN, result)
    }

    @Test
    fun `win when opponent has no legal moves`() {
        val board = createBoardWithPieces(
            CheckersPiece(1, PieceType.MAN, 5, 1),
            CheckersPiece(1, PieceType.MAN, 5, 3),
            CheckersPiece(2, PieceType.MAN, 4, 2),  // Blocked
            CheckersPiece(2, PieceType.MAN, 4, 4)   // Blocked
        )

        val state = createGameState(board, 2) // Player 2's turn but blocked
        val result = rules.evaluateResult(state)

        assertEquals(GameResult.WIN, result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════

    private fun createBoardWithPieces(vararg pieces: CheckersPiece): CheckersBoard {
        return CheckersBoard(pieces.toList())
    }

    private fun createGameState(board: CheckersBoard, currentPlayerId: Int): GameState {
        return GameState(
            gameId = "test",
            players = listOf(Player(1, "White", true), Player(2, "Black", true)),
            currentPlayer = Player(currentPlayerId, "", true),
            boardData = board,
            result = GameResult.IN_PROGRESS
        )
    }

    private fun applyMove(board: CheckersBoard, move: CheckersMove, playerId: Int): CheckersBoard {
        return board.movePiece(move.from.row, move.from.col, move.to.row, move.to.col)
    }

    // Extension to access internal method for testing
    private fun CheckersRules.isValidCheckersMove(
        board: CheckersBoard,
        piece: CheckersPiece,
        move: CheckersMove,
        playerId: Int
    ): Boolean {
        val state = createGameState(board, playerId)
        val checkersMove = move.copy()
        return getValidMovesForPiece(board, piece, playerId).any { it.to == checkersMove.to }
    }
}
