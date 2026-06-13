package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = SophisticatedDarkAccent,
    onPrimary = SophisticatedDarkAccentText,
    primaryContainer = SophisticatedDarkSurface,
    onPrimaryContainer = SophisticatedDarkText,
    secondary = SophisticatedDarkAccent,
    onSecondary = SophisticatedDarkAccentText,
    background = SophisticatedDarkBackground,
    onBackground = SophisticatedDarkText,
    surface = SophisticatedDarkSurface,
    onSurface = SophisticatedDarkText,
    surfaceVariant = SophisticatedDarkSurface,
    onSurfaceVariant = SophisticatedDarkSecondaryText,
    outline = SophisticatedDarkBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = SophisticatedDarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
