package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = DarkAmberPrimary,
  onPrimary = OnDarkAmberPrimary,
  primaryContainer = DarkPrimaryContainer,
  onPrimaryContainer = OnDarkPrimaryContainer,
  secondary = DarkSlateSecondary,
  onSecondary = OnDarkSlateSecondary,
  secondaryContainer = DarkSecondaryContainer,
  onSecondaryContainer = OnDarkSecondaryContainer,
  tertiary = DarkTealTertiary,
  onTertiary = OnDarkTealTertiary,
  tertiaryContainer = DarkTertiaryContainer,
  onTertiaryContainer = OnDarkTertiaryContainer,
  background = DarkBackground,
  onBackground = OnDarkSurface,
  surface = DarkSurface,
  onSurface = OnDarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = OnDarkSurfaceVariant,
  outline = DarkOutline,
  error = CriticalCrimson,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = SlateNavyPrimary,
  onPrimary = OnSlateNavyPrimary,
  primaryContainer = SlatePrimaryContainer,
  onPrimaryContainer = OnSlatePrimaryContainer,
  secondary = SafetyAmberSecondary,
  onSecondary = OnSafetyAmberSecondary,
  secondaryContainer = AmberSecondaryContainer,
  onSecondaryContainer = OnAmberSecondaryContainer,
  tertiary = TealLogisticsTertiary,
  onTertiary = OnTealLogisticsTertiary,
  tertiaryContainer = TealTertiaryContainer,
  onTertiaryContainer = OnTealTertiaryContainer,
  background = LightBackground,
  onBackground = OnLightSurface,
  surface = LightSurface,
  onSurface = OnLightSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = OnLightSurfaceVariant,
  outline = LightOutline,
  error = CriticalCrimson,
  onError = Color.White
)

@Composable
fun GudangPintarTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
