package com.offlinegames.games.twenty48

import com.offlinegames.core.*

/**
 * 2048-specific board data structure.
 *
 * Each cell contains a tile value (0 = empty, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048...)
 * The board is immutable - all operations return new instances.
 *
 * @param tiles 4x4 array of tile values
 * @param score Current score for this board state
 */
data class Game2048Board(
    val tiles: Array<IntArray> = Array(4) { IntArray(4) { 0 } },
    val score: Int = 0
) {
    companion object {
        const val SIZE = 4
        const val WINNING_TILE = 2048
    }

    /**
     * Get tile value at position (0 if empty).
     */
    fun get(row: Int, col: Int): Int = tiles[row][col]

    /**
     * Check if a cell is empty.
     */
    fun isEmpty(row: Int, col: Int): Boolean = tiles[row][col] == 0

    /**
     * Create a new board with a tile placed at the given position.
     */
    fun place(row: Int, col: Int, value: Int): Game2048Board {
        val newTiles = Array(SIZE) { r -> tiles[r].copyOf() }
        newTiles[row][col] = value
        return Game2048Board(newTiles, score)
    }

    /**
     * Create a new board with updated score.
     */
    fun withScore(newScore: Int): Game2048Board {
        return Game2048Board(tiles, newScore)
    }

    /**
     * Check if the board has any empty cells.
     */
    fun hasEmptyCells(): Boolean {
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (tiles[r][c] == 0) return true
            }
        }
        return false
    }

    /**
     * Get all empty cell positions.
     */
    fun getEmptyCells(): List<Position> {
        val empty = mutableListOf<Position>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (tiles[r][c] == 0) empty.add(Position(r, c))
            }
        }
        return empty
    }

    /**
     * Check if any tile has reached the winning value.
     */
    fun hasWon(): Boolean {
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (tiles[r][c] >= WINNING_TILE) return true
            }
        }
        return false
    }

    /**
     * Check if any moves are possible (adjacent merges or empty cells).
     */
    fun hasPossibleMoves(): Boolean {
        // Check for empty cells
        if (hasEmptyCells()) return true

        // Check for possible merges (horizontal and vertical)
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val value = tiles[r][c]
                // Check right neighbor
                if (c < SIZE - 1 && tiles[r][c + 1] == value) return true
                // Check bottom neighbor
                if (r < SIZE - 1 && tiles[r + 1][c] == value) return true
            }
        }
        return false
    }

    /**
     * Create a deep copy of this board.
     */
    fun copy(): Game2048Board {
        val newTiles = Array(SIZE) { r -> tiles[r].copyOf() }
        return Game2048Board(newTiles, score)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Game2048Board) return false
        if (score != other.score) return false
        return tiles.contentDeepEquals(other.tiles)
    }

    override fun hashCode(): Int {
        var result = score
        result = 31 * result + tiles.contentDeepHashCode()
        return result
    }
}

/**
 * Snapshot of board state for undo functionality.
 */
data class Game2048Snapshot(
    val board: Game2048Board,
    val moveCount: Int
)

/**
 * 2048-specific MVI state.
 * Wraps [GameState] with typed board access and 2048-specific properties.
 */
data class Game2048State(
    val gameState: GameState,
    val showResultDialog: Boolean = false,
    val undoStack: List<Game2048Snapshot> = emptyList(),
    val maxUndoDepth: Int = 1  // 2048 allows only 1 undo
) {
    val board: Game2048Board get() = gameState.boardData as Game2048Board
    val currentScore: Int get() = board.score
    val result: GameResult get() = gameState.result
    val isOngoing: Boolean get() = gameState.isOngoing
    val moveCount: Int get() = gameState.moveHistory.size
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    /**
     * Check if the game is won (has a 2048 tile).
     */
    fun hasWinningTile(): Boolean = board.hasWon()
}
