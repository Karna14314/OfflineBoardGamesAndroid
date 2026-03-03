package com.offlinegames.games.minesweeper

import com.offlinegames.core.*

/**
 * Cell state for Minesweeper.
 *
 * @param isMine true if this cell contains a mine
 * @param visibility current visibility state of the cell
 * @param adjacentMines count of adjacent mines (-1 if not calculated yet)
 */
data class MinesweeperCell(
    val isMine: Boolean = false,
    val visibility: CellVisibility = CellVisibility.HIDDEN,
    val adjacentMines: Int = -1
) {
    val isRevealed: Boolean get() = visibility == CellVisibility.REVEALED
    val isFlagged: Boolean get() = visibility == CellVisibility.FLAGGED
}

/**
 * Minesweeper board state.
 *
 * @param width board width
 * @param height board height
 * @param mineCount total number of mines
 * @param cells 2D array of cell states
 * @param firstClickCompleted true after first click (mines are placed after first click)
 * @param flagsPlaced number of flags currently placed
 */
data class MinesweeperBoard(
    val width: Int = 9,
    val height: Int = 9,
    val mineCount: Int = 10,
    val cells: Array<Array<MinesweeperCell>> = Array(height) {
        Array(width) { MinesweeperCell() }
    },
    val firstClickCompleted: Boolean = false,
    val flagsPlaced: Int = 0,
    val revealedCount: Int = 0
) {
    companion object {
        const val DEFAULT_WIDTH = 9
        const val DEFAULT_HEIGHT = 9
        const val DEFAULT_MINES = 10

        const val MEDIUM_WIDTH = 16
        const val MEDIUM_HEIGHT = 16
        const val MEDIUM_MINES = 40

        const val HARD_WIDTH = 30
        const val HARD_HEIGHT = 16
        const val HARD_MINES = 99
    }

    /**
     * Get cell at position.
     */
    fun getCell(row: Int, col: Int): MinesweeperCell {
        return if (isValidPosition(row, col)) cells[row][col] else MinesweeperCell()
    }

    /**
     * Check if position is valid.
     */
    fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until height && col in 0 until width
    }

    /**
     * Create a new board with a cell modified.
     */
    fun withCell(row: Int, col: Int, cell: MinesweeperCell): MinesweeperBoard {
        val newCells = Array(height) { r ->
            Array(width) { c ->
                if (r == row && c == col) cell else cells[r][c]
            }
        }

        val revealedDelta = if (cell.isRevealed && !cells[row][col].isRevealed) 1
        else if (!cell.isRevealed && cells[row][col].isRevealed) -1
        else 0

        val flagDelta = if (cell.isFlagged && !cells[row][col].isFlagged) 1
        else if (!cell.isFlagged && cells[row][col].isFlagged) -1
        else 0

        return copy(
            cells = newCells,
            flagsPlaced = flagsPlaced + flagDelta,
            revealedCount = revealedCount + revealedDelta
        )
    }

    /**
     * Create a new board with multiple cells modified.
     */
    fun withCells(modifications: Map<Pair<Int, Int>, MinesweeperCell>): MinesweeperBoard {
        var newBoard = this
        for ((pos, cell) in modifications) {
            newBoard = newBoard.withCell(pos.first, pos.second, cell)
        }
        return newBoard
    }

    /**
     * Check if the game is won (all non-mine cells revealed).
     */
    fun isWin(): Boolean {
        return revealedCount == (width * height - mineCount)
    }

    /**
     * Check if the game is lost (a mine was revealed).
     */
    fun isLoss(): Boolean {
        for (r in 0 until height) {
            for (c in 0 until width) {
                if (cells[r][c].isMine && cells[r][c].isRevealed) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Get adjacent positions.
     */
    fun getAdjacentPositions(row: Int, col: Int): List<Position> {
        val adjacent = mutableListOf<Position>()
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = row + dr
                val nc = col + dc
                if (isValidPosition(nr, nc)) {
                    adjacent.add(Position(nr, nc))
                }
            }
        }
        return adjacent
    }

    /**
     * Count adjacent mines for a cell.
     */
    fun countAdjacentMines(row: Int, col: Int): Int {
        return getAdjacentPositions(row, col).count { getCell(it.row, it.col).isMine }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MinesweeperBoard) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (mineCount != other.mineCount) return false
        if (firstClickCompleted != other.firstClickCompleted) return false
        if (flagsPlaced != other.flagsPlaced) return false
        if (revealedCount != other.revealedCount) return false
        return cells.contentDeepEquals(other.cells)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + mineCount
        result = 31 * result + cells.contentDeepHashCode()
        result = 31 * result + firstClickCompleted.hashCode()
        result = 31 * result + flagsPlaced
        result = 31 * result + revealedCount
        return result
    }
}

/**
 * Minesweeper-specific MVI state.
 */
data class MinesweeperState(
    val gameState: GameState,
    val showResultDialog: Boolean = false,
    val timerSeconds: Int = 0,
    val isPaused: Boolean = false
) {
    val board: MinesweeperBoard get() = gameState.boardData as MinesweeperBoard
    val result: GameResult get() = gameState.result
    val isOngoing: Boolean get() = gameState.isOngoing
    val minesRemaining: Int get() = board.mineCount - board.flagsPlaced
}

/**
 * Difficulty presets for Minesweeper.
 */
enum class MinesweeperDifficulty(
    val width: Int,
    val height: Int,
    val mineCount: Int
) {
    BEGINNER(9, 9, 10),
    INTERMEDIATE(16, 16, 40),
    EXPERT(30, 16, 99)
}
