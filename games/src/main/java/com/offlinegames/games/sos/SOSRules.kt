package com.offlinegames.games.sos

import com.offlinegames.core.*

/**
 * Rules for the SOS Game.
 *
 * Board: 5×5 grid
 * Players place either S (value 3) or O (value 4) on empty cells.
 * Forming "SOS" in any direction scores a point and grants an extra turn.
 * Game ends when the board is full; highest score wins.
 *
 * Piece encoding:
 *   0 = empty
 *   3 = S (regardless of which player placed it)
 *   4 = O (regardless of which player placed it)
 * Player ownership is tracked only through move history.
 */
class SOSRules : GameRules<GridBoard> {

    companion object {
        const val SIZE = 5
        const val PIECE_S = 3
        const val PIECE_O = 4
        const val META_PIECE_TYPE = "pieceType"

        /** All 8 directions as (dr, dc) pairs for line scanning. */
        private val DIRECTIONS = arrayOf(
            intArrayOf(0, 1),   // right
            intArrayOf(0, -1),  // left
            intArrayOf(1, 0),   // down
            intArrayOf(-1, 0),  // up
            intArrayOf(1, 1),   // down-right
            intArrayOf(-1, -1), // up-left
            intArrayOf(1, -1),  // down-left
            intArrayOf(-1, 1)   // up-right
        )

        /** Paired directions forming lines through a center. */
        private val LINE_PAIRS = arrayOf(
            intArrayOf(0, 1) to intArrayOf(0, -1),     // horizontal
            intArrayOf(1, 0) to intArrayOf(-1, 0),     // vertical
            intArrayOf(1, 1) to intArrayOf(-1, -1),    // diagonal \
            intArrayOf(1, -1) to intArrayOf(-1, 1)     // diagonal /
        )
    }

    override fun isValidMove(state: GameState, move: Move): Boolean {
        if (move.type != MoveType.PLACE) return false
        val pieceType = move.metadata[META_PIECE_TYPE] ?: return false
        if (pieceType != PIECE_S && pieceType != PIECE_O) return false
        val pos = move.position
        if (pos.row !in 0 until SIZE || pos.col !in 0 until SIZE) return false
        val board = state.boardData as GridBoard
        return board.isEmpty(pos.row, pos.col)
    }

    override fun getLegalMoves(state: GameState, player: Player): List<Move> {
        val board = state.boardData as GridBoard
        val moves = mutableListOf<Move>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board.isEmpty(r, c)) {
                    moves.add(Move(player.id, Position(r, c), MoveType.PLACE,
                        mapOf(META_PIECE_TYPE to PIECE_S)))
                    moves.add(Move(player.id, Position(r, c), MoveType.PLACE,
                        mapOf(META_PIECE_TYPE to PIECE_O)))
                }
            }
        }
        return moves
    }

    override fun applyMove(boardData: GridBoard, move: Move, player: Player): GridBoard {
        val pieceType = move.metadata[META_PIECE_TYPE]
            ?: error("SOS move must have pieceType in metadata")
        return boardData.place(move.position.row, move.position.col, pieceType)
    }

    /**
     * Count how many new SOS patterns the last move created.
     *
     * An SOS is: S-O-S in a line of 3 consecutive cells.
     * The placed piece can be at any of the three positions.
     *
     * Optional overrides allow checking for SOS patterns without creating new GridBoard instances.
     */
    fun countNewSOS(
        board: GridBoard,
        row: Int,
        col: Int,
        pieceOverride: Int? = null,
        otherRow: Int = -1,
        otherCol: Int = -1,
        otherPiece: Int = 0
    ): Int {
        val placed = pieceOverride ?: board.get(row, col)
        var count = 0

        fun getPiece(r: Int, c: Int): Int {
            if (r == row && c == col && pieceOverride != null) return pieceOverride
            if (r == otherRow && c == otherCol) return otherPiece
            return board.get(r, c)
        }

        when (placed) {
            PIECE_S -> {
                // This S could be the start or end of an SOS
                for (dir in DIRECTIONS) {
                    val mr = row + dir[0]
                    val mc = col + dir[1]
                    val er = row + 2 * dir[0]
                    val ec = col + 2 * dir[1]
                    if (mr in 0 until SIZE && mc in 0 until SIZE &&
                        er in 0 until SIZE && ec in 0 until SIZE) {
                        if (getPiece(mr, mc) == PIECE_O && getPiece(er, ec) == PIECE_S) {
                            count++
                        }
                    }
                }
            }
            PIECE_O -> {
                // This O could be the center of an SOS
                for ((fwd, bwd) in LINE_PAIRS) {
                    val sr = row + bwd[0]
                    val sc = col + bwd[1]
                    val er = row + fwd[0]
                    val ec = col + fwd[1]
                    if (sr in 0 until SIZE && sc in 0 until SIZE &&
                        er in 0 until SIZE && ec in 0 until SIZE) {
                        if (getPiece(sr, sc) == PIECE_S && getPiece(er, ec) == PIECE_S) {
                            count++
                        }
                    }
                }
            }
        }
        return count
    }

    /**
     * After placing, if SOS was formed → same player continues (extra turn).
     */
    override fun shouldAdvanceTurn(state: GameState): Boolean {
        val lastMove = state.moveHistory.lastOrNull()?.move ?: return true
        val board = state.boardData as GridBoard
        val newSOS = countNewSOS(board, lastMove.position.row, lastMove.position.col)
        return newSOS == 0
    }

    /**
     * Update scores: add any new SOS count to the player who just moved.
     */
    override fun computeScores(state: GameState, move: Move): Map<Int, Int> {
        val board = state.boardData as GridBoard
        val newSOS = countNewSOS(board, move.position.row, move.position.col)
        if (newSOS == 0) return state.scores
        val scores = state.scores.toMutableMap()
        scores[move.playerId] = (scores[move.playerId] ?: 0) + newSOS
        return scores
    }

    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as GridBoard
        if (!board.isFull()) return GameResult.IN_PROGRESS

        // Board is full — determine winner by score
        val scores = state.scores
        val p1Score = scores[1] ?: 0
        val p2Score = scores[2] ?: 0
        return when {
            p1Score > p2Score -> GameResult.WIN
            p2Score > p1Score -> GameResult.WIN
            else -> GameResult.DRAW
        }
    }

    /**
     * When the game ends by score, the winner is the player with the higher score.
     * This is used by the GameState.winner() function via the last move.
     * For scoring games, we need a different mechanism.
     */
    fun getWinner(state: GameState): Player? {
        if (state.result != GameResult.WIN) return null
        val scores = state.scores
        val p1Score = scores[1] ?: 0
        val p2Score = scores[2] ?: 0
        return when {
            p1Score > p2Score -> state.players[0]
            p2Score > p1Score -> state.players[1]
            else -> null
        }
    }
}
