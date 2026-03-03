// Simple verification script to test the fixes
// This can be run in a Kotlin REPL or IDE

import com.offlinegames.games.minesweeper.*
import com.offlinegames.core.*

fun main() {
    val rules = MinesweeperRules()
    
    println("=== Testing Flood Fill Fix ===")
    // Create a 9x9 board with a single mine in corner
    val cells1 = Array(9) { r ->
        Array(9) { c ->
            MinesweeperCell(isMine = r == 0 && c == 0)
        }
    }
    
    val board1 = MinesweeperBoard(
        width = 9,
        height = 9,
        mineCount = 1,
        cells = cells1,
        firstClickCompleted = true
    )
    
    // Click on center cell
    val result1 = rules.applyAction(board1, GameAction.RevealCellsAction(Position(4, 4)), Player.PLAYER_ONE)
    val newBoard1 = result1.newBoardData
    
    // Count revealed cells
    var revealedCount = 0
    for (r in 0 until 9) {
        for (c in 0 until 9) {
            if (newBoard1.getCell(r, c).isRevealed) {
                revealedCount++
            }
        }
    }
    
    println("Revealed count: $revealedCount (expected >= 10)")
    println("Flood fill test: ${if (revealedCount >= 10) "PASS" else "FAIL"}")
    
    println("\n=== Testing Flag Placement Fix ===")
    val cells2 = Array(3) { Array(3) { MinesweeperCell(isMine = false) } }
    
    val board2 = MinesweeperBoard(
        width = 3,
        height = 3,
        mineCount = 1,
        cells = cells2,
        firstClickCompleted = true
    )
    
    val result2 = rules.applyAction(board2, GameAction.FlagCellAction(Position(1, 1)), Player.PLAYER_ONE)
    val newBoard2 = result2.newBoardData
    
    println("Cell flagged: ${newBoard2.getCell(1, 1).isFlagged} (expected true)")
    println("Flags placed: ${newBoard2.flagsPlaced} (expected 1)")
    println("Flag placement test: ${if (newBoard2.getCell(1, 1).isFlagged && newBoard2.flagsPlaced == 1) "PASS" else "FAIL"}")
    
    println("\n=== Testing Chord Reveal Fix ===")
    // Create board with revealed cell showing "3" and 3 flagged mines
    val cells3 = Array(3) { r ->
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
    
    val board3 = MinesweeperBoard(
        width = 3,
        height = 3,
        mineCount = 3,
        cells = cells3,
        firstClickCompleted = true,
        flagsPlaced = 3
    )
    
    val result3 = rules.applyAction(board3, GameAction.ChordAction(Position(1, 1)), Player.PLAYER_ONE)
    val newBoard3 = result3.newBoardData
    
    println("Cell (0,1) revealed: ${newBoard3.getCell(0, 1).isRevealed} (expected true)")
    println("Cell (0,2) revealed: ${newBoard3.getCell(0, 2).isRevealed} (expected true)")
    println("Chord reveal test: ${if (newBoard3.getCell(0, 1).isRevealed && newBoard3.getCell(0, 2).isRevealed) "PASS" else "FAIL"}")
    
    println("\n=== All Tests Complete ===")
}
