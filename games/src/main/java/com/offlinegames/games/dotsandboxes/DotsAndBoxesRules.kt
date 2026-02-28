package com.offlinegames.games.dotsandboxes

import com.offlinegames.core.*

/**
 * Rules for Dots & Boxes.
 *
 * Encoding:
 *   A 4×4 box grid is represented on a 9×9 GridBoard:
 *   - Dots at even-even coordinates (0,0), (0,2), ..., (8,8)
 *   - Horizontal edges at even-odd: (0,1), (0,3), ...
 *   - Vertical edges at odd-even: (1,0), (3,0), ...
 *   - Box centers at odd-odd: (1,1), (3,3), ...
 *
 * Cell values:
 *   0 = empty / unclaimed
 *   1 = edge drawn by player 1 / box owned by player 1
 *   2 = edge drawn by player 2 / box owned by player 2
 *   5 = dot marker (placed at init)
 *   Non-zero at edge position = edge has been drawn
 *
 * A move is placing an EDGE at a horizontal or vertical edge position.
 * Completing a box (all 4 edges drawn) claims it and grants another turn.
 */
class DotsAndBoxesRules : GameRules<GridBoard> {

    companion object {
        /** Number of boxes per side. */
        const val BOX_COUNT = 4
        /** Grid dimension: 2 * BOX_COUNT + 1. */
        const val GRID_SIZE = 2 * BOX_COUNT + 1
        /** Marker value for dots. */
        const val DOT = 5

        /**
         * Create the initial board with dots placed.
         */
        fun createInitialBoard(): GridBoard {
            val cells = Array(GRID_SIZE) { IntArray(GRID_SIZE) }
            for (r in 0 until GRID_SIZE step 2) {
                for (c in 0 until GRID_SIZE step 2) {
                    cells[r][c] = DOT
                }
            }
            return GridBoard(GRID_SIZE, GRID_SIZE, cells)
        }

        /** true if (r,c) is a horizontal edge position. */
        fun isHorizontalEdge(r: Int, c: Int): Boolean = r % 2 == 0 && c % 2 == 1

        /** true if (r,c) is a vertical edge position. */
        fun isVerticalEdge(r: Int, c: Int): Boolean = r % 2 == 1 && c % 2 == 0

        /** true if (r,c) is any edge position. */
        fun isEdge(r: Int, c: Int): Boolean = isHorizontalEdge(r, c) || isVerticalEdge(r, c)

        /** true if (r,c) is a box center. */
        fun isBoxCenter(r: Int, c: Int): Boolean = r % 2 == 1 && c % 2 == 1
    }

    override fun isValidMove(state: GameState, move: Move): Boolean {
        if (move.type != MoveType.EDGE) return false
        val r = move.position.row
        val c = move.position.col
        if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) return false
        if (!isEdge(r, c)) return false
        val board = state.boardData as GridBoard
        return board.get(r, c) == 0
    }

    override fun getLegalMoves(state: GameState, player: Player): List<Move> {
        val board = state.boardData as GridBoard
        val moves = mutableListOf<Move>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (isEdge(r, c) && board.get(r, c) == 0) {
                    moves.add(Move(player.id, Position(r, c), MoveType.EDGE))
                }
            }
        }
        return moves
    }

    /**
     * Place the edge, then check and claim any completed boxes.
     */
    override fun applyMove(boardData: GridBoard, move: Move, player: Player): GridBoard {
        var board = boardData.place(move.position.row, move.position.col, player.id)

        // Check adjacent boxes and claim completed ones
        val adjacentBoxes = getAdjacentBoxes(move.position.row, move.position.col)
        for ((br, bc) in adjacentBoxes) {
            if (isBoxComplete(board, br, bc)) {
                board = board.place(br, bc, player.id)
            }
        }
        return board
    }

    /**
     * Returns the box center coordinates adjacent to the edge at (r, c).
     */
    private fun getAdjacentBoxes(r: Int, c: Int): List<Pair<Int, Int>> {
        val boxes = mutableListOf<Pair<Int, Int>>()
        if (isHorizontalEdge(r, c)) {
            // Horizontal edge: box above (r-1, c) and box below (r+1, c)
            if (r > 0) boxes.add(r - 1 to c)
            if (r < GRID_SIZE - 1) boxes.add(r + 1 to c)
        } else if (isVerticalEdge(r, c)) {
            // Vertical edge: box left (r, c-1) and box right (r, c+1)
            if (c > 0) boxes.add(r to c - 1)
            if (c < GRID_SIZE - 1) boxes.add(r to c + 1)
        }
        return boxes
    }

    /**
     * Check if all 4 edges of the box centered at (br, bc) are drawn.
     */
    fun isBoxComplete(board: GridBoard, br: Int, bc: Int): Boolean {
        if (!isBoxCenter(br, bc)) return false
        val top = board.get(br - 1, bc)    // horizontal edge above
        val bottom = board.get(br + 1, bc) // horizontal edge below
        val left = board.get(br, bc - 1)   // vertical edge left
        val right = board.get(br, bc + 1)  // vertical edge right
        return top != 0 && bottom != 0 && left != 0 && right != 0
    }

    /**
     * Count how many new boxes were completed by the last move.
     */
    fun countNewBoxes(boardBefore: GridBoard, boardAfter: GridBoard, edgeR: Int, edgeC: Int): Int {
        val adjacent = getAdjacentBoxes(edgeR, edgeC)
        var count = 0
        for ((br, bc) in adjacent) {
            if (!isBoxCenter(br, bc)) continue
            val wasClaimed = boardBefore.get(br, bc) != 0
            val isClaimed = boardAfter.get(br, bc) != 0
            if (!wasClaimed && isClaimed) count++
        }
        return count
    }

    /**
     * Count edges around a box center. Returns 0–4.
     */
    fun countEdges(board: GridBoard, br: Int, bc: Int): Int {
        if (!isBoxCenter(br, bc)) return 0
        var count = 0
        if (board.get(br - 1, bc) != 0) count++
        if (board.get(br + 1, bc) != 0) count++
        if (board.get(br, bc - 1) != 0) count++
        if (board.get(br, bc + 1) != 0) count++
        return count
    }

    /**
     * Player continues if they completed a box with their move.
     */
    override fun shouldAdvanceTurn(state: GameState): Boolean {
        val lastMove = state.moveHistory.lastOrNull()?.move ?: return true
        val board = state.boardData as GridBoard
        val adjacent = getAdjacentBoxes(lastMove.position.row, lastMove.position.col)
        // If any adjacent box is now owned by the last player, they completed it
        for ((br, bc) in adjacent) {
            if (isBoxCenter(br, bc) && board.get(br, bc) == lastMove.playerId) {
                return false // extra turn
            }
        }
        return true
    }

    /**
     * Score = number of boxes owned by each player.
     */
    override fun computeScores(state: GameState, move: Move): Map<Int, Int> {
        val board = state.boardData as GridBoard
        val scores = mutableMapOf<Int, Int>()
        for (r in 1 until GRID_SIZE step 2) {
            for (c in 1 until GRID_SIZE step 2) {
                val owner = board.get(r, c)
                if (owner == 1 || owner == 2) {
                    scores[owner] = (scores[owner] ?: 0) + 1
                }
            }
        }
        return scores
    }

    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as GridBoard
        // Check if all edges are drawn
        val allEdgesDrawn = (0 until GRID_SIZE).all { r ->
            (0 until GRID_SIZE).all { c ->
                if (isEdge(r, c)) board.get(r, c) != 0 else true
            }
        }
        if (!allEdgesDrawn) return GameResult.IN_PROGRESS

        val scores = state.scores
        val p1 = scores[1] ?: 0
        val p2 = scores[2] ?: 0
        return when {
            p1 > p2 -> GameResult.WIN
            p2 > p1 -> GameResult.WIN
            else -> GameResult.DRAW
        }
    }

    fun getWinner(state: GameState): Player? {
        if (state.result != GameResult.WIN) return null
        val scores = state.scores
        val p1 = scores[1] ?: 0
        val p2 = scores[2] ?: 0
        return when {
            p1 > p2 -> state.players.find { it.id == 1 }
            p2 > p1 -> state.players.find { it.id == 2 }
            else -> null
        }
    }
}
