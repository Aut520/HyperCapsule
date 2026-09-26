package com.aut.hypercapsule.ui.page

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aut.hypercapsule.CapsuleConfig
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.component.CapsulePreviewCard
import com.aut.hypercapsule.ui.component.RootPage
import com.aut.hypercapsule.ui.component.SectionLabel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

@Composable
fun SystemUiPage(
    preferences: AppPreferences,
    bottomContentPadding: Dp,
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

    RootPage(
        title = stringResource(R.string.system_ui),
        bottomContentPadding = bottomContentPadding,
    ) {
        // ── ⓪ 实时预览卡片 ──
        item {
            CapsulePreviewCard(
                preferences = preferences,
                onHaptic = onHaptic,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

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
                        CapsuleSlider(
                            title = "胶囊透明度",
                            valueText = "${(preferences.capsuleAlpha * 100).toInt()}%",
                            value = preferences.capsuleAlpha,
                            onValueChange = preferences::updateCapsuleAlpha,
                            valueRange = 0f..1f,
                            resetValue = 0.40f,
                            onReset = { preferences.updateCapsuleAlpha(0.40f) },
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
                        CapsuleSlider(
                            title = "高斯模糊半径",
                            valueText = "${preferences.capsuleBlurRadius.toInt()} dp",
                            value = preferences.capsuleBlurRadius,
                            onValueChange = preferences::updateCapsuleBlurRadius,
                            valueRange = 0f..80f,
                            resetValue = 20f,
                            onReset = { preferences.updateCapsuleBlurRadius(20f) },
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
                        CapsuleSlider(
                            title = "边缘高光强度",
                            valueText = "${(preferences.capsuleEdgeStrength * 100).toInt()}%",
                            value = preferences.capsuleEdgeStrength,
                            onValueChange = preferences::updateCapsuleEdgeStrength,
                            valueRange = 0f..1f,
                            resetValue = 0.55f,
                            onReset = { preferences.updateCapsuleEdgeStrength(0.55f) },
                        )
                        CapsuleSlider(
                            title = "玻璃折射强度",
                            valueText = "${(preferences.capsuleRefractionStrength * 100).toInt()}%",
                            value = preferences.capsuleRefractionStrength,
                            onValueChange = preferences::updateCapsuleRefractionStrength,
                            valueRange = 0f..1f,
                            resetValue = 0.25f,
                            onReset = { preferences.updateCapsuleRefractionStrength(0.25f) },
                        )
                        CapsuleSlider(
                            title = "光源照射方向",
                            valueText = "${preferences.capsuleLightDirection.toInt()}°",
                            value = preferences.capsuleLightDirection,
                            onValueChange = preferences::updateCapsuleLightDirection,
                            valueRange = 0f..359f,
                            resetValue = 225f,
                            onReset = { preferences.updateCapsuleLightDirection(225f) },
                        )
                    }
                }
            }
        }

        // ── ③ 尺寸与对齐 ──
        item { SectionLabel(stringResource(R.string.group_size_alignment)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                val radiusAuto = preferences.cornerRadius < 0f
                CapsuleSlider(
                    title = stringResource(R.string.capsule_corner_radius),
                    valueText = if (radiusAuto) stringResource(R.string.capsule_corner_radius_auto)
                    else stringResource(R.string.capsule_corner_radius_value, preferences.cornerRadius.toInt()),
                    value = preferences.cornerRadius,
                    onValueChange = preferences::updateCornerRadius,
                    valueRange = -1f..40f,
                    resetValue = -1f,
                    onReset = { preferences.updateCornerRadius(-1f) },
                )
                CapsuleSlider(
                    title = "胶囊总高度",
                    valueText = "${preferences.capsuleHeight.toInt()} dp",
                    value = preferences.capsuleHeight,
                    onValueChange = preferences::updateCapsuleHeight,
                    valueRange = 8f..80f,
                    resetValue = 24f,
                    onReset = { preferences.updateCapsuleHeight(24f) },
                )
                CapsuleSlider(
                    title = "水平内边距",
                    valueText = "${preferences.horizontalPadding.toInt()} dp",
                    value = preferences.horizontalPadding,
                    onValueChange = preferences::updateHorizontalPadding,
                    valueRange = 0f..64f,
                    resetValue = 8f,
                    onReset = { preferences.updateHorizontalPadding(8f) },
                )
                CapsuleSlider(
                    title = "垂直内间距",
                    valueText = "${preferences.verticalInset.toInt()} dp",
                    value = preferences.verticalInset,
                    onValueChange = preferences::updateVerticalInset,
                    valueRange = 0f..32f,
                    resetValue = 2f,
                    onReset = { preferences.updateVerticalInset(2f) },
                )
                CapsuleSlider(
                    title = "全局水平偏移",
                    valueText = "${preferences.globalOffsetX.toInt()} dp",
                    value = preferences.globalOffsetX.coerceIn(-200f, 200f),
                    onValueChange = preferences::updateGlobalOffsetX,
                    valueRange = -200f..200f,
                    steps = 0,
                    resetValue = 0f,
                    onReset = { preferences.updateGlobalOffsetX(0f) },
                )
                CapsuleSlider(
                    title = "全局垂直偏移",
                    valueText = "${preferences.globalOffsetY.toInt()} dp",
                    value = preferences.globalOffsetY.coerceIn(-200f, 200f),
                    onValueChange = preferences::updateGlobalOffsetY,
                    valueRange = -200f..200f,
                    steps = 0,
                    resetValue = 0f,
                    onReset = { preferences.updateGlobalOffsetY(0f) },
                )
            }
        }

        // ── ④ 左右独立控制 ──
        item { SectionLabel(stringResource(R.string.group_left_right_control)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
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
                        CapsuleSlider(
                            title = "左胶囊水平偏移",
                            valueText = "${preferences.leftOffsetX.toInt()} dp",
                            value = preferences.leftOffsetX.coerceIn(-200f, 200f),
                            onValueChange = preferences::updateLeftOffsetX,
                            valueRange = -200f..200f,
                            steps = 0,
                            resetValue = 0f,
                            onReset = { preferences.updateLeftOffsetX(0f) },
                        )
                        CapsuleSlider(
                            title = "左胶囊垂直偏移",
                            valueText = "${preferences.leftOffsetY.toInt()} dp",
                            value = preferences.leftOffsetY.coerceIn(-200f, 200f),
                            onValueChange = preferences::updateLeftOffsetY,
                            valueRange = -200f..200f,
                            steps = 0,
                            resetValue = 0f,
                            onReset = { preferences.updateLeftOffsetY(0f) },
                        )
                        CapsuleSlider(
                            title = "左胶囊额外水平边距",
                            valueText = "${preferences.leftPaddingX.toInt()} dp",
                            value = preferences.leftPaddingX,
                            onValueChange = preferences::updateLeftPaddingX,
                            valueRange = -32f..64f,
                            resetValue = 0f,
                            onReset = { preferences.updateLeftPaddingX(0f) },
                        )
                        CapsuleSlider(
                            title = "左胶囊额外垂直边距",
                            valueText = "${preferences.leftPaddingY.toInt()} dp",
                            value = preferences.leftPaddingY,
                            onValueChange = preferences::updateLeftPaddingY,
                            valueRange = -16f..32f,
                            resetValue = 0f,
                            onReset = { preferences.updateLeftPaddingY(0f) },
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
                        CapsuleSlider(
                            title = "右胶囊水平偏移",
                            valueText = "${preferences.rightOffsetX.toInt()} dp",
                            value = preferences.rightOffsetX.coerceIn(-200f, 200f),
                            onValueChange = preferences::updateRightOffsetX,
                            valueRange = -200f..200f,
                            steps = 0,
                            resetValue = 0f,
                            onReset = { preferences.updateRightOffsetX(0f) },
                        )
                        CapsuleSlider(
                            title = "右胶囊垂直偏移",
                            valueText = "${preferences.rightOffsetY.toInt()} dp",
                            value = preferences.rightOffsetY.coerceIn(-200f, 200f),
                            onValueChange = preferences::updateRightOffsetY,
                            valueRange = -200f..200f,
                            steps = 0,
                            resetValue = 0f,
                            onReset = { preferences.updateRightOffsetY(0f) },
                        )
                        CapsuleSlider(
                            title = "右胶囊额外水平边距",
                            valueText = "${preferences.rightPaddingX.toInt()} dp",
                            value = preferences.rightPaddingX,
                            onValueChange = preferences::updateRightPaddingX,
                            valueRange = -32f..64f,
                            resetValue = 0f,
                            onReset = { preferences.updateRightPaddingX(0f) },
                        )
                        CapsuleSlider(
                            title = "右胶囊额外垂直边距",
                            valueText = "${preferences.rightPaddingY.toInt()} dp",
                            value = preferences.rightPaddingY,
                            onValueChange = preferences::updateRightPaddingY,
                            valueRange = -16f..32f,
                            resetValue = 0f,
                            onReset = { preferences.updateRightPaddingY(0f) },
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
private fun CapsuleSlider(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    resetValue: Float? = null,
    onReset: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = valueText,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                if (resetValue != null && onReset != null && abs(value - resetValue) > 0.01f) {
                    Text(
                        text = "归零",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onReset() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
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
