package com.offlinegames.games.connect4

import com.offlinegames.core.*

/**
 * Rules for Connect 4.
 *
 * Board: 7 columns × 6 rows (GridBoard(7, 6))
 * Gravity: pieces always fall to the lowest empty row in a column.
 * Win: 4 consecutive pieces horizontally, vertically, or diagonally.
 */
class Connect4Rules : GameRules<GridBoard> {

    companion object {
        const val COLS = 7
        const val ROWS = 6
        const val WIN_LENGTH = 4
    }

    /**
     * A move is valid if:
     * 1. Type is COLUMN_DROP
     * 2. Column is in range [0, COLS)
     * 3. Top row of that column is empty (column not full)
     */
    override fun isValidMove(state: GameState, move: Move): Boolean {
        if (move.type != MoveType.COLUMN_DROP) return false
        val col = move.position.col
        if (col !in 0 until COLS) return false
        val board = state.boardData as GridBoard
        // Column is playable if the top row is empty
        return board.isEmpty(0, col)
    }

    override fun getLegalMoves(state: GameState, player: Player): List<Move> {
        val board = state.boardData as GridBoard
        val moves = mutableListOf<Move>()
        for (c in 0 until COLS) {
            if (board.isEmpty(0, c)) {
                moves.add(Move(player.id, Position(0, c), MoveType.COLUMN_DROP))
            }
        }
        return moves
    }

    /**
     * Apply gravity: find the lowest empty row in the column and place there.
     */
    override fun applyMove(boardData: GridBoard, move: Move, player: Player): GridBoard {
        val col = move.position.col
        val row = findDropRow(boardData, col)
        require(row >= 0) { "Column $col is full" }
        return boardData.place(row, col, player.id)
    }

    /** Find the lowest empty row in [col], or -1 if full. */
    fun findDropRow(board: GridBoard, col: Int): Int {
        for (r in ROWS - 1 downTo 0) {
            if (board.isEmpty(r, col)) return r
        }
        return -1
    }

    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as GridBoard
        val lastRecord = state.moveHistory.lastOrNull() ?: return GameResult.IN_PROGRESS
        val lastPlayerId = lastRecord.move.playerId

        if (hasWon(board, lastPlayerId)) return GameResult.WIN
        if (board.isFull()) return GameResult.DRAW
        return GameResult.IN_PROGRESS
    }

    /**
     * Check if [playerId] has 4 in a row on the board.
     * Scans all possible lines of 4.
     */
    fun hasWon(board: GridBoard, playerId: Int): Boolean {
        val c = board.cells

        // Horizontal
        for (r in 0 until ROWS) {
            for (col in 0..COLS - WIN_LENGTH) {
                if ((0 until WIN_LENGTH).all { c[r][col + it] == playerId }) return true
            }
        }

        // Vertical
        for (col in 0 until COLS) {
            for (r in 0..ROWS - WIN_LENGTH) {
                if ((0 until WIN_LENGTH).all { c[r + it][col] == playerId }) return true
            }
        }

        // Diagonal (top-left to bottom-right)
        for (r in 0..ROWS - WIN_LENGTH) {
            for (col in 0..COLS - WIN_LENGTH) {
                if ((0 until WIN_LENGTH).all { c[r + it][col + it] == playerId }) return true
            }
        }

        // Diagonal (bottom-left to top-right)
        for (r in WIN_LENGTH - 1 until ROWS) {
            for (col in 0..COLS - WIN_LENGTH) {
                if ((0 until WIN_LENGTH).all { c[r - it][col + it] == playerId }) return true
            }
        }

        return false
    }
}
