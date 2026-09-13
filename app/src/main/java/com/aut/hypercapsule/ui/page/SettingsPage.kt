package com.aut.hypercapsule.ui.page

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.component.RootPage
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    preferences: AppPreferences,
    bottomContentPadding: Dp,
    onHaptic: () -> Unit,
) {
    val context = LocalContext.current
    val resetMessage = stringResource(R.string.appearance_reset_done)
    val blurSupported = isRenderEffectSupported()

    RootPage(
        title = stringResource(R.string.nav_settings),
        bottomContentPadding = bottomContentPadding,
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.dynamic_color),
                    summary = stringResource(R.string.dynamic_color_summary),
                    startAction = { SettingsIcon(Icons.Rounded.Palette) },
                    checked = preferences.dynamicColorEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateDynamicColor(it)
                    },
                )
                SwitchPreference(
                    title = stringResource(R.string.floating_navigation),
                    summary = stringResource(R.string.floating_navigation_summary),
                    startAction = { SettingsIcon(Icons.Rounded.Dock) },
                    checked = preferences.floatingNavigationEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateFloatingNavigation(it)
                    },
                )
                AnimatedVisibility(
                    visible = preferences.floatingNavigationEnabled,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        expandFrom = Alignment.Top,
                    ) + fadeIn(),
                    exit = shrinkVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        shrinkTowards = Alignment.Top,
                    ) + fadeOut(),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.liquid_glass_navigation),
                        summary = stringResource(R.string.liquid_glass_navigation_summary),
                        startAction = { SettingsIcon(Icons.Rounded.BlurOn) },
                        checked = preferences.liquidGlassEnabled,
                        onCheckedChange = {
                            onHaptic()
                            preferences.updateLiquidGlass(it)
                        },
                    )
                }
                if (preferences.floatingNavigationEnabled && preferences.liquidGlassEnabled && !blurSupported) {
                    Text(
                        text = stringResource(R.string.liquid_glass_fallback),
                        modifier = Modifier.padding(start = 62.dp, end = 18.dp, bottom = 14.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.haptic_feedback),
                    summary = stringResource(R.string.haptic_feedback_summary),
                    startAction = { SettingsIcon(Icons.Rounded.TouchApp) },
                    checked = preferences.hapticFeedbackEnabled,
                    onCheckedChange = {
                        if (it) onHaptic()
                        preferences.updateHapticFeedback(it)
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.reset_appearance),
                    summary = stringResource(R.string.reset_appearance_summary),
                    startAction = { SettingsIcon(Icons.Rounded.RestartAlt) },
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

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.padding(end = 6.dp),
        tint = MiuixTheme.colorScheme.onBackground,
    )
}
