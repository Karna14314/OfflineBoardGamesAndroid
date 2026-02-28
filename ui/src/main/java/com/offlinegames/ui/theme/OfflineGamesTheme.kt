package com.offlinegames.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark palette ────────────────────────────────────────────────────────────
private val DarkPrimary    = Color(0xFF7C83FD)
private val DarkSecondary  = Color(0xFF96FEFF)
private val DarkBackground = Color(0xFF0F0F1A)
private val DarkSurface    = Color(0xFF1E1E30)
private val DarkOnPrimary  = Color(0xFF0F0F1A)
private val DarkOnSurface  = Color(0xFFE8E8F0)
private val DarkError      = Color(0xFFFF6B6B)

private val darkColorScheme = darkColorScheme(
    primary          = DarkPrimary,
    onPrimary        = DarkOnPrimary,
    secondary        = DarkSecondary,
    background       = DarkBackground,
    surface          = DarkSurface,
    onSurface        = DarkOnSurface,
    onBackground     = DarkOnSurface,
    error            = DarkError
)

// ── Light palette ───────────────────────────────────────────────────────────
private val LightPrimary    = Color(0xFF4A52E8)
private val LightSecondary  = Color(0xFF00BCD4)
private val LightBackground = Color(0xFFF5F5FF)
private val LightSurface    = Color(0xFFFFFFFF)
private val LightOnPrimary  = Color(0xFFFFFFFF)
private val LightOnSurface  = Color(0xFF1A1A2E)

private val lightColorScheme = lightColorScheme(
    primary          = LightPrimary,
    onPrimary        = LightOnPrimary,
    secondary        = LightSecondary,
    background       = LightBackground,
    surface          = LightSurface,
    onSurface        = LightOnSurface,
    onBackground     = LightOnSurface
)

/**
 * App-wide Compose theme.
 *
 * @param darkTheme  Override system theme; defaults to system setting.
 * @param content    The Composable content to theme.
 */
@Composable
fun OfflineGamesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkColorScheme else lightColorScheme
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
