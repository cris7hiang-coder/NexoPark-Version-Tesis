package com.example.parking.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.example.parking.R
private val LightColorScheme = lightColorScheme(
    primary = AzulPetroleo,
    onPrimary = FondoElevado,

    secondary = VerdeOlivaClaro,
    onSecondary = AzulPetroleoFuerte,

    background = FondoClaro,
    onBackground = TextoBase,

    surface = FondoCard,
    onSurface = TextoBase,

    surfaceVariant = FondoElevado,
    onSurfaceVariant = TextoSecundario,

    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = AzulPetroleoFuerte,

    error = RojoCoral,
    onError = FondoElevado,
    errorContainer = RojoCoral.copy(alpha = 0.12f),
    onErrorContainer = RojoCoral,

    outline = DivisorSuave,
    outlineVariant = DivisorClaro,

    scrim = ScrimBase.copy(alpha = 0.32f),
    surfaceTint = AzulPetroleo
)

private val DarkColorScheme = darkColorScheme(
    primary = AzulPetroleo,
    onPrimary = FondoElevado,

    secondary = VerdeOlivaClaro,
    onSecondary = AzulPetroleoFuerte,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnBackground,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0B0B0),

    primaryContainer = Color(0xFF233F52),
    onPrimaryContainer = FondoElevado,

    error = RojoCoral,
    onError = FondoElevado,
    errorContainer = Color(0xFF4A1F1F),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = DivisorSuave.copy(alpha = 0.45f),
    outlineVariant = DivisorClaro.copy(alpha = 0.25f),

    scrim = ScrimBase.copy(alpha = 0.45f),
    surfaceTint = AzulPetroleo
)
val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
@Composable
fun ParkingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}


