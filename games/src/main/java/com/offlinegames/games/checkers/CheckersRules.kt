package com.offlinegames.games.checkers

import com.offlinegames.core.*

/**
 * Checkers rules using the ActionBased architecture.
 *
 * Features:
 * - Diagonal movement
 * - Forced captures (must capture if possible)
 * - Multi-jump capture chains
 * - King promotion on opposite end
 */
class CheckersRules : ActionBasedRules<CheckersBoard> {

    companion object {
        const val SIZE = 8
        const val MAN_VALUE = 100
        const val KING_VALUE = 300
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy GameRules implementation
    // ═══════════════════════════════════════════════════════════════════════

    override fun isValidMove(state: GameState, move: Move): Boolean {
        val board = state.boardData as CheckersBoard
        val fromPiece = board.getPiece(move.position.row, move.position.col) ?: return false

        // Check if this is a capture move
        val toRow = move.metadata["toRow"] ?: return false
        val toCol = move.metadata["toCol"] ?: return false

        val checkersMove = CheckersMove(
            from = move.position,
            to = Position(toRow, toCol),
            capturedPositions = emptyList() // Will be determined by rules
        )

        return isValidCheckersMove(board, fromPiece, checkersMove, state.currentPlayer.id)
    }

    override fun getLegalMoves(state: GameState, player: Player): List<Move> {
        val board = state.boardData as CheckersBoard
        val moves = mutableListOf<Move>()

        // Check for forced captures first
        val captureMoves = getAllCaptureMoves(board, player.id)

        if (captureMoves.isNotEmpty()) {
            // Must capture - return only capture moves
            for (checkersMove in captureMoves) {
                moves.add(createMoveFromCheckersMove(player.id, checkersMove))
            }
        } else {
            // Regular moves
            val playerPieces = board.getPlayerPieces(player.id)
            for (piece in playerPieces) {
                val pieceMoves = getPieceMoves(board, piece)
                for (checkersMove in pieceMoves) {
                    moves.add(createMoveFromCheckersMove(player.id, checkersMove))
                }
            }
        }

        return moves
    }

    private fun createMoveFromCheckersMove(playerId: Int, checkersMove: CheckersMove): Move {
        return Move(
            playerId = playerId,
            position = checkersMove.from,
            type = if (checkersMove.capturedPositions.isNotEmpty()) MoveType.JUMP else MoveType.SLIDE,
            metadata = mapOf(
                "toRow" to checkersMove.to.row,
                "toCol" to checkersMove.to.col
            )
        )
    }

    override fun applyMove(boardData: CheckersBoard, move: Move, player: Player): CheckersBoard {
        val toRow = move.metadata["toRow"] ?: return boardData
        val toCol = move.metadata["toCol"] ?: return boardData

        var board = boardData
        val fromPiece = board.getPiece(move.position.row, move.position.col) ?: return boardData

        // Calculate captured piece position for a single capture
        val capturedRow = (move.position.row + toRow) / 2
        val capturedCol = (move.position.col + toCol) / 2

        // Check if this is a capture (jumping 2 squares)
        val isCapture = kotlin.math.abs(toRow - move.position.row) == 2

        if (isCapture) {
            // Remove captured piece
            board = board.capturePiece(capturedRow, capturedCol)
        }

        // Move the piece
        board = board.movePiece(move.position.row, move.position.col, toRow, toCol)

        return board
    }

    override fun evaluateResult(state: GameState): GameResult {
        val board = state.boardData as CheckersBoard

        // Check if current player has any pieces
        val currentPlayerId = state.currentPlayer.id
        if (board.countPieces(currentPlayerId) == 0) {
            // Previous player won
            return GameResult.WIN
        }

        // Check if current player has any legal moves
        if (!board.hasLegalMoves(currentPlayerId)) {
            // Previous player won
            return GameResult.WIN
        }

        return GameResult.IN_PROGRESS
    }

    override fun shouldAdvanceTurn(state: GameState): Boolean {
        val board = state.boardData as CheckersBoard
        val lastPlayerId = if (state.moveHistory.isEmpty()) {
            2 // Default to player 2 if no history
        } else {
            state.moveHistory.last().move.playerId
        }

        // Check if the piece that just moved can capture again
        val lastMove = state.moveHistory.lastOrNull() ?: return true
        val toRow = lastMove.move.metadata["toRow"] ?: return true
        val toCol = lastMove.move.metadata["toCol"] ?: return true

        val movedPiece = board.getPiece(toRow, toCol) ?: return true

        // If last move was a capture, check for chain captures
        val wasCapture = kotlin.math.abs(toRow - lastMove.move.position.row) == 2
        if (wasCapture && board.canPieceCapture(movedPiece)) {
            return false // Continue turn for chain capture
        }

        return true
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ActionBasedRules implementation
    // ═══════════════════════════════════════════════════════════════════════

    override fun isValidAction(state: GameState, action: GameAction): Boolean {
        val board = state.boardData as CheckersBoard

        return when (action) {
            is GameAction.MovePieceAction -> isValidMove(state, action.move)
            is GameAction.CaptureAction -> isValidCaptureAction(board, action, state.currentPlayer.id)
            is GameAction.ChainMoveAction -> isValidChainMove(board, action, state.currentPlayer.id)
            is GameAction.RestartAction -> true
            is GameAction.UndoAction -> true
            is GameAction.SaveAndExitAction -> true
            else -> false
        }
    }

    override fun applyAction(
        boardData: CheckersBoard,
        action: GameAction,
        player: Player
    ): ActionResult<CheckersBoard> {
        return when (action) {
            is GameAction.CaptureAction -> applyCaptureAction(boardData, action, player)
            is GameAction.ChainMoveAction -> applyChainMove(boardData, action, player)
            is GameAction.MovePieceAction -> {
                val newBoard = applyMove(boardData, action.move, player)
                ActionResult(
                    newBoardData = newBoard,
                    moveRecord = MoveRecord(move = action.move)
                )
            }
            else -> ActionResult(boardData)
        }
    }

    override fun shouldContinueTurn(state: GameState, lastAction: GameAction): Boolean {
        if (lastAction !is GameAction.CaptureAction) return false

        val board = state.boardData as CheckersBoard
        val move = lastAction.move
        val toRow = move.metadata["toRow"] ?: return false
        val toCol = move.metadata["toCol"] ?: return false

        val movedPiece = board.getPiece(toRow, toCol) ?: return false
        return board.canPieceCapture(movedPiece)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Checkers-specific logic
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Check if there's any capture available for a player (forced capture rule).
     */
    fun hasAnyCapture(board: CheckersBoard, playerId: Int): Boolean {
        val playerPieces = board.getPlayerPieces(playerId)
        return playerPieces.any { board.canPieceCapture(it) }
    }

    /**
     * Get all capture moves for a player.
     */
    fun getAllCaptureMoves(board: CheckersBoard, playerId: Int): List<CheckersMove> {
        val moves = mutableListOf<CheckersMove>()
        val playerPieces = board.getPlayerPieces(playerId)

        for (piece in playerPieces) {
            val capturePositions = board.getCapturePositions(piece)
            for (capturePos in capturePositions) {
                val capturedRow = (piece.row + capturePos.row) / 2
                val capturedCol = (piece.col + capturePos.col) / 2
                moves.add(CheckersMove(
                    from = Position(piece.row, piece.col),
                    to = capturePos,
                    capturedPositions = listOf(Position(capturedRow, capturedCol))
                ))
            }
        }

        return moves
    }

    /**
     * Get all regular (non-capture) moves for a piece.
     */
    fun getPieceMoves(board: CheckersBoard, piece: CheckersPiece): List<CheckersMove> {
        val moves = mutableListOf<CheckersMove>()
        val directions = getMoveDirections(piece)

        for ((dr, dc) in directions) {
            val newRow = piece.row + dr
            val newCol = piece.col + dc

            if (board.isValidPosition(newRow, newCol) && !board.isOccupied(newRow, newCol)) {
                moves.add(CheckersMove(
                    from = Position(piece.row, piece.col),
                    to = Position(newRow, newCol)
                ))
            }
        }

        return moves
    }

    /**
     * Get valid moves for a piece, considering forced capture rule.
     */
    fun getValidMovesForPiece(
        board: CheckersBoard,
        piece: CheckersPiece,
        playerId: Int
    ): List<CheckersMove> {
        val hasCapture = hasAnyCapture(board, playerId)

        if (hasCapture) {
            // Must capture
            if (!board.canPieceCapture(piece)) return emptyList()

            val capturePositions = board.getCapturePositions(piece)
            return capturePositions.map { capturePos ->
                val capturedRow = (piece.row + capturePos.row) / 2
                val capturedCol = (piece.col + capturePos.col) / 2
                CheckersMove(
                    from = Position(piece.row, piece.col),
                    to = capturePos,
                    capturedPositions = listOf(Position(capturedRow, capturedCol))
                )
            }
        } else {
            // Regular moves
            return getPieceMoves(board, piece)
        }
    }

    /**
     * Get chain captures for a piece (multi-jump).
     */
    fun getChainCaptures(board: CheckersBoard, piece: CheckersPiece): List<List<CheckersMove>> {
        val chains = mutableListOf<List<CheckersMove>>()
        findChainCaptures(board, piece, emptyList(), chains)
        return chains
    }

    private fun findChainCaptures(
        board: CheckersBoard,
        piece: CheckersPiece,
        currentChain: List<CheckersMove>,
        allChains: MutableList<List<CheckersMove>>
    ) {
        val capturePositions = board.getCapturePositions(piece)

        if (capturePositions.isEmpty()) {
            // End of chain
            if (currentChain.isNotEmpty()) {
                allChains.add(currentChain)
            }
            return
        }

        for (capturePos in capturePositions) {
            val capturedRow = (piece.row + capturePos.row) / 2
            val capturedCol = (piece.col + capturePos.col) / 2

            val move = CheckersMove(
                from = Position(piece.row, piece.col),
                to = capturePos,
                capturedPositions = listOf(Position(capturedRow, capturedCol))
            )

            // Simulate the capture
            var newBoard = board.capturePiece(capturedRow, capturedCol)
            newBoard = newBoard.movePiece(piece.row, piece.col, capturePos.row, capturePos.col)

            // Check if promoted
            val movedPiece = newBoard.getPiece(capturePos.row, capturePos.col)!!
            val finalPiece = if (movedPiece.isKing) movedPiece else {
                // Continue with potentially promoted piece
                newBoard.getPiece(capturePos.row, capturePos.col)!!
            }

            findChainCaptures(newBoard, finalPiece, currentChain + move, allChains)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════════

    private fun isValidCheckersMove(
        board: CheckersBoard,
        piece: CheckersPiece,
        move: CheckersMove,
        playerId: Int
    ): Boolean {
        // Must be player's piece
        if (piece.playerId != playerId) return false

        // Check forced capture rule
        val hasCapture = hasAnyCapture(board, playerId)
        val isCapture = move.capturedPositions.isNotEmpty()

        if (hasCapture && !isCapture) return false

        // Validate move
        return if (isCapture) {
            board.getCapturePositions(piece).contains(move.to)
        } else {
            getPieceMoves(board, piece).any { it.to == move.to }
        }
    }

    private fun isValidCaptureAction(
        board: CheckersBoard,
        action: GameAction.CaptureAction,
        playerId: Int
    ): Boolean {
        val move = action.move
        val fromPiece = board.getPiece(move.position.row, move.position.col) ?: return false

        if (fromPiece.playerId != playerId) return false

        val toRow = move.metadata["toRow"] ?: return false
        val toCol = move.metadata["toCol"] ?: return false

        // Must be a jump
        if (kotlin.math.abs(toRow - move.position.row) != 2) return false

        val capturedRow = (move.position.row + toRow) / 2
        val capturedCol = (move.position.col + toCol) / 2

        // Must match the captured piece
        if (action.capturedPiece != Position(capturedRow, capturedCol)) return false

        // Must be opponent's piece
        val capturedPiece = board.getPiece(capturedRow, capturedCol)
        if (capturedPiece == null || capturedPiece.playerId == playerId) return false

        return true
    }

    private fun isValidChainMove(
        board: CheckersBoard,
        action: GameAction.ChainMoveAction,
        playerId: Int
    ): Boolean {
        // Validate each move in the chain
        var currentBoard = board
        for (move in action.moves) {
            val captureAction = GameAction.CaptureAction(
                move = move,
                capturedPiece = action.capturedPositions[action.moves.indexOf(move)]
            )
            if (!isValidCaptureAction(currentBoard, captureAction, playerId)) {
                return false
            }
            // Apply to board for next iteration
            currentBoard = applyMove(currentBoard, move, Player(playerId, "", true))
        }
        return true
    }

    private fun applyCaptureAction(
        board: CheckersBoard,
        action: GameAction.CaptureAction,
        player: Player
    ): ActionResult<CheckersBoard> {
        var newBoard = board
        val move = action.move
        val toRow = move.metadata["toRow"] ?: return ActionResult(board)
        val toCol = move.metadata["toCol"] ?: return ActionResult(board)

        // Capture the piece
        newBoard = newBoard.capturePiece(action.capturedPiece.row, action.capturedPiece.col)

        // Move the capturing piece
        newBoard = newBoard.movePiece(move.position.row, move.position.col, toRow, toCol)

        val continueChain = action.continueChain && newBoard.canPieceCapture(
            newBoard.getPiece(toRow, toCol)!!
        )

        return ActionResult(
            newBoardData = newBoard,
            moveRecord = MoveRecord(move = move),
            chainActions = if (continueChain) {
                // Return potential follow-up captures
                emptyList() // Will be calculated by the reducer
            } else emptyList()
        )
    }

    private fun applyChainMove(
        board: CheckersBoard,
        action: GameAction.ChainMoveAction,
        player: Player
    ): ActionResult<CheckersBoard> {
        var currentBoard = board
        var totalCaptured = 0

        for (move in action.moves) {
            val capturedPos = action.capturedPositions[action.moves.indexOf(move)]
            currentBoard = currentBoard.capturePiece(capturedPos.row, capturedPos.col)
            currentBoard = applyMove(currentBoard, move, player)
            totalCaptured++
        }

        return ActionResult(
            newBoardData = currentBoard,
            moveRecord = MoveRecord(move = action.moves.first())
        )
    }

    private fun getMoveDirections(piece: CheckersPiece): List<Pair<Int, Int>> {
        return when {
            piece.isKing -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
            piece.playerId == 1 -> listOf(-1 to -1, -1 to 1) // Up
            else -> listOf(1 to -1, 1 to 1) // Down
        }
    }
}
