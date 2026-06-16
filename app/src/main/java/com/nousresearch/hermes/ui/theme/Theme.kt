package com.nousresearch.hermes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color

// ── Brand palette ──────────────────────────────────────────────────────────

// Primary: deep electric indigo
val Indigo50   = Color(0xFFEDE7FF)
val Indigo100  = Color(0xFFD1C4E9)
val Indigo400  = Color(0xFF7C4DFF)
val Indigo500  = Color(0xFF651FFF)
val Indigo600  = Color(0xFF6200EA)
val Indigo700  = Color(0xFF4A1FCC)
val Indigo900  = Color(0xFF1A0070)

// Accent: electric cyan
val Cyan400    = Color(0xFF00E5FF)
val Cyan500    = Color(0xFF00B0FF)

// Neutrals — deep space dark theme
val Ink950     = Color(0xFF060612)   // page background
val Ink900     = Color(0xFF0D0D1F)   // app bg
val Ink850     = Color(0xFF121228)   // surface
val Ink800     = Color(0xFF181832)   // card
val Ink750     = Color(0xFF1E1E3A)   // elevated card
val Ink700     = Color(0xFF252545)   // outline / border
val Ink600     = Color(0xFF3A3A5C)   // disabled / muted
val Ink200     = Color(0xFFB0B0D0)   // on-surface-variant
val Ink100     = Color(0xFFD8D8F0)   // secondary text
val Ink50      = Color(0xFFEEEEFF)   // primary text

// Semantic
val Red400     = Color(0xFFFF5370)
val Green400   = Color(0xFF69FF47)
val Amber400   = Color(0xFFFFD740)

private val DarkColors = darkColorScheme(
    primary               = Indigo400,
    onPrimary             = Color.White,
    primaryContainer      = Indigo700,
    onPrimaryContainer    = Indigo50,

    secondary             = Cyan400,
    onSecondary           = Ink900,
    secondaryContainer    = Color(0xFF004D6B),
    onSecondaryContainer  = Cyan400,

    tertiary              = Green400,
    onTertiary            = Ink900,

    error                 = Red400,
    onError               = Color.White,
    errorContainer        = Color(0xFF4D0016),
    onErrorContainer      = Red400,

    background            = Ink900,
    onBackground          = Ink50,

    surface               = Ink850,
    onSurface             = Ink50,
    surfaceVariant        = Ink800,
    onSurfaceVariant      = Ink200,
    surfaceTint           = Indigo400,

    outline               = Ink700,
    outlineVariant        = Ink600,
    scrim                 = Color(0xCC000000),
    inverseSurface        = Ink100,
    inverseOnSurface      = Ink900,
    inversePrimary        = Indigo700,
)

private val LightColors = lightColorScheme(
    primary               = Indigo600,
    onPrimary             = Color.White,
    primaryContainer      = Indigo50,
    onPrimaryContainer    = Indigo900,

    secondary             = Cyan500,
    onSecondary           = Color.White,
    secondaryContainer    = Color(0xFFCCF0FF),
    onSecondaryContainer  = Color(0xFF003549),

    tertiary              = Color(0xFF1B6B1B),
    onTertiary            = Color.White,

    error                 = Color(0xFFB00020),
    onError               = Color.White,
    errorContainer        = Color(0xFFFFDAD6),
    onErrorContainer      = Color(0xFF410002),

    background            = Color(0xFFF5F4FF),
    onBackground          = Color(0xFF1A1732),

    surface               = Color.White,
    onSurface             = Color(0xFF1A1732),
    surfaceVariant        = Color(0xFFECEAFF),
    onSurfaceVariant      = Color(0xFF4A4870),
    surfaceTint           = Indigo600,

    outline               = Color(0xFFCAC7EC),
    outlineVariant        = Color(0xFFE4E1FF),
)

@Composable
fun HermesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme  = if (darkTheme) DarkColors else LightColors,
        typography   = HermesTypography,
        shapes       = HermesShapes,
        content      = content,
    )
}
