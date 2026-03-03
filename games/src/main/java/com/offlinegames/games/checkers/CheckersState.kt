package com.offlinegames.games.checkers

import com.offlinegames.core.*

/**
 * Piece representation for Checkers.
 *
 * @param playerId 1 or 2 (player 1 at bottom, player 2 at top)
 * @param type MAN or KING
 * @param row Position row (0-7)
 * @param col Position column (0-7)
 */
data class CheckersPiece(
    val playerId: Int,
    val type: PieceType,
    val row: Int,
    val col: Int
) {
    val isKing: Boolean get() = type == PieceType.KING

    fun withPosition(newRow: Int, newCol: Int): CheckersPiece {
        return copy(row = newRow, col = newCol)
    }

    fun promoted(): CheckersPiece {
        return copy(type = PieceType.KING)
    }
}

/**
 * Checkers board state.
 *
 * 8x8 board where pieces only occupy dark squares.
 * Player 1 pieces start at rows 0-2, Player 2 at rows 5-7.
 *
 * @param pieces List of all pieces on the board
 * @param capturedPieces List of captured pieces (for undo/detailed tracking)
 */
data class CheckersBoard(
    val pieces: List<CheckersPiece> = emptyList(),
    val capturedPieces: List<CheckersPiece> = emptyList()
) {
    companion object {
        const val SIZE = 8
        const val ROWS_PER_PLAYER = 3
    }

    /**
     * Get piece at position, or null if empty.
     */
    fun getPiece(row: Int, col: Int): CheckersPiece? {
        return pieces.find { it.row == row && it.col == col }
    }

    /**
     * Check if position is occupied.
     */
    fun isOccupied(row: Int, col: Int): Boolean {
        return getPiece(row, col) != null
    }

    /**
     * Check if position is valid (on board and dark square).
     */
    fun isValidPosition(row: Int, col: Int): Boolean {
        if (row !in 0 until SIZE || col !in 0 until SIZE) return false
        // Dark squares only: (row + col) % 2 == 1
        return (row + col) % 2 == 1
    }

    /**
     * Move a piece to a new position.
     */
    fun movePiece(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): CheckersBoard {
        val piece = getPiece(fromRow, fromCol) ?: return this
        val newPiece = piece.withPosition(toRow, toCol)

        // Check for king promotion
        val promotedPiece = when {
            piece.playerId == 1 && toRow == SIZE - 1 -> newPiece.promoted()
            piece.playerId == 2 && toRow == 0 -> newPiece.promoted()
            else -> newPiece
        }

        val newPieces = pieces.map {
            if (it.row == fromRow && it.col == fromCol) promotedPiece else it
        }

        return copy(pieces = newPieces)
    }

    /**
     * Capture a piece at the given position.
     */
    fun capturePiece(row: Int, col: Int): CheckersBoard {
        val captured = getPiece(row, col) ?: return this
        val newPieces = pieces.filterNot { it.row == row && it.col == col }
        return copy(
            pieces = newPieces,
            capturedPieces = capturedPieces + captured
        )
    }

    /**
     * Get all pieces for a player.
     */
    fun getPlayerPieces(playerId: Int): List<CheckersPiece> {
        return pieces.filter { it.playerId == playerId }
    }

    /**
     * Count pieces for a player.
     */
    fun countPieces(playerId: Int): Int {
        return pieces.count { it.playerId == playerId }
    }

    /**
     * Count kings for a player.
     */
    fun countKings(playerId: Int): Int {
        return pieces.count { it.playerId == playerId && it.isKing }
    }

    /**
     * Create initial board setup.
     */
    fun createInitial(): CheckersBoard {
        val initialPieces = mutableListOf<CheckersPiece>()

        // Player 1 pieces (bottom, rows 0-2 from their perspective, actually rows 5-7)
        for (row in SIZE - ROWS_PER_PLAYER until SIZE) {
            for (col in 0 until SIZE) {
                if (isValidPosition(row, col)) {
                    initialPieces.add(CheckersPiece(1, PieceType.MAN, row, col))
                }
            }
        }

        // Player 2 pieces (top, rows 0-2)
        for (row in 0 until ROWS_PER_PLAYER) {
            for (col in 0 until SIZE) {
                if (isValidPosition(row, col)) {
                    initialPieces.add(CheckersPiece(2, PieceType.MAN, row, col))
                }
            }
        }

        return CheckersBoard(initialPieces)
    }

    /**
     * Check if a player has any legal moves.
     */
    fun hasLegalMoves(playerId: Int): Boolean {
        val playerPieces = getPlayerPieces(playerId)
        return playerPieces.any { piece ->
            canPieceMove(piece) || canPieceCapture(piece)
        }
    }

    /**
     * Check if a piece can make any move.
     */
    fun canPieceMove(piece: CheckersPiece): Boolean {
        val directions = getMoveDirections(piece)
        for ((dr, dc) in directions) {
            val newRow = piece.row + dr
            val newCol = piece.col + dc
            if (isValidPosition(newRow, newCol) && !isOccupied(newRow, newCol)) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a piece can make any capture.
     */
    fun canPieceCapture(piece: CheckersPiece): Boolean {
        return getCapturePositions(piece).isNotEmpty()
    }

    /**
     * Get valid capture positions for a piece.
     */
    fun getCapturePositions(piece: CheckersPiece): List<Position> {
        val captures = mutableListOf<Position>()
        val directions = getCaptureDirections(piece)

        for ((dr, dc) in directions) {
            val jumpRow = piece.row + dr * 2
            val jumpCol = piece.col + dc * 2
            val midRow = piece.row + dr
            val midCol = piece.col + dc

            if (isValidPosition(jumpRow, jumpCol) &&
                !isOccupied(jumpRow, jumpCol) &&
                isOccupied(midRow, midCol)
            ) {
                val midPiece = getPiece(midRow, midCol)
                if (midPiece != null && midPiece.playerId != piece.playerId) {
                    captures.add(Position(jumpRow, jumpCol))
                }
            }
        }

        return captures
    }

    /**
     * Get move directions for a piece.
     */
    private fun getMoveDirections(piece: CheckersPiece): List<Pair<Int, Int>> {
        return when {
            piece.isKing -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
            piece.playerId == 1 -> listOf(-1 to -1, -1 to 1) // Player 1 moves up (decreasing row)
            else -> listOf(1 to -1, 1 to 1) // Player 2 moves down (increasing row)
        }
    }

    /**
     * Get capture directions for a piece.
     */
    private fun getCaptureDirections(piece: CheckersPiece): List<Pair<Int, Int>> {
        return if (piece.isKing) {
            listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        } else {
            // Men can capture in all diagonal directions
            listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        }
    }
}

/**
 * Represents a complete move in Checkers (may include multiple jumps).
 */
data class CheckersMove(
    val from: Position,
    val to: Position,
    val capturedPositions: List<Position> = emptyList(),
    val isKingPromotion: Boolean = false
)

/**
 * Checkers-specific MVI state.
 */
data class CheckersState(
    val gameState: GameState,
    val vsAi: Boolean = false,
    val showResultDialog: Boolean = false,
    val selectedPiece: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val isChainCapture: Boolean = false, // Player must continue capturing
    val lastMove: CheckersMove? = null
) {
    val board: CheckersBoard get() = gameState.boardData as CheckersBoard
    val currentPlayer: Player get() = gameState.currentPlayer
    val result: GameResult get() = gameState.result
    val isOngoing: Boolean get() = gameState.isOngoing
}
