package com.pedilo.app.ui.publicuser

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val PediloOrange = Color(0xFFFF7308)
val PediloOrangeDark = Color(0xFFC84A00)
val PediloBg = Color(0xFF02070A)
val PediloPanel = Color(0xFF09141A)
val PediloPanelSoft = Color(0xFF122129)
val PediloLine = Color(0xFF2B3E45)
val PediloText = Color(0xFFF7F7F7)
val PediloMuted = Color(0xFFB9C3C8)
val PediloGreen = Color(0xFF69D125)
val PediloOrangeSoft = Color(0xFFFFA22E)
val PediloWarning = Color(0xFFFFC04D)
val PediloOverlay = Color(0x8A111F26)
val PediloCyan = Color(0xFF3B7E8E)
val PediloPink = Color(0xFF9A4A78)
val PediloOlive = Color(0xFF5E7D2B)
val PediloGoldLine = Color(0xFF6E4A2C)
val PediloPressed = Color(0xFFFF8D20)
val PediloWarmDepth = Color(0xFF3A1C0B)
val PediloCream = Color(0xFFEFE8DE)

val PediloPrimaryBrush: Brush = Brush.verticalGradient(
    listOf(PediloWarning, PediloOrangeSoft, PediloOrangeDark),
)

val PediloCardBrush: Brush = Brush.verticalGradient(
    listOf(PediloPanelSoft.copy(alpha = 0.92f), PediloPanel),
)

val PediloWarmPanelBrush: Brush = Brush.linearGradient(
    listOf(PediloOrangeDark.copy(alpha = 0.34f), PediloPanelSoft.copy(alpha = 0.84f), PediloPanel),
)

fun Modifier.pediloCardDepth(shape: Shape = RoundedCornerShape(14.dp)): Modifier =
    shadow(
        elevation = 11.dp,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.34f),
        spotColor = PediloOrange.copy(alpha = 0.10f),
    )

fun Modifier.pediloButtonDepth(shape: Shape = RoundedCornerShape(14.dp)): Modifier =
    shadow(
        elevation = 10.dp,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.30f),
        spotColor = PediloOrange.copy(alpha = 0.26f),
    )

private val PublicScheme = darkColorScheme(
    primary = PediloOrange,
    secondary = PediloOrange,
    background = PediloBg,
    surface = PediloPanel,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = PediloText,
    onSurface = PediloText,
)

@Composable
fun PublicTheme(content: @Composable () -> Unit) {
    val unused = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = PublicScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
