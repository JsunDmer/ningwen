package com.jsundmer.ningwen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
  primary = Brand,
  background = Bg,
  surface = Card,
  onBackground = TextMain,
  onSurface = TextMain,
)

@Composable
fun NingwenTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColors,
    typography = NingwenTypography,
    content = content,
  )
}
