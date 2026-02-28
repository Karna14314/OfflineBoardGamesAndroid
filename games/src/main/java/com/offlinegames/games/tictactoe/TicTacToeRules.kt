package com.offlinegames.games.tictactoe

import com.offlinegames.core.*

/**
 * All game rules for TicTacToe.
 *
 * Implements [GameRules]<[GridBoard]> and handles:
 * - Move validation (occupied / out-of-bounds)
 * - Legal move enumeration (all empty cells)
 * - Win and draw detection
 * - Board state transitions
 */
class TicTacToeRules : GameRules<GridBoard> {

    override fun isValidMove(state: GameState, move: Move): Boolean {
        if (move.type != MoveType.PLACE) return false
        val board = state.boardData as GridBoard
        if (move.position.row !in 0..2 || move.position.col !in 0..2) return false
        return board.isEmpty(move.position.row, move.position.col)
    }

    override fun getLegalMoves(state: GameState, player: Player): List<Move> {
        val board = state.boardData as GridBoard
        val moves = mutableListOf<Move>()
        for (r in 0..2) {
            for (c in 0..2) {
                if (board.isEmpty(r, c)) {
                    moves.add(Move(player.id, Position(r, c)))
                }
            }
        }
        return moves
    }

    override fun applyMove(boardData: GridBoard, move: Move, player: Player): GridBoard {
        require(move.type == MoveType.PLACE)
        return boardData.place(move.position.row, move.position.col, player.id)
    }

    /**
     * Check all winning lines and draw condition.
     *
     * Win detection occurs AFTER the move is applied, so we look at
     * the player who just moved (the previous player in the history).
     */
    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as GridBoard
        val lastRecord = state.moveHistory.lastOrNull() ?: return GameResult.IN_PROGRESS
        val lastPlayerId = lastRecord.move.playerId

        if (hasWon(board, lastPlayerId)) return GameResult.WIN
        if (board.isFull()) return GameResult.DRAW
        return GameResult.IN_PROGRESS
    }

    /**
     * Returns true if [playerId] occupies any winning line on [board].
     */
    fun hasWon(board: GridBoard, playerId: Int): Boolean {
        val c = board.cells
        // Rows
        for (r in 0..2) {
            if (c[r][0] == playerId && c[r][1] == playerId && c[r][2] == playerId) return true
        }
        // Columns
        for (col in 0..2) {
            if (c[0][col] == playerId && c[1][col] == playerId && c[2][col] == playerId) return true
        }
        // Diagonals
        if (c[0][0] == playerId && c[1][1] == playerId && c[2][2] == playerId) return true
        if (c[0][2] == playerId && c[1][1] == playerId && c[2][0] == playerId) return true
        return false
    }
}
