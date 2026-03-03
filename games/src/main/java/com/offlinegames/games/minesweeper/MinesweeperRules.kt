package com.offlinegames.games.minesweeper

import com.offlinegames.core.*

/**
 * Minesweeper rules using the ActionBased architecture.
 *
 * Features:
 * - Random mine placement after first click (first-click safety)
 * - Flood fill reveal for empty cells
 * - Flag/unflag cells
 * - Win detection when all safe cells revealed
 */
class MinesweeperRules : ActionBasedRules<MinesweeperBoard> {

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy GameRules implementation
    // ═══════════════════════════════════════════════════════════════════════

    override fun isValidMove(state: GameState, move: Move): Boolean {
        // Minesweeper uses actions, not traditional moves
        return false
    }

    override fun getLegalMoves(state: GameState, player: Player): List<Move> {
        return emptyList()
    }

    override fun applyMove(boardData: MinesweeperBoard, move: Move, player: Player): MinesweeperBoard {
        return boardData
    }

    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as MinesweeperBoard

        return when {
            board.isLoss() -> GameResult.DRAW // Loss
            board.isWin() -> GameResult.WIN
            else -> GameResult.IN_PROGRESS
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ActionBasedRules implementation
    // ═══════════════════════════════════════════════════════════════════════

    override fun isValidAction(state: GameState, action: GameAction): Boolean {
        val board = state.boardData as MinesweeperBoard

        return when (action) {
            is GameAction.RevealCellsAction -> {
                val pos = action.position
                board.isValidPosition(pos.row, pos.col) &&
                        !board.getCell(pos.row, pos.col).isRevealed &&
                        !board.getCell(pos.row, pos.col).isFlagged
            }
            is GameAction.FlagCellAction -> {
                val pos = action.position
                board.isValidPosition(pos.row, pos.col) &&
                        !board.getCell(pos.row, pos.col).isRevealed
            }
            is GameAction.RevealAreaAction -> {
                action.positions.all { board.isValidPosition(it.row, it.col) }
            }
            is GameAction.ChordAction -> {
                val pos = action.position
                if (!board.isValidPosition(pos.row, pos.col)) return false
                val cell = board.getCell(pos.row, pos.col)
                // Chord is valid only on revealed numbered cells
                cell.isRevealed && cell.adjacentMines > 0 && !cell.isMine
            }
            is GameAction.RestartAction -> true
            is GameAction.UndoAction -> false // Minesweeper doesn't support undo
            is GameAction.SaveAndExitAction -> true
            else -> false
        }
    }

    override fun applyAction(
        boardData: MinesweeperBoard,
        action: GameAction,
        player: Player
    ): ActionResult<MinesweeperBoard> {
        return when (action) {
            is GameAction.RevealCellsAction -> revealCell(boardData, action.position)
            is GameAction.FlagCellAction -> flagCell(boardData, action.position)
            is GameAction.RevealAreaAction -> revealArea(boardData, action.positions)
            is GameAction.ChordAction -> chordReveal(boardData, action.position)
            else -> ActionResult(boardData)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Minesweeper-specific logic
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Reveal a cell. If first click, place mines first ensuring safety.
     * If empty cell, flood fill adjacent cells.
     */
    private fun revealCell(board: MinesweeperBoard, position: Position): ActionResult<MinesweeperBoard> {
        var currentBoard = board

        // First click - place mines ensuring safety
        if (!board.firstClickCompleted) {
            currentBoard = placeMines(board, position)
        }

        // Reveal the cell
        val cell = currentBoard.getCell(position.row, position.col)
        if (cell.isRevealed || cell.isFlagged) {
            return ActionResult(currentBoard)
        }

        // Calculate adjacent mines if not done
        val adjacentCount = if (cell.adjacentMines >= 0) {
            cell.adjacentMines
        } else {
            currentBoard.countAdjacentMines(position.row, position.col)
        }

        val revealedCell = cell.copy(
            visibility = CellVisibility.REVEALED,
            adjacentMines = adjacentCount
        )

        currentBoard = currentBoard.withCell(position.row, position.col, revealedCell)

        // If mine was revealed, game over
        if (cell.isMine) {
            return ActionResult(
                newBoardData = currentBoard,
                gameEnded = true
            )
        }

        // If empty cell (no adjacent mines), flood fill
        if (adjacentCount == 0 && !cell.isMine) {
            currentBoard = floodFillReveal(currentBoard, position)
        }

        // Check for win
        val gameEnded = currentBoard.isWin()

        return ActionResult(
            newBoardData = currentBoard,
            gameEnded = gameEnded
        )
    }

    /**
     * Toggle flag on a cell.
     */
    private fun flagCell(board: MinesweeperBoard, position: Position): ActionResult<MinesweeperBoard> {
        val cell = board.getCell(position.row, position.col)

        if (cell.isRevealed) return ActionResult(board)

        val newVisibility = when (cell.visibility) {
            CellVisibility.HIDDEN -> CellVisibility.FLAGGED
            CellVisibility.FLAGGED -> CellVisibility.HIDDEN
            else -> cell.visibility
        }

        val newCell = cell.copy(visibility = newVisibility)
        val newBoard = board.withCell(position.row, position.col, newCell)

        return ActionResult(newBoard)
    }

    /**
     * Chord reveal: if a revealed number cell has exactly as many adjacent flags
     * as its number, reveal all remaining unflagged hidden neighbors.
     * This is a standard Minesweeper shortcut that speeds up gameplay.
     */
    private fun chordReveal(board: MinesweeperBoard, position: Position): ActionResult<MinesweeperBoard> {
        val cell = board.getCell(position.row, position.col)

        // Must be a revealed numbered cell
        if (!cell.isRevealed || cell.adjacentMines <= 0 || cell.isMine) {
            return ActionResult(board)
        }

        // Count adjacent flags
        val adjacents = board.getAdjacentPositions(position.row, position.col)
        val flagCount = adjacents.count { board.getCell(it.row, it.col).isFlagged }

        // Only chord if flag count matches the number
        if (flagCount != cell.adjacentMines) {
            return ActionResult(board)
        }

        // Reveal all unflagged hidden neighbors
        val toReveal = adjacents.filter { pos ->
            val adj = board.getCell(pos.row, pos.col)
            !adj.isRevealed && !adj.isFlagged
        }

        if (toReveal.isEmpty()) return ActionResult(board)

        var currentBoard = board
        for (pos in toReveal) {
            val result = revealCell(currentBoard, pos)
            currentBoard = result.newBoardData
            if (result.gameEnded) {
                return ActionResult(currentBoard, gameEnded = true)
            }
        }

        return ActionResult(currentBoard, gameEnded = currentBoard.isWin())
    }

    /**
     * Reveal multiple cells (used for flood fill).
     */
    private fun revealArea(board: MinesweeperBoard, positions: List<Position>): ActionResult<MinesweeperBoard> {
        var currentBoard = board

        for (pos in positions) {
            val result = revealCell(currentBoard, pos)
            currentBoard = result.newBoardData
            if (result.gameEnded) {
                return ActionResult(currentBoard, gameEnded = true)
            }
        }

        return ActionResult(currentBoard, gameEnded = currentBoard.isWin())
    }

    /**
     * Place mines randomly, ensuring a large safe zone around the first click.
     *
     * Uses a radius-2 safe zone so the clicked cell AND all its neighbors
     * have 0 adjacent mines. This guarantees the first click always triggers
     * a flood fill that opens up a large playable area (typically 10-20+ cells).
     */
    fun placeMines(board: MinesweeperBoard, safePosition: Position): MinesweeperBoard {
        val safeZone = mutableSetOf<Pair<Int, Int>>()

        // Build a radius-2 safe zone around the clicked position.
        // Radius 1 = clicked cell is safe.
        // Radius 2 = clicked cell's neighbors are also mine-free,
        //            so the clicked cell shows "0" and flood fill triggers.
        val safeRadius = 2
        for (dr in -safeRadius..safeRadius) {
            for (dc in -safeRadius..safeRadius) {
                val nr = safePosition.row + dr
                val nc = safePosition.col + dc
                if (board.isValidPosition(nr, nc)) {
                    safeZone.add(nr to nc)
                }
            }
        }

        // Collect all non-safe positions as candidates for mine placement
        val allPositions = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until board.height) {
            for (c in 0 until board.width) {
                if ((r to c) !in safeZone) {
                    allPositions.add(r to c)
                }
            }
        }

        // Fallback: if not enough room for all mines outside radius-2,
        // shrink safe zone to radius-1 (original behavior)
        if (allPositions.size < board.mineCount) {
            safeZone.clear()
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val nr = safePosition.row + dr
                    val nc = safePosition.col + dc
                    if (board.isValidPosition(nr, nc)) {
                        safeZone.add(nr to nc)
                    }
                }
            }
            allPositions.clear()
            for (r in 0 until board.height) {
                for (c in 0 until board.width) {
                    if ((r to c) !in safeZone) {
                        allPositions.add(r to c)
                    }
                }
            }
        }

        // Shuffle and pick mine positions
        allPositions.shuffle()
        val minePositions = allPositions.take(board.mineCount).toSet()

        // Create new cells with mines
        val newCells = Array(board.height) { r ->
            Array(board.width) { c ->
                val isMine = (r to c) in minePositions
                MinesweeperCell(isMine = isMine)
            }
        }

        return board.copy(
            cells = newCells,
            firstClickCompleted = true
        )
    }

    /**
     * Flood fill reveal - recursively reveal all connected empty cells.
     */
    fun floodFillReveal(board: MinesweeperBoard, startPosition: Position): MinesweeperBoard {
        var currentBoard = board
        val queue = ArrayDeque<Position>()
        val visited = mutableSetOf<Pair<Int, Int>>()

        // Start position is already revealed, so add its neighbors to the queue
        visited.add(startPosition.row to startPosition.col)
        
        // Add all neighbors of the start position to begin flood fill
        for (adj in currentBoard.getAdjacentPositions(startPosition.row, startPosition.col)) {
            val key = adj.row to adj.col
            if (key !in visited) {
                visited.add(key)
                queue.add(adj)
            }
        }

        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            val cell = currentBoard.getCell(pos.row, pos.col)

            if (cell.isRevealed || cell.isMine || cell.isFlagged) continue

            // Calculate adjacent mines
            val adjacentCount = currentBoard.countAdjacentMines(pos.row, pos.col)
            val revealedCell = cell.copy(
                visibility = CellVisibility.REVEALED,
                adjacentMines = adjacentCount
            )

            currentBoard = currentBoard.withCell(pos.row, pos.col, revealedCell)

            // If empty, add neighbors to queue
            if (adjacentCount == 0) {
                for (adj in currentBoard.getAdjacentPositions(pos.row, pos.col)) {
                    val key = adj.row to adj.col
                    if (key !in visited) {
                        visited.add(key)
                        queue.add(adj)
                    }
                }
            }
        }

        return currentBoard
    }

    /**
     * Create initial board with specified difficulty.
     */
    fun createInitialBoard(difficulty: MinesweeperDifficulty = MinesweeperDifficulty.BEGINNER): MinesweeperBoard {
        return MinesweeperBoard(
            width = difficulty.width,
            height = difficulty.height,
            mineCount = difficulty.mineCount,
            firstClickCompleted = false,
            flagsPlaced = 0,
            revealedCount = 0
        )
    }
}
