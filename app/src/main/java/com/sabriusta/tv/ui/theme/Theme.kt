package com.sabriusta.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sabriusta.tv.data.prefs.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Altin,
    onPrimary = Siyah,
    primaryContainer = AltinKoyu,
    onPrimaryContainer = Siyah,
    secondary = KoyuLacivertAcik,
    onSecondary = BeyazKirik,
    secondaryContainer = KoyuLacivert,
    onSecondaryContainer = BeyazKirik,
    tertiary = AltinNeon,
    background = Siyah,
    onBackground = BeyazKirik,
    surface = SiyahYumusak,
    onSurface = BeyazKirik,
    surfaceVariant = KoyuLacivert,
    onSurfaceVariant = GriMetin,
    outline = Lacivert,
    error = Kirmizi,
    onError = Siyah
)

private val LightColors = lightColorScheme(
    primary = AltinKoyu,
    onPrimary = Siyah,
    primaryContainer = Altin,
    onPrimaryContainer = Siyah,
    secondary = Lacivert,
    onSecondary = BeyazKirik,
    secondaryContainer = Color_LightSecondaryContainer,
    onSecondaryContainer = AcikMetin,
    background = AcikZemin,
    onBackground = AcikMetin,
    surface = AcikYuzey,
    onSurface = AcikMetin,
    surfaceVariant = Color_LightSurfaceVariant,
    onSurfaceVariant = Color_LightOnSurfaceVariant,
    outline = Color_LightOutline,
    error = Kirmizi,
    onError = AcikYuzey
)

val SabriShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SabriUstaTvTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        typography = SabriTypography,
        shapes = SabriShapes,
        content = content
    )
}
