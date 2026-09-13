package com.aut.hypercapsule

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.HyperCapsuleApp
import com.aut.hypercapsule.ui.theme.HyperCapsuleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            HyperCapsuleTheme(
                dynamicColorEnabled = preferences.dynamicColorEnabled,
                colorMode = preferences.themeColorMode,
                paletteName = preferences.themePalette,
            ) {
                HyperCapsuleApp(preferences = preferences)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // HyperOS can re-apply a black nav-bar scrim after rotation/density change.
        applyEdgeToEdge()
    }

    /**
     * Must run on the Activity before composition.
     *
     * SystemBarStyle.auto() defaults navBarContrastEnforced=true, which paints a
     * black strip under transparent navigation bars on HyperOS. KernelSU turns
     * that off explicitly after enableEdgeToEdge — do the same here with `this`,
     * not via LocalContext unwrapping.
     */
    private fun applyEdgeToEdge() {
        val dark =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { dark },
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { dark },
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.isNavigationBarContrastEnforced = false
    }
}
