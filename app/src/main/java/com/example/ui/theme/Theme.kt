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

private val LightColorScheme =
  lightColorScheme(
    primary = CrestGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = CrestGreenContainer,
    onPrimaryContainer = OnCrestGreenContainer,
    secondary = GateSlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = GateSlateContainer,
    onSecondaryContainer = OnGateSlateContainer,
    tertiary = PassGoldTertiary,
    onTertiary = Color.White,
    tertiaryContainer = PassGoldContainer,
    onTertiaryContainer = OnPassGoldContainer,
    error = StatusExpiredAccent,
    onError = Color.White,
    errorContainer = StatusExpiredBgLight,
    onErrorContainer = StatusExpiredFgLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLow = Color.White,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = CrestGreenDark,
    onPrimary = OnCrestGreenContainer,
    primaryContainer = CrestGreenContainerDark,
    onPrimaryContainer = OnCrestGreenContainerDark,
    secondary = GateSlateDark,
    onSecondary = OnGateSlateContainer,
    secondaryContainer = GateSlateContainerDark,
    onSecondaryContainer = OnGateSlateContainerDark,
    tertiary = PassGoldDark,
    onTertiary = OnPassGoldContainer,
    tertiaryContainer = PassGoldContainerDark,
    onTertiaryContainer = OnPassGoldContainerDark,
    error = StatusExpiredFgDark,
    onError = StatusExpiredBgDark,
    errorContainer = StatusExpiredBgDark,
    onErrorContainer = StatusExpiredFgDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLow = SurfaceContainerDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
  )

@Immutable
data class GateStatusColors(
  val validBg: Color,
  val validFg: Color,
  val validAccent: Color,
  val expiringBg: Color,
  val expiringFg: Color,
  val expiringAccent: Color,
  val expiredBg: Color,
  val expiredFg: Color,
  val expiredAccent: Color,
)

val LocalGateStatusColors = staticCompositionLocalOf {
  GateStatusColors(
    validBg = StatusValidBgLight,
    validFg = StatusValidFgLight,
    validAccent = StatusValidAccent,
    expiringBg = StatusExpiringBgLight,
    expiringFg = StatusExpiringFgLight,
    expiringAccent = StatusExpiringAccent,
    expiredBg = StatusExpiredBgLight,
    expiredFg = StatusExpiredFgLight,
    expiredAccent = StatusExpiredAccent,
  )
}

val GatePassShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun GatePassTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val statusColors =
    if (darkTheme) {
      GateStatusColors(
        validBg = StatusValidBgDark,
        validFg = StatusValidFgDark,
        validAccent = StatusValidAccent,
        expiringBg = StatusExpiringBgDark,
        expiringFg = StatusExpiringFgDark,
        expiringAccent = StatusExpiringAccent,
        expiredBg = StatusExpiredBgDark,
        expiredFg = StatusExpiredFgDark,
        expiredAccent = StatusExpiredAccent,
      )
    } else {
      GateStatusColors(
        validBg = StatusValidBgLight,
        validFg = StatusValidFgLight,
        validAccent = StatusValidAccent,
        expiringBg = StatusExpiringBgLight,
        expiringFg = StatusExpiringFgLight,
        expiringAccent = StatusExpiringAccent,
        expiredBg = StatusExpiredBgLight,
        expiredFg = StatusExpiredFgLight,
        expiredAccent = StatusExpiredAccent,
      )
    }

  CompositionLocalProvider(LocalGateStatusColors provides statusColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = GatePassTypography,
      shapes = GatePassShapes,
      content = content,
    )
  }
}
