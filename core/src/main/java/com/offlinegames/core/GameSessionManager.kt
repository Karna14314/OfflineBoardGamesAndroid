package com.offlinegames.core

import java.util.UUID

/**
 * Factory and lifecycle manager for [GameState] sessions.
 *
 * Responsible for:
 * - Creating fresh game sessions with a unique ID
 * - Registering known game types (extensible plug-in table)
 *
 * This is the only place that knows how to create an initial state
 * for a given game identifier string.
 */
class GameSessionManager {

    /**
     * A registry of game IDs → initial-state factories.
     * New games are added here without touching core.
     */
    private val factories = mutableMapOf<String, () -> GameState>()

    /** Register a game factory so [createSession] can use it. */
    fun register(gameId: String, factory: () -> GameState) {
        factories[gameId] = factory
    }

    /**
     * Creates a brand-new [GameState] for [gameId].
     *
     * @throws IllegalArgumentException if the game ID has not been registered.
     */
    fun createSession(gameId: String): GameState {
        val factory = factories[gameId]
            ?: error("No game registered with id='$gameId'. Call register() first.")
        return factory()
    }

    /** Generate a cryptographically random session ID. */
    fun newSessionId(): String = UUID.randomUUID().toString()
}
