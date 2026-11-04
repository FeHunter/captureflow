package com.fehunter.captureflow.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * CaptureFlow Personal Notebook palette.
 *
 * Keep feature UI bound to MaterialTheme.colorScheme instead of defining
 * feature-specific colors. This keeps Light and Dark themes consistent.
 */
object CaptureFlowPalette {
    // Light surfaces and content
    val Paper = Color(0xFFF3EBDD)
    val PaperWhite = Color(0xFFFFFCF5)
    val PaperVariant = Color(0xFFEAE0CE)
    val Ochre = Color(0xFF9A7138)
    val Sage = Color(0xFF7C8063)
    val Terracotta = Color(0xFFC77A48)
    val Ink = Color(0xFF39382F)
    val MutedInk = Color(0xFF756F61)
    val PaperLine = Color(0xFFD8CCB8)

    // Dark surfaces and content
    val Night = Color(0xFF292A27)
    val NightSurface = Color(0xFF343630)
    val NightVariant = Color(0xFF41433A)
    val WarmGold = Color(0xFFD5B47A)
    val NightSage = Color(0xFFB6B991)
    val NightTerracotta = Color(0xFFD99163)
    val WarmWhite = Color(0xFFF0E8D9)
    val WarmMuted = Color(0xFFC2B9A9)
    val NightLine = Color(0xFF55564C)
}

private val LightColors = lightColorScheme(
    primary = CaptureFlowPalette.Ochre,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D9BE),
    onPrimaryContainer = CaptureFlowPalette.Ink,
    secondary = CaptureFlowPalette.Sage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E3D5),
    onSecondaryContainer = CaptureFlowPalette.Ink,
    tertiary = CaptureFlowPalette.Terracotta,
    onTertiary = Color.White,
    background = CaptureFlowPalette.Paper,
    onBackground = CaptureFlowPalette.Ink,
    surface = CaptureFlowPalette.PaperWhite,
    onSurface = CaptureFlowPalette.Ink,
    surfaceVariant = CaptureFlowPalette.PaperVariant,
    onSurfaceVariant = CaptureFlowPalette.MutedInk,
    outline = CaptureFlowPalette.PaperLine,
    outlineVariant = CaptureFlowPalette.PaperLine,
    error = Color(0xFFB34F42),
    onError = Color.White,
    errorContainer = Color(0xFFF4DDD7),
    onErrorContainer = Color(0xFF54231D),
)

private val DarkColors = darkColorScheme(
    primary = CaptureFlowPalette.WarmGold,
    onPrimary = CaptureFlowPalette.Night,
    primaryContainer = Color(0xFF51432F),
    onPrimaryContainer = CaptureFlowPalette.WarmWhite,
    secondary = CaptureFlowPalette.NightSage,
    onSecondary = CaptureFlowPalette.Night,
    secondaryContainer = Color(0xFF41483A),
    onSecondaryContainer = CaptureFlowPalette.WarmWhite,
    tertiary = CaptureFlowPalette.NightTerracotta,
    onTertiary = CaptureFlowPalette.Night,
    background = CaptureFlowPalette.Night,
    onBackground = CaptureFlowPalette.WarmWhite,
    surface = CaptureFlowPalette.NightSurface,
    onSurface = CaptureFlowPalette.WarmWhite,
    surfaceVariant = CaptureFlowPalette.NightVariant,
    onSurfaceVariant = CaptureFlowPalette.WarmMuted,
    outline = CaptureFlowPalette.NightLine,
    outlineVariant = CaptureFlowPalette.NightLine,
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD4),
)

private val CaptureTypography = Typography().let { base ->
    // Native serif/sans fallbacks keep the app self-contained. Custom Lora,
    // Inter and Caveat font assets can be added later without runtime downloads.
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = FontFamily.Serif),
        displayMedium = base.displayMedium.copy(fontFamily = FontFamily.Serif),
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif),
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif),
        titleSmall = base.titleSmall.copy(fontFamily = FontFamily.SansSerif),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif),
        labelLarge = base.labelLarge.copy(fontFamily = FontFamily.SansSerif),
        labelMedium = base.labelMedium.copy(fontFamily = FontFamily.SansSerif),
        labelSmall = base.labelSmall.copy(fontFamily = FontFamily.SansSerif),
    )
}

private val CaptureShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun CaptureFlowTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CaptureTypography,
        shapes = CaptureShapes,
        content = content,
    )
}

/**
 * Subtle warm ambient background. Components should use the semantic
 * Material color scheme; this backdrop is decorative only.
 */
@Composable
fun CaptureFlowBackdrop(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background == CaptureFlowPalette.Night
    val warmGlow = if (dark) {
        CaptureFlowPalette.WarmGold.copy(alpha = 0.055f)
    } else {
        CaptureFlowPalette.Ochre.copy(alpha = 0.075f)
    }
    val sageGlow = if (dark) {
        CaptureFlowPalette.NightSage.copy(alpha = 0.045f)
    } else {
        CaptureFlowPalette.Sage.copy(alpha = 0.055f)
    }

    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.align(Alignment.TopEnd)
                .offset(x = 150.dp, y = (-130).dp)
                .size(380.dp)
                .background(Brush.radialGradient(listOf(warmGlow, Color.Transparent)), CircleShape),
        )
        Box(
            Modifier.align(Alignment.BottomStart)
                .offset(x = (-190).dp, y = 130.dp)
                .size(400.dp)
                .background(Brush.radialGradient(listOf(sageGlow, Color.Transparent)), CircleShape),
        )
    }
}
