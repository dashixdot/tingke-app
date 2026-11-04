package com.phygroup.tingke.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF2563EB)
private val BlueDark = Color(0xFF1D4ED8)
private val BlueLight = Color(0xFF93C5FD)

private val LightScheme = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = BlueDark,
    secondary = Color(0xFF0891B2),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF1F2937),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFEEF2FF),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = BlueLight,
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E3A8A),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF1F2937)
)

/** Material3 配色之外，原型里还有几个语义色，单独用 CompositionLocal 传 */
@Immutable
data class TkExtra(
    val card: Color,
    val track: Color,
    val sub: Color,
    val ok: Color,
    val warn: Color,
    val doubt: Color,
    val danger: Color,
    val warnBg: Color,
    val warnFg: Color,
    val warnBorder: Color
)

val LocalTkExtra = staticCompositionLocalOf {
    TkExtra(
        card = Color.White,
        track = Color(0xFFE8EDF3),
        sub = Color(0xFF6B7280),
        ok = Color(0xFF16A34A),
        warn = Color(0xFFD97706),
        doubt = Color(0xFF7C3AED),
        danger = Color(0xFFDC2626),
        warnBg = Color(0xFFFEF3C7),
        warnFg = Color(0xFF92400E),
        warnBorder = Color(0xFFFBBF24)
    )
}

@Composable
fun TingKeTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (dark) DarkScheme else LightScheme
    val extra = if (dark) {
        TkExtra(
            card = Color(0xFF1E293B),
            track = Color(0xFF0B1220),
            sub = Color(0xFF94A3B8),
            ok = Color(0xFF4ADE80),
            warn = Color(0xFFFBBF24),
            doubt = Color(0xFFA78BFA),
            danger = Color(0xFFF87171),
            warnBg = Color(0xFF422006),
            warnFg = Color(0xFFFDE68A),
            warnBorder = Color(0xFFA16207)
        )
    } else {
        LocalTkExtra.current
    }
    CompositionLocalProvider(LocalTkExtra provides extra) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
