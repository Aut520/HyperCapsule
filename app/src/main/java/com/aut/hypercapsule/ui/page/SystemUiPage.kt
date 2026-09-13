package com.aut.hypercapsule.ui.page

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.component.DetailPage
import com.aut.hypercapsule.ui.component.SectionLabel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SystemUiPage(
    preferences: AppPreferences,
    onBack: () -> Unit,
    onHaptic: () -> Unit,
) {
    DetailPage(
        title = stringResource(R.string.system_ui),
        onBack = onBack,
    ) {
        item { SectionLabel(stringResource(R.string.group_transient_status_bar)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                PreferenceSwitch(
                    checked = preferences.transientCapsuleEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateTransientCapsule(it)
                    },
                    title = stringResource(R.string.transient_capsule),
                    summary = stringResource(R.string.transient_capsule_summary),
                )
                PreferenceSwitch(
                    checked = preferences.landscapeEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateLandscape(it)
                    },
                    title = stringResource(R.string.landscape_support),
                    summary = stringResource(R.string.landscape_support_summary),
                    enabled = preferences.transientCapsuleEnabled,
                )
                PreferenceSwitch(
                    checked = preferences.portraitEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updatePortrait(it)
                    },
                    title = stringResource(R.string.portrait_support),
                    summary = stringResource(R.string.portrait_support_summary),
                    enabled = preferences.transientCapsuleEnabled,
                )
            }
        }

        item { SectionLabel(stringResource(R.string.group_status_bar_behavior)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                PreferenceSwitch(
                    checked = preferences.regularStatusBarEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateRegularStatusBar(it)
                    },
                    title = stringResource(R.string.regular_status_bar),
                    summary = stringResource(R.string.regular_status_bar_summary),
                )
                PreferenceSwitch(
                    checked = preferences.hideLandscapeIsland,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateHideLandscapeIsland(it)
                    },
                    title = stringResource(R.string.hide_landscape_island),
                    summary = stringResource(R.string.hide_landscape_island_summary),
                )
                PreferenceSwitch(
                    checked = preferences.includePrivacyIndicator,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateIncludePrivacy(it)
                    },
                    title = stringResource(R.string.include_privacy_indicator),
                    summary = stringResource(R.string.include_privacy_indicator_summary),
                    enabled = preferences.transientCapsuleEnabled || preferences.regularStatusBarEnabled,
                )
            }
        }

        item { SectionLabel(stringResource(R.string.group_visual_behavior)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                PreferenceSwitch(
                    checked = preferences.capsuleAnimationEnabled,
                    onCheckedChange = {
                        onHaptic()
                        preferences.updateCapsuleAnimation(it)
                    },
                    title = stringResource(R.string.capsule_animation),
                    summary = stringResource(R.string.capsule_animation_summary),
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                insideMargin = androidx.compose.foundation.layout.PaddingValues(16.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.primaryContainer,
                    contentColor = MiuixTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.hook_pending_note),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    summary: String,
    enabled: Boolean = true,
) {
    SwitchPreference(
        checked = checked,
        onCheckedChange = onCheckedChange,
        title = title,
        summary = summary,
        enabled = enabled,
    )
}
