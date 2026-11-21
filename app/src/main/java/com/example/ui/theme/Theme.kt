package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class ConcoursExtraColors(
    val gold: Color,
    val goldDark: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val heart: Color,
    val heartDark: Color,
    val heartContainer: Color,
    val onHeartContainer: Color,
    val gem: Color,
    val gemDark: Color,
    val gemContainer: Color,
    val onGemContainer: Color,
    val streak: Color,
    val streakContainer: Color,
    val lockedSurface: Color,
    val lockedBorder: Color,
    val lockedIcon: Color,
    val unitColors: Map<String, Pair<Color, Color>>,
    val leagueColors: Map<String, Color>
)

private val LightExtraColors = ConcoursExtraColors(
    gold = GoldXp,
    goldDark = GoldDark,
    goldContainer = GoldContainerLight,
    onGoldContainer = GoldOnContainerLight,
    heart = CoralHeart,
    heartDark = CoralDark,
    heartContainer = CoralContainerLight,
    onHeartContainer = CoralOnContainerLight,
    gem = CobaltGem,
    gemDark = CobaltDark,
    gemContainer = CobaltContainerLight,
    onGemContainer = CobaltOnContainerLight,
    streak = FlameStreak,
    streakContainer = FlameContainerLight,
    lockedSurface = LockedNodeSurface,
    lockedBorder = LockedNodeBorder,
    lockedIcon = LockedNodeIcon,
    unitColors = mapOf(
        "emerald" to (EmeraldPrimary to EmeraldDark),
        "cobalt" to (CobaltGem to CobaltDark),
        "violet" to (VioletUnit to VioletDark),
        "teal" to (TealUnit to TealDark),
        "amber" to (GoldXp to GoldDark)
    ),
    leagueColors = mapOf(
        "Bronze" to LeagueBronze,
        "Argent" to LeagueArgent,
        "Or" to LeagueOr,
        "Diamant" to LeagueDiamant
    )
)

private val DarkExtraColors = ConcoursExtraColors(
    gold = GoldXp,
    goldDark = GoldDark,
    goldContainer = Color(0xFF422006),
    onGoldContainer = GoldContainerLight,
    heart = CoralHeart,
    heartDark = CoralDark,
    heartContainer = CoralContainerDark,
    onHeartContainer = CoralContainerLight,
    gem = Color(0xFF60A5FA),
    gemDark = CobaltGem,
    gemContainer = Color(0xFF1E3A8A),
    onGemContainer = CobaltContainerLight,
    streak = FlameStreak,
    streakContainer = Color(0xFF431407),
    lockedSurface = DarkSurfaceVariant,
    lockedBorder = DarkOutline,
    lockedIcon = DarkOnSurfaceVariant,
    unitColors = mapOf(
        "emerald" to (EmeraldBright to EmeraldPrimary),
        "cobalt" to (Color(0xFF3B82F6) to CobaltGem),
        "violet" to (Color(0xFF8B5CF6) to VioletUnit),
        "teal" to (Color(0xFF14B8A6) to TealUnit),
        "amber" to (GoldXp to GoldDark)
    ),
    leagueColors = mapOf(
        "Bronze" to Color(0xFFD97706),
        "Argent" to Color(0xFF94A3B8),
        "Or" to Color(0xFFFACC15),
        "Diamant" to Color(0xFF22D3EE)
    )
)

val LocalConcoursColors = staticCompositionLocalOf { LightExtraColors }

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = CobaltGem,
    onSecondary = Color.White,
    secondaryContainer = CobaltContainerLight,
    onSecondaryContainer = CobaltOnContainerLight,
    tertiary = GoldXp,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainerLight,
    onTertiaryContainer = GoldOnContainerLight,
    error = CoralHeart,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = CoralOnContainerLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = Color(0xFF052E16),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldContainerLight,
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = CobaltContainerLight,
    tertiary = GoldXp,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = GoldContainerLight,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralContainerLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val ConcoursShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ConcoursGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalConcoursColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ConcoursShapes,
            content = content
        )
    }
}
