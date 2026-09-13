package com.aut.hypercapsule.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
fun HyperCapsuleTheme(
    dynamicColorEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val controller = remember(dynamicColorEnabled, darkTheme) {
        ThemeController(
            colorSchemeMode = ColorSchemeMode.MonetSystem,
            keyColor = if (dynamicColorEnabled) null else FallbackBlue,
            colorSpec = ThemeColorSpec.Spec2025,
            paletteStyle = ThemePaletteStyle.Expressive,
            isDark = darkTheme,
        )
    }

    MiuixTheme(controller = controller) {
        LaunchedEffect(darkTheme) {
            val window = (context as? Activity)?.window ?: return@LaunchedEffect
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
        content()
    }
}

private val FallbackBlue = Color(0xFF0B57D0)
