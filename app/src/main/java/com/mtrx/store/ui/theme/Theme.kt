package com.mtrx.store.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mtrx.store.data.local.preferences.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = MtrxGold,
    onPrimary = MtrxDarkBg,
    primaryContainer = MtrxGoldContainer,
    onPrimaryContainer = MtrxGoldLight,
    secondary = MtrxGoldLight,
    onSecondary = MtrxDarkBg,
    background = MtrxDarkBg,
    onBackground = TextPrimaryDark,
    surface = MtrxDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = MtrxDarkCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = MtrxDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = MtrxGoldDark,
    onPrimary = MtrxLightSurface,
    primaryContainer = MtrxGoldLight,
    onPrimaryContainer = MtrxGoldDark,
    secondary = MtrxGold,
    onSecondary = MtrxLightSurface,
    background = MtrxLightBg,
    onBackground = TextPrimaryLight,
    surface = MtrxLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = MtrxLightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = MtrxLightBorder
)

@Composable
fun MTRXStoreTheme(
    themeSetting: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeSetting) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
