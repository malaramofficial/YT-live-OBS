package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudioColorScheme = darkColorScheme(
    primary = YouTubeRed,
    onPrimary = TextPrimary,
    primaryContainer = LiveRedDark,
    onPrimaryContainer = TextPrimary,
    secondary = AccentBlue,
    onSecondary = TextPrimary,
    secondaryContainer = StudioCardBgElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = SuperChatGold,
    onTertiary = StudioBlack,
    background = StudioBlack,
    onBackground = TextPrimary,
    surface = StudioCardBg,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardBgElevated,
    onSurfaceVariant = TextSecondary,
    outline = StudioCardBorder
)

@Composable
fun MalaramLiveTheme(
    darkTheme: Boolean = true, // Priority on dark creator studio theme
    content: @Composable () -> Unit
) {
    val colorScheme = StudioColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = StudioBlack.toArgb()
                window.navigationBarColor = StudioBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
