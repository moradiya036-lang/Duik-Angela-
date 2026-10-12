package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = DuikCyan,
  onPrimary = Color(0xFF00363D),
  primaryContainer = Color(0xFF004F58),
  onPrimaryContainer = Color(0xFF8CEFFC),
  secondary = DuikAmber,
  onSecondary = Color(0xFF452B00),
  secondaryContainer = Color(0xFF633F00),
  onSecondaryContainer = Color(0xFFFFDF9E),
  tertiary = DuikMagenta,
  onTertiary = Color(0xFF5D0026),
  background = DuikDarkBg,
  onBackground = DuikDarkOnSurface,
  surface = DuikDarkSurface,
  onSurface = DuikDarkOnSurface,
  surfaceVariant = DuikDarkSurfaceVariant,
  onSurfaceVariant = DuikDarkOnSurfaceMuted,
  outline = DuikDarkSurfaceBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
