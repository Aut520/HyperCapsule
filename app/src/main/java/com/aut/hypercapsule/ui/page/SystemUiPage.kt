package com.aut.hypercapsule.ui.page

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aut.hypercapsule.CapsuleConfig
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.component.DetailPage
import com.aut.hypercapsule.ui.component.SectionLabel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SystemUiPage(
    preferences: AppPreferences,
    onBack: () -> Unit,
    onHaptic: () -> Unit,
) {
    val material = preferences.capsuleMaterial
    val isOriginal = material == CapsuleConfig.ORIGINAL
    val showToneAlpha = !isOriginal
    val showBlur = material == CapsuleConfig.GAUSSIAN_BLUR ||
        material == CapsuleConfig.SOFT_GLASS ||
        material == CapsuleConfig.LIQUID_GLASS
    val showGlass = material == CapsuleConfig.SOFT_GLASS ||
        material == CapsuleConfig.LIQUID_GLASS

    DetailPage(
        title = stringResource(R.string.system_ui),
        onBack = onBack,
    ) {
        // ── ① 材质选择 ──
        item { SectionLabel(stringResource(R.string.group_material_select)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                OverlayDropdownPreference(
                    title = stringResource(R.string.capsule_material),
                    items = listOf(
                        R.string.material_original,
                        R.string.material_translucent,
                        R.string.material_gaussian,
                        R.string.material_soft_glass,
                        R.string.material_liquid_glass,
                    ).map { stringResource(it) },
                    selectedIndex = listOf(
                        CapsuleConfig.ORIGINAL,
                        CapsuleConfig.TRANSLUCENT,
                        CapsuleConfig.GAUSSIAN_BLUR,
                        CapsuleConfig.SOFT_GLASS,
                        CapsuleConfig.LIQUID_GLASS,
                    ).indexOf(material).coerceAtLeast(0),
                    onSelectedIndexChange = { index ->
                        onHaptic()
                        preferences.updateCapsuleMaterial(
                            listOf(
                                CapsuleConfig.ORIGINAL,
                                CapsuleConfig.TRANSLUCENT,
                                CapsuleConfig.GAUSSIAN_BLUR,
                                CapsuleConfig.SOFT_GLASS,
                                CapsuleConfig.LIQUID_GLASS,
                            )[index],
                        )
                    },
                )
            }
        }

        // ── ② 材质参数（条件显示） ──
        item {
            AnimatedVisibility(
                visible = showToneAlpha,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    SectionLabel(stringResource(R.string.group_material_params))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        val tones = listOf(
                            stringResource(R.string.tone_auto),
                            stringResource(R.string.tone_dark),
                            stringResource(R.string.tone_light),
                        )
                        val toneKeys = listOf(CapsuleConfig.TONE_AUTO, CapsuleConfig.TONE_DARK, CapsuleConfig.TONE_LIGHT)
                        OverlayDropdownPreference(
                            title = stringResource(R.string.capsule_tone),
                            items = tones,
                            selectedIndex = toneKeys.indexOf(preferences.capsuleTone).coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                onHaptic()
                                preferences.updateCapsuleTone(toneKeys[index])
                            },
                        )
                        Text(
                            stringResource(R.string.capsule_alpha, (preferences.capsuleAlpha * 100).toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.capsuleAlpha,
                            onValueChange = preferences::updateCapsuleAlpha,
                            valueRange = 0f..1f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = showBlur,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text(
                            stringResource(R.string.capsule_blur_radius, preferences.capsuleBlurRadius.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.capsuleBlurRadius,
                            onValueChange = preferences::updateCapsuleBlurRadius,
                            valueRange = 0f..80f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = showGlass,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    SectionLabel(stringResource(R.string.group_glass_params))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.capsule_edge_strength, (preferences.capsuleEdgeStrength * 100).toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.capsuleEdgeStrength,
                            onValueChange = preferences::updateCapsuleEdgeStrength,
                            valueRange = 0f..1f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Text(
                            stringResource(R.string.capsule_refraction_strength, (preferences.capsuleRefractionStrength * 100).toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.capsuleRefractionStrength,
                            onValueChange = preferences::updateCapsuleRefractionStrength,
                            valueRange = 0f..1f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Text(
                            stringResource(R.string.capsule_light_direction, preferences.capsuleLightDirection.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.capsuleLightDirection,
                            onValueChange = preferences::updateCapsuleLightDirection,
                            valueRange = 0f..359f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                    }
                }
            }
        }

        // ── ③ 尺寸与对齐 ──
        item { SectionLabel(stringResource(R.string.group_size_alignment)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                // 圆角半径：-1 = 自动, 0~40 = 固定值
                val radiusAuto = preferences.cornerRadius < 0f
                Text(
                    stringResource(R.string.capsule_corner_radius) + "：" +
                        if (radiusAuto) stringResource(R.string.capsule_corner_radius_auto)
                        else stringResource(R.string.capsule_corner_radius_value, preferences.cornerRadius.toInt()),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MiuixTheme.textStyles.body2,
                )
                Slider(
                    value = preferences.cornerRadius,
                    onValueChange = preferences::updateCornerRadius,
                    valueRange = -1f..40f,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Text(
                    stringResource(R.string.capsule_height, preferences.capsuleHeight.toInt()),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MiuixTheme.textStyles.body2,
                )
                Slider(
                    value = preferences.capsuleHeight,
                    onValueChange = preferences::updateCapsuleHeight,
                    valueRange = 8f..80f,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Text(
                    stringResource(R.string.horizontal_padding, preferences.horizontalPadding.toInt()),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MiuixTheme.textStyles.body2,
                )
                Slider(
                    value = preferences.horizontalPadding,
                    onValueChange = preferences::updateHorizontalPadding,
                    valueRange = 0f..64f,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Text(
                    stringResource(R.string.vertical_inset, preferences.verticalInset.toInt()),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MiuixTheme.textStyles.body2,
                )
                Slider(
                    value = preferences.verticalInset,
                    onValueChange = preferences::updateVerticalInset,
                    valueRange = 0f..32f,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Text(
                    stringResource(R.string.global_horizontal_offset, preferences.globalOffsetX.toInt()),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MiuixTheme.textStyles.body2,
                )
                Slider(
                    value = preferences.globalOffsetX.coerceIn(-200f, 200f),
                    onValueChange = preferences::updateGlobalOffsetX,
                    valueRange = -200f..200f,
                    steps = 39,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Text(
                    stringResource(R.string.global_vertical_offset, preferences.globalOffsetY.toInt()),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MiuixTheme.textStyles.body2,
                )
                Slider(
                    value = preferences.globalOffsetY.coerceIn(-200f, 200f),
                    onValueChange = preferences::updateGlobalOffsetY,
                    valueRange = -200f..200f,
                    steps = 39,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }

        // ── ④ 左右独立控制 ──
        item { SectionLabel(stringResource(R.string.group_left_right_control)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                // 左胶囊
                PreferenceSwitch(
                    checked = preferences.leftCapsuleEnabled,
                    onCheckedChange = { onHaptic(); preferences.updateLeftCapsuleEnabled(it) },
                    title = stringResource(R.string.left_capsule_enabled),
                    summary = stringResource(R.string.left_capsule_enabled_summary),
                )
                AnimatedVisibility(
                    visible = preferences.leftCapsuleEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        PreferenceSwitch(
                            checked = preferences.includeNotificationIcons,
                            onCheckedChange = { onHaptic(); preferences.updateIncludeNotification(it) },
                            title = stringResource(R.string.include_notification_icons),
                            summary = stringResource(R.string.include_notification_icons_summary),
                        )
                        Text(
                            stringResource(R.string.left_horizontal_offset, preferences.leftOffsetX.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.leftOffsetX.coerceIn(-200f, 200f),
                            onValueChange = preferences::updateLeftOffsetX,
                            valueRange = -200f..200f,
                            steps = 39,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Text(
                            stringResource(R.string.left_vertical_offset, preferences.leftOffsetY.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.leftOffsetY.coerceIn(-200f, 200f),
                            onValueChange = preferences::updateLeftOffsetY,
                            valueRange = -200f..200f,
                            steps = 39,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Text(
                            stringResource(R.string.left_padding_x, preferences.leftPaddingX.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.leftPaddingX,
                            onValueChange = preferences::updateLeftPaddingX,
                            valueRange = -32f..64f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Text(
                            stringResource(R.string.left_padding_y, preferences.leftPaddingY.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.leftPaddingY,
                            onValueChange = preferences::updateLeftPaddingY,
                            valueRange = -16f..32f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        val horizontal = listOf(
                            stringResource(R.string.align_start),
                            stringResource(R.string.align_center),
                            stringResource(R.string.align_end),
                        )
                        val vertical = listOf(
                            stringResource(R.string.align_top),
                            stringResource(R.string.align_center),
                            stringResource(R.string.align_bottom),
                        )
                        OverlayDropdownPreference(
                            title = stringResource(R.string.left_horizontal_alignment),
                            items = horizontal,
                            selectedIndex = preferences.leftHorizontalAlignment,
                            onSelectedIndexChange = preferences::updateLeftHAlign,
                        )
                        OverlayDropdownPreference(
                            title = stringResource(R.string.left_vertical_alignment),
                            items = vertical,
                            selectedIndex = preferences.leftVerticalAlignment,
                            onSelectedIndexChange = preferences::updateLeftVAlign,
                        )
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                // 右胶囊
                PreferenceSwitch(
                    checked = preferences.rightCapsuleEnabled,
                    onCheckedChange = { onHaptic(); preferences.updateRightCapsuleEnabled(it) },
                    title = stringResource(R.string.right_capsule_enabled),
                    summary = stringResource(R.string.right_capsule_enabled_summary),
                )
                AnimatedVisibility(
                    visible = preferences.rightCapsuleEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        Text(
                            stringResource(R.string.right_horizontal_offset, preferences.rightOffsetX.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.rightOffsetX.coerceIn(-200f, 200f),
                            onValueChange = preferences::updateRightOffsetX,
                            valueRange = -200f..200f,
                            steps = 39,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Text(
                            stringResource(R.string.right_vertical_offset, preferences.rightOffsetY.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.rightOffsetY.coerceIn(-200f, 200f),
                            onValueChange = preferences::updateRightOffsetY,
                            valueRange = -200f..200f,
                            steps = 39,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Text(
                            stringResource(R.string.right_padding_x, preferences.rightPaddingX.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.rightPaddingX,
                            onValueChange = preferences::updateRightPaddingX,
                            valueRange = -32f..64f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Text(
                            stringResource(R.string.right_padding_y, preferences.rightPaddingY.toInt()),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Slider(
                            value = preferences.rightPaddingY,
                            onValueChange = preferences::updateRightPaddingY,
                            valueRange = -16f..32f,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        val horizontal = listOf(
                            stringResource(R.string.align_start),
                            stringResource(R.string.align_center),
                            stringResource(R.string.align_end),
                        )
                        val vertical = listOf(
                            stringResource(R.string.align_top),
                            stringResource(R.string.align_center),
                            stringResource(R.string.align_bottom),
                        )
                        OverlayDropdownPreference(
                            title = stringResource(R.string.right_horizontal_alignment),
                            items = horizontal,
                            selectedIndex = preferences.rightHorizontalAlignment,
                            onSelectedIndexChange = preferences::updateRightHAlign,
                        )
                        OverlayDropdownPreference(
                            title = stringResource(R.string.right_vertical_alignment),
                            items = vertical,
                            selectedIndex = preferences.rightVerticalAlignment,
                            onSelectedIndexChange = preferences::updateRightVAlign,
                        )
                    }
                }
            }
        }

        // ── ⑤ 行为开关 ──
        item { SectionLabel(stringResource(R.string.group_transient_status_bar)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                PreferenceSwitch(
                    checked = preferences.transientCapsuleEnabled,
                    onCheckedChange = { onHaptic(); preferences.updateTransientCapsule(it) },
                    title = stringResource(R.string.transient_capsule),
                    summary = stringResource(R.string.transient_capsule_summary),
                )
                PreferenceSwitch(
                    checked = preferences.landscapeEnabled,
                    onCheckedChange = { onHaptic(); preferences.updateLandscape(it) },
                    title = stringResource(R.string.landscape_support),
                    summary = stringResource(R.string.landscape_support_summary),
                )
                PreferenceSwitch(
                    checked = preferences.portraitEnabled,
                    onCheckedChange = { onHaptic(); preferences.updatePortrait(it) },
                    title = stringResource(R.string.portrait_support),
                    summary = stringResource(R.string.portrait_support_summary),
                )
            }
        }

        item { SectionLabel(stringResource(R.string.group_status_bar_behavior)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                PreferenceSwitch(
                    checked = preferences.regularStatusBarEnabled,
                    onCheckedChange = { onHaptic(); preferences.updateRegularStatusBar(it) },
                    title = stringResource(R.string.regular_status_bar),
                    summary = stringResource(R.string.regular_status_bar_summary),
                )
                PreferenceSwitch(
                    checked = preferences.hideLandscapeIsland,
                    onCheckedChange = { onHaptic(); preferences.updateHideLandscapeIsland(it) },
                    title = stringResource(R.string.hide_landscape_island),
                    summary = stringResource(R.string.hide_landscape_island_summary),
                )
                PreferenceSwitch(
                    checked = preferences.includePrivacyIndicator,
                    onCheckedChange = { onHaptic(); preferences.updateIncludePrivacy(it) },
                    title = stringResource(R.string.include_privacy_indicator),
                    summary = stringResource(R.string.include_privacy_indicator_summary),
                    enabled = preferences.transientCapsuleEnabled || preferences.regularStatusBarEnabled,
                )
            }
        }

        item { SectionLabel(stringResource(R.string.group_visual_behavior)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                PreferenceSwitch(
                    checked = preferences.capsuleAnimationEnabled,
                    onCheckedChange = { onHaptic(); preferences.updateCapsuleAnimation(it) },
                    title = stringResource(R.string.capsule_animation),
                    summary = stringResource(R.string.capsule_animation_summary),
                )
            }
        }

        // ── ⑥ 安全模式 ──
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(
                    text = stringResource(
                        if (preferences.hookSafeMode) R.string.hook_safe_mode_enabled
                        else R.string.hook_crash_protection_enabled,
                    ),
                    modifier = Modifier.padding(16.dp),
                    style = MiuixTheme.textStyles.body2,
                )
                if (preferences.hookSafeMode) {
                    BasicComponent(
                        title = stringResource(R.string.confirm_hook_restore),
                        summary = stringResource(R.string.confirm_hook_restore_summary),
                        onClick = {
                            onHaptic()
                            preferences.confirmSafeModeRestore()
                        },
                    )
                }
            }
        }

        // ── ⑦ 重置胶囊参数 ──
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                val resetContext = LocalContext.current
                BasicComponent(
                    title = stringResource(R.string.reset_capsule),
                    summary = stringResource(R.string.reset_capsule_summary),
                    onClick = {
                        onHaptic()
                        preferences.resetCapsule()
                        Toast.makeText(
                            resetContext,
                            R.string.capsule_reset_done,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }
        }

        // ── ⑧ 提示 ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                insideMargin = PaddingValues(16.dp),
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
