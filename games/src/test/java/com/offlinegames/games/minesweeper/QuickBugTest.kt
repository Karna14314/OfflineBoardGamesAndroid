package com.offlinegames.games.minesweeper

import com.offlinegames.core.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Quick test to verify the bugs exist or are fixed
 */
class QuickBugTest {

    private val rules = MinesweeperRules()

    @Test
    fun `test flood fill works`() {
        // Create a 9x9 board with a single mine in corner
        val cells = Array(9) { r ->
            Array(9) { c ->
                MinesweeperCell(isMine = r == 0 && c == 0)
            }
        }

        val board = MinesweeperBoard(
            width = 9,
            height = 9,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true
        )

        // Click on center cell
        val result = rules.applyAction(board, GameAction.RevealCellsAction(Position(4, 4)), Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        // Count revealed cells
        var revealedCount = 0
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (newBoard.getCell(r, c).isRevealed) {
                    revealedCount++
                }
            }
        }

        println("Revealed count: $revealedCount")
        assertTrue("Should reveal multiple cells, got $revealedCount", revealedCount >= 10)
    }

    @Test
    fun `test flag placement works`() {
        val cells = Array(3) { Array(3) { MinesweeperCell(isMine = false) } }

        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true
        )

        val result = rules.applyAction(board, GameAction.FlagCellAction(Position(1, 1)), Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        println("Cell flagged: ${newBoard.getCell(1, 1).isFlagged}")
        println("Flags placed: ${newBoard.flagsPlaced}")
        
        assertTrue("Cell should be flagged", newBoard.getCell(1, 1).isFlagged)
        assertEquals("flagsPlaced should be 1", 1, newBoard.flagsPlaced)
    }

    @Test
    fun `test chord reveal works`() {
        // Create board with revealed cell showing "3" and 3 flagged mines
        val cells = Array(3) { r ->
            Array(3) { c ->
                when {
                    c == 0 -> MinesweeperCell(isMine = true, visibility = CellVisibility.FLAGGED)
                    r == 1 && c == 1 -> MinesweeperCell(
                        isMine = false,
                        visibility = CellVisibility.REVEALED,
                        adjacentMines = 3
                    )
                    else -> MinesweeperCell(isMine = false)
                }
            }
        }

        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 3,
            cells = cells,
            firstClickCompleted = true,
            flagsPlaced = 3
        )

        val result = rules.applyAction(board, GameAction.ChordAction(Position(1, 1)), Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        println("Cell (0,1) revealed: ${newBoard.getCell(0, 1).isRevealed}")
        println("Cell (0,2) revealed: ${newBoard.getCell(0, 2).isRevealed}")
        
        assertTrue("Cell (0,1) should be revealed", newBoard.getCell(0, 1).isRevealed)
        assertTrue("Cell (0,2) should be revealed", newBoard.getCell(0, 2).isRevealed)
    }
}
