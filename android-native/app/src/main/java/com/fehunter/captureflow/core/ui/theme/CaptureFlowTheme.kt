package com.fehunter.captureflow.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val Navy = Color(0xFF07182B)
private val DeepBlue = Color(0xFF0C2B46)
private val Ocean = Color(0xFF164B62)
private val Cyan = Color(0xFF63C4F0)
private val LightBackground = Color(0xFFF5F7F9)
private val LightForeground = Color(0xFF0A1B33)
private val LightMuted = Color(0xFF566878)
private val LightLine = Color(0xFFDCE5EA)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F83),
    onPrimary = Color.White,
    secondary = Color(0xFF164B62),
    onSecondary = Color.White,
    tertiary = Color(0xFF29B3C7),
    onTertiary = LightForeground,
    background = LightBackground,
    onBackground = LightForeground,
    surface = Color.White,
    onSurface = LightForeground,
    surfaceVariant = Color(0xFFE6EEF2),
    onSurfaceVariant = LightMuted,
    outline = LightLine,
    outlineVariant = Color(0xFFDCE5EA),
    primaryContainer = Color(0xFFE3F2F5),
    onPrimaryContainer = LightForeground,
    secondaryContainer = Color(0xFFE6EEF2),
    onSecondaryContainer = LightForeground,
)

private val DarkColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Navy,
    secondary = Color(0xFF58B9D6),
    onSecondary = Navy,
    tertiary = Color(0xFF8ADCF0),
    onTertiary = Navy,
    background = Navy,
    onBackground = Color(0xFFF3F7FB),
    surface = DeepBlue,
    onSurface = Color(0xFFF3F7FB),
    surfaceVariant = Ocean,
    onSurfaceVariant = Color(0xFFA9C0D2),
    outline = Color(0xFF52758E),
    outlineVariant = Color(0xFF1E4663),
    primaryContainer = Color(0xFF12335A),
    onPrimaryContainer = Color(0xFFF3F7FB),
    secondaryContainer = Color(0xFF164B62),
    onSecondaryContainer = Color(0xFFF3F7FB),
)

private val CaptureShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CaptureFlowTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = CaptureShapes,
        content = content,
    )
}

/** Subtle cyan/blue ambient gradients used by the designer's light and dark canvases. */
@Composable
fun CaptureFlowBackdrop(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background == Navy
    val cyanGlow = if (dark) Color(0xFF29B3C7).copy(alpha = 0.30f) else Color(0xFF29B3C7).copy(alpha = 0.22f)
    val blueGlow = if (dark) Color(0xFF2E6BB0).copy(alpha = 0.34f) else Color(0xFF2E6BB0).copy(alpha = 0.16f)
    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 150.dp, y = (-120).dp).size(390.dp)
                .background(Brush.radialGradient(listOf(cyanGlow, Color.Transparent)), CircleShape),
        )
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = (-210).dp, y = 40.dp).size(390.dp)
                .background(Brush.radialGradient(listOf(blueGlow, Color.Transparent)), CircleShape),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 180.dp, y = 120.dp).size(390.dp)
                .background(Brush.radialGradient(listOf(cyanGlow.copy(alpha = 0.7f), Color.Transparent)), CircleShape),
        )
    }
}
