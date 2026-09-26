package com.aut.hypercapsule.ui.page

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.component.RootPage
import com.aut.hypercapsule.ui.component.SectionLabel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported

/**
 * 界面主题与外观。纯 Miuix 偏好组件，作为顶层「设置」Tab 直接展示。
 */
@Composable
fun ThemePage(
    preferences: AppPreferences,
    bottomContentPadding: Dp,
    onHaptic: () -> Unit,
) {
    val context = LocalContext.current
    val resetMessage = stringResource(R.string.appearance_reset_done)
    val blurSupported = isRenderEffectSupported()
    val colorModes = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
    )
    val paletteLabels = listOf(
        stringResource(R.string.palette_tonal_spot),
        stringResource(R.string.palette_vibrant),
        stringResource(R.string.palette_expressive),
        stringResource(R.string.palette_neutral),
    )
    val paletteKeys = AppPreferences.PALETTES

    RootPage(
        title = stringResource(R.string.nav_settings),
        bottomContentPadding = bottomContentPadding,
    ) {
        item { SectionLabel(stringResource(R.string.theme_section_color)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                SwitchPreference(
                    title = stringResource(R.string.dynamic_color),
                    summary = stringResource(R.string.dynamic_color_summary),
                    checked = preferences.dynamicColorEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateDynamicColor(it)
                    },
                )
                OverlayDropdownPreference(
                    title = stringResource(R.string.theme_color_mode),
                    items = colorModes,
                    selectedIndex = preferences.themeColorMode.coerceIn(0, 2),
                    enabled = !preferences.dynamicColorEnabled,
                    onSelectedIndexChange = { index ->
                        onHaptic()
                        preferences.updateThemeColorMode(index)
                    },
                )
                OverlayDropdownPreference(
                    title = stringResource(R.string.theme_palette),
                    items = paletteLabels,
                    selectedIndex = paletteKeys
                        .indexOf(preferences.themePalette)
                        .coerceAtLeast(0),
                    enabled = !preferences.dynamicColorEnabled,
                    onSelectedIndexChange = { index ->
                        onHaptic()
                        preferences.updateThemePalette(paletteKeys[index])
                    },
                )
                if (preferences.dynamicColorEnabled) {
                    BasicComponent(
                        title = stringResource(R.string.theme_monet_note),
                        onClick = {},
                    )
                }
            }
        }

        item { SectionLabel(stringResource(R.string.group_appearance)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                SwitchPreference(
                    title = stringResource(R.string.floating_navigation),
                    summary = stringResource(R.string.floating_navigation_summary),
                    checked = preferences.floatingNavigationEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateFloatingNavigation(it)
                    },
                )
                SwitchPreference(
                    title = stringResource(R.string.liquid_glass_navigation),
                    summary = stringResource(R.string.liquid_glass_navigation_summary),
                    checked = preferences.liquidGlassEnabled,
                    enabled = preferences.floatingNavigationEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateLiquidGlass(it)
                    },
                )
                if (preferences.floatingNavigationEnabled &&
                    preferences.liquidGlassEnabled &&
                    !blurSupported
                ) {
                    BasicComponent(
                        title = stringResource(R.string.liquid_glass_fallback),
                        onClick = {},
                    )
                }
                SwitchPreference(
                    title = stringResource(R.string.haptic_feedback),
                    summary = stringResource(R.string.haptic_feedback_summary),
                    checked = preferences.hapticFeedbackEnabled,
                    onCheckedChange = {
                        if (it) onHaptic()
                        preferences.updateHapticFeedback(it)
                    },
                )
            }
        }

        item { SectionLabel(stringResource(R.string.group_reset)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = stringResource(R.string.reset_appearance),
                    summary = stringResource(R.string.reset_appearance_summary),
                    onClick = {
                        onHaptic()
                        preferences.resetAppearance()
                        Toast.makeText(context, resetMessage, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}
