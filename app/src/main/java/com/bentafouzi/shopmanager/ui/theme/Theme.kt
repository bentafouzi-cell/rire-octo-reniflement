package com.bentafouzi.shopmanager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Green = Color(0xFF16834B)
private val DarkGreen = Color(0xFF0C5531)
private val LightGreen = Color(0xFFE8F5EC)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = LightGreen,
    secondary = DarkGreen,
    background = Color(0xFFF7F9F7),
    surface = Color.White
)

@Composable
fun ShopTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme(primary = Green) else LightColors, content = content)
}
