package com.offlinegames.games.twenty48

import com.offlinegames.core.*

/**
 * Rules for 2048 game using the ActionBased architecture.
 *
 * Supports:
 * - Swipe-based tile movement and merging
 * - Directional board traversal
 * - Multi-tile mutations
 * - Deterministic state updates
 * - Score tracking
 */
class Game2048Rules : ActionBasedRules<Game2048Board> {

    companion object {
        const val SIZE = 4
        val NEW_TILE_VALUES = listOf(2, 4)
        val NEW_TILE_PROBABILITIES = listOf(0.9, 0.1) // 90% chance of 2, 10% chance of 4
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy GameRules implementation (for backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════

    override fun isValidMove(state: GameState, move: Move): Boolean {
        // 2048 doesn't use traditional moves - it uses actions
        return false
    }

    override fun getLegalMoves(state: GameState, player: Player): List<Move> {
        // 2048 doesn't use traditional moves
        return emptyList()
    }

    override fun applyMove(boardData: Game2048Board, move: Move, player: Player): Game2048Board {
        // Not used in 2048 - use applyAction instead
        return boardData
    }

    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as Game2048Board

        // Win condition: reached 2048
        if (board.hasWon()) return GameResult.WIN

        // Loss condition: no possible moves
        if (!board.hasPossibleMoves()) return GameResult.DRAW

        return GameResult.IN_PROGRESS
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ActionBasedRules implementation
    // ═══════════════════════════════════════════════════════════════════════

    override fun isValidAction(state: GameState, action: GameAction): Boolean {
        return when (action) {
            is GameAction.MergeTilesAction -> canMoveInDirection(state, action.direction)
            is GameAction.RestartAction -> true
            is GameAction.UndoAction -> true
            is GameAction.SaveAndExitAction -> true
            else -> false
        }
    }

    override fun applyAction(
        boardData: Game2048Board,
        action: GameAction,
        player: Player
    ): ActionResult<Game2048Board> {
        return when (action) {
            is GameAction.MergeTilesAction -> {
                val (newBoard, scoreDelta) = executeSwipe(boardData, action.direction)

                // Spawn a new tile if the board changed
                val finalBoard = if (newBoard != boardData) {
                    spawnRandomTile(newBoard)
                } else {
                    newBoard
                }

                ActionResult(
                    newBoardData = finalBoard,
                    scoreDelta = scoreDelta,
                    gameEnded = !finalBoard.hasPossibleMoves() || finalBoard.hasWon()
                )
            }
            else -> ActionResult(boardData) // No-op for unsupported actions
        }
    }

    override fun getUndoAction(state: GameState): GameAction? {
        // Undo is handled by the reducer's state management
        return null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2048-specific logic
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Check if any tiles can move in the given direction.
     */
    fun canMoveInDirection(state: GameState, direction: SwipeDirection): Boolean {
        val board = state.boardData as Game2048Board
        return canMoveInDirection(board, direction)
    }

    fun canMoveInDirection(board: Game2048Board, direction: SwipeDirection): Boolean {
        when (direction) {
            SwipeDirection.LEFT -> {
                for (r in 0 until SIZE) {
                    var merged = false
                    for (c in 1 until SIZE) {
                        if (board.get(r, c) == 0) continue
                        val leftValue = board.get(r, c - 1)
                        if (leftValue == 0 || (leftValue == board.get(r, c) && !merged)) {
                            return true
                        }
                        if (leftValue == board.get(r, c)) merged = true
                    }
                }
            }
            SwipeDirection.RIGHT -> {
                for (r in 0 until SIZE) {
                    var merged = false
                    for (c in SIZE - 2 downTo 0) {
                        if (board.get(r, c) == 0) continue
                        val rightValue = board.get(r, c + 1)
                        if (rightValue == 0 || (rightValue == board.get(r, c) && !merged)) {
                            return true
                        }
                        if (rightValue == board.get(r, c)) merged = true
                    }
                }
            }
            SwipeDirection.UP -> {
                for (c in 0 until SIZE) {
                    var merged = false
                    for (r in 1 until SIZE) {
                        if (board.get(r, c) == 0) continue
                        val upValue = board.get(r - 1, c)
                        if (upValue == 0 || (upValue == board.get(r, c) && !merged)) {
                            return true
                        }
                        if (upValue == board.get(r, c)) merged = true
                    }
                }
            }
            SwipeDirection.DOWN -> {
                for (c in 0 until SIZE) {
                    var merged = false
                    for (r in SIZE - 2 downTo 0) {
                        if (board.get(r, c) == 0) continue
                        val downValue = board.get(r + 1, c)
                        if (downValue == 0 || (downValue == board.get(r, c) && !merged)) {
                            return true
                        }
                        if (downValue == board.get(r, c)) merged = true
                    }
                }
            }
        }
        return false
    }

    /**
     * Execute a swipe in the given direction.
     * Returns the new board and the score gained from merges.
     */
    fun executeSwipe(board: Game2048Board, direction: SwipeDirection): Pair<Game2048Board, Int> {
        var newTiles = board.tiles.map { it.copyOf() }.toTypedArray()
        var scoreDelta = 0

        when (direction) {
            SwipeDirection.LEFT -> {
                for (r in 0 until SIZE) {
                    val (row, score) = processLineLeft(newTiles[r])
                    newTiles[r] = row
                    scoreDelta += score
                }
            }
            SwipeDirection.RIGHT -> {
                for (r in 0 until SIZE) {
                    val (row, score) = processLineRight(newTiles[r])
                    newTiles[r] = row
                    scoreDelta += score
                }
            }
            SwipeDirection.UP -> {
                for (c in 0 until SIZE) {
                    val column = IntArray(SIZE) { newTiles[it][c] }
                    val (newCol, score) = processLineLeft(column)
                    scoreDelta += score
                    for (r in 0 until SIZE) {
                        newTiles[r][c] = newCol[r]
                    }
                }
            }
            SwipeDirection.DOWN -> {
                for (c in 0 until SIZE) {
                    val column = IntArray(SIZE) { newTiles[it][c] }
                    val (newCol, score) = processLineRight(column)
                    scoreDelta += score
                    for (r in 0 until SIZE) {
                        newTiles[r][c] = newCol[r]
                    }
                }
            }
        }

        val newBoard = Game2048Board(newTiles, board.score + scoreDelta)
        return Pair(newBoard, scoreDelta)
    }

    /**
     * Process a line moving left (compact then merge left).
     * Returns the processed line and score from merges.
     */
    private fun processLineLeft(line: IntArray): Pair<IntArray, Int> {
        val newLine = IntArray(SIZE)
        var score = 0
        var writePos = 0
        var lastValue = 0
        var canMerge = true

        for (i in 0 until SIZE) {
            val value = line[i]
            if (value == 0) continue

            if (canMerge && value == lastValue) {
                // Merge
                val mergedValue = value * 2
                newLine[writePos - 1] = mergedValue
                score += mergedValue
                lastValue = 0  // Prevent double merge
                canMerge = false
            } else {
                // Move to next position
                newLine[writePos] = value
                lastValue = value
                writePos++
                canMerge = true
            }
        }

        return Pair(newLine, score)
    }

    /**
     * Process a line moving right (compact then merge right).
     */
    private fun processLineRight(line: IntArray): Pair<IntArray, Int> {
        val newLine = IntArray(SIZE)
        var score = 0
        var writePos = SIZE - 1
        var lastValue = 0
        var canMerge = true

        for (i in SIZE - 1 downTo 0) {
            val value = line[i]
            if (value == 0) continue

            if (canMerge && value == lastValue) {
                // Merge
                val mergedValue = value * 2
                newLine[writePos + 1] = mergedValue
                score += mergedValue
                lastValue = 0
                canMerge = false
            } else {
                // Move to next position
                newLine[writePos] = value
                lastValue = value
                writePos--
                canMerge = true
            }
        }

        return Pair(newLine, score)
    }

    /**
     * Spawn a random tile (2 or 4) in an empty cell.
     */
    fun spawnRandomTile(board: Game2048Board): Game2048Board {
        val emptyCells = board.getEmptyCells()
        if (emptyCells.isEmpty()) return board

        val randomPos = emptyCells.random()
        val value = if (Math.random() < 0.9) 2 else 4
        return board.place(randomPos.row, randomPos.col, value)
    }

    /**
     * Create initial board with two random tiles.
     */
    fun createInitialBoard(): Game2048Board {
        var board = Game2048Board()
        board = spawnRandomTile(board)
        board = spawnRandomTile(board)
        return board
    }
}
