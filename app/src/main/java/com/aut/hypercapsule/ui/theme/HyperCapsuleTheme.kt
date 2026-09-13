package com.aut.hypercapsule.ui.theme

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Pure Miuix theme. Color mode + palette + optional Monet key color,
 * mirroring KernelSU's ThemeController wiring without Material3 mixing.
 */
@Composable
fun HyperCapsuleTheme(
    dynamicColorEnabled: Boolean,
    colorMode: Int,
    paletteName: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (colorMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val paletteStyle = remember(paletteName) {
        runCatching { ThemePaletteStyle.valueOf(paletteName) }
            .getOrDefault(ThemePaletteStyle.Expressive)
    }
    val controller = remember(dynamicColorEnabled, colorMode, paletteStyle, darkTheme) {
        ThemeController(
            colorSchemeMode = when {
                dynamicColorEnabled && darkTheme -> ColorSchemeMode.MonetDark
                dynamicColorEnabled -> ColorSchemeMode.MonetLight
                else -> when (colorMode) {
                    1 -> ColorSchemeMode.Light
                    2 -> ColorSchemeMode.Dark
                    else -> ColorSchemeMode.System
                }
            },
            keyColor = if (dynamicColorEnabled) null else FallbackBlue,
            colorSpec = ThemeColorSpec.Spec2025,
            paletteStyle = paletteStyle,
            isDark = darkTheme,
        )
    }

    // KernelSU: fully transparent system bars + no nav contrast scrim.
    // LocalContext may be a ContextThemeWrapper — unwrap to the Activity.
    DisposableEffect(darkTheme, context) {
        val activity = context.findActivity()
        if (activity != null) {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ) { darkTheme },
            )
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
            activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
            activity.window.isNavigationBarContrastEnforced = false
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
        onDispose { }
    }

    MiuixTheme(controller = controller) {
        content()
    }
}

private val FallbackBlue = Color(0xFF0B57D0)
