package com.offlinegames.core

/**
 * Reusable board representation for games based on 2D grids.
 * Intentionally immutable – place returns a new instance.
 */
data class GridBoard(
    val width: Int,
    val height: Int,
    val cells: Array<IntArray> = Array(height) { IntArray(width) }
) {
    fun place(row: Int, col: Int, playerId: Int): GridBoard {
        val newCells = Array(height) { r -> cells[r].copyOf() }
        newCells[row][col] = playerId
        return GridBoard(width, height, newCells)
    }

    fun isEmpty(row: Int, col: Int): Boolean = cells[row][col] == 0

    fun isFull(): Boolean = cells.all { row -> row.all { it != 0 } }

    fun get(row: Int, col: Int): Int = cells[row][col]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GridBoard) return false
        if (width != other.width) return false
        if (height != other.height) return false
        return cells.contentDeepEquals(other.cells)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + cells.contentDeepHashCode()
        return result
    }
}
