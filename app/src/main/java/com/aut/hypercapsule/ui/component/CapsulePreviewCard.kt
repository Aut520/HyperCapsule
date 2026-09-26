package com.aut.hypercapsule.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aut.hypercapsule.CapsuleConfig
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.AppPreferences
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 实时状态栏胶囊预览卡片：
 * 1:1 复刻 HyperOS 状态栏真机实测效果，自适应当前界面的深浅色模式，
 * 真实还原文字色彩、系统信号/电量排布与 Hook 底层着色逻辑。
 */
@Composable
fun CapsulePreviewCard(
    preferences: AppPreferences,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAppDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    // 默认跟随当前应用/系统主题的深浅模式，并支持手动快速切换
    var previewDarkMode by remember(isAppDark) { mutableStateOf(isAppDark) }

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // 顶栏：标题与模式切换按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.capsule_preview),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.headline1,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (previewDarkMode) "当前：深色环境" else "当前：浅色环境",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.footnote1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            onHaptic()
                            previewDarkMode = !previewDarkMode
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Text(
                text = stringResource(R.string.capsule_preview_summary),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )

            // 模拟 HyperOS 真实桌面壁纸环境（深浅色自适应）
            val wallpaperBrush = if (previewDarkMode) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B0F19),
                        Color(0xFF1E1B4B),
                        Color(0xFF111827),
                    ),
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0),
                        Color(0xFFBAE6FD),
                        Color(0xFFDDD6FE),
                    ),
                )
            }

            // 状态栏前景色（时间与图标颜色）
            // 在真机 HyperOS 上：深色壁纸时状态栏为白字，浅色壁纸时状态栏为深灰黑字
            val statusContentColor = if (previewDarkMode) Color(0xFFF8FAFC) else Color(0xFF1E293B)

            // 状态栏外框容器（按真机顶部 44dp 比例模拟）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(wallpaperBrush)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 模拟小米手机中置微型挖孔摄像头
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.9f)),
                )

                // 左右胶囊真实布局排布
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // ──── 左侧胶囊（时钟 + 通知图标） ────
                    val leftOffsetX = (preferences.globalOffsetX + preferences.leftOffsetX).roundToInt()
                    val leftOffsetY = (preferences.globalOffsetY + preferences.leftOffsetY).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(leftOffsetX, leftOffsetY) }
                            .renderHookCapsule(
                                preferences = preferences,
                                isLeft = true,
                                isDarkBg = previewDarkMode,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "14:30",
                                color = statusContentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp,
                            )
                            if (preferences.includeNotificationIcons) {
                                Spacer(modifier = Modifier.width(4.dp))
                                // 模拟真实 HyperOS 状态栏通知图标（微信/短信微型图标）
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(if (previewDarkMode) Color(0xFF4ADE80) else Color(0xFF16A34A)),
                                )
                            }
                        }
                    }

                    // ──── 右侧胶囊（信号 + 5G + WiFi + 电池 + 隐私点） ────
                    val rightOffsetX = (preferences.globalOffsetX + preferences.rightOffsetX).roundToInt()
                    val rightOffsetY = (preferences.globalOffsetY + preferences.rightOffsetY).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(rightOffsetX, rightOffsetY) }
                            .renderHookCapsule(
                                preferences = preferences,
                                isLeft = false,
                                isDarkBg = previewDarkMode,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            // 4 阶梯真实蜂窝信号条
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                                modifier = Modifier.height(8.dp),
                            ) {
                                Box(Modifier.width(1.5.dp).height(2.5.dp).background(statusContentColor))
                                Box(Modifier.width(1.5.dp).height(4.dp).background(statusContentColor))
                                Box(Modifier.width(1.5.dp).height(6.dp).background(statusContentColor))
                                Box(Modifier.width(1.5.dp).height(8.dp).background(statusContentColor))
                            }
                            Spacer(modifier = Modifier.width(3.dp))

                            Text(
                                text = "5G",
                                color = statusContentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(3.dp))

                            // 真实 HyperOS 电池横置药丸
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(15.dp)
                                        .height(7.5.dp)
                                        .border(
                                            width = 0.8.dp,
                                            color = statusContentColor.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(1.5.dp),
                                        )
                                        .padding(1.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(5.5.dp)
                                            .background(
                                                color = if (previewDarkMode) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                                shape = RoundedCornerShape(0.8.dp),
                                            ),
                                    )
                                }
                            }

                            // 隐私指示器（绿点）
                            if (preferences.includePrivacyIndicator) {
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)),
                                )
                            }
                        }
                    }
                }
            }

            // ──── 快捷风格预设 ────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetChip(
                    label = "经典透明",
                    onClick = {
                        onHaptic()
                        preferences.updateCapsuleMaterial(CapsuleConfig.TRANSLUCENT)
                        preferences.updateCapsuleAlpha(0.40f)
                        preferences.updateCapsuleHeight(24f)
                        preferences.updateHorizontalPadding(8f)
                        preferences.updateVerticalInset(2f)
                        preferences.updateCornerRadius(-1f)
                    },
                    modifier = Modifier.weight(1f),
                )
                PresetChip(
                    label = "高斯模糊",
                    onClick = {
                        onHaptic()
                        preferences.updateCapsuleMaterial(CapsuleConfig.GAUSSIAN_BLUR)
                        preferences.updateCapsuleAlpha(0.35f)
                        preferences.updateCapsuleBlurRadius(20f)
                        preferences.updateCapsuleHeight(24f)
                        preferences.updateHorizontalPadding(8f)
                        preferences.updateCornerRadius(-1f)
                    },
                    modifier = Modifier.weight(1f),
                )
                PresetChip(
                    label = "柔光玻璃",
                    onClick = {
                        onHaptic()
                        preferences.updateCapsuleMaterial(CapsuleConfig.SOFT_GLASS)
                        preferences.updateCapsuleAlpha(0.45f)
                        preferences.updateCapsuleBlurRadius(20f)
                        preferences.updateCapsuleEdgeStrength(0.55f)
                        preferences.updateCapsuleRefractionStrength(0.25f)
                        preferences.updateCapsuleHeight(25f)
                        preferences.updateHorizontalPadding(9f)
                        preferences.updateCornerRadius(-1f)
                    },
                    modifier = Modifier.weight(1f),
                )
                PresetChip(
                    label = "液态玻璃",
                    onClick = {
                        onHaptic()
                        preferences.updateCapsuleMaterial(CapsuleConfig.LIQUID_GLASS)
                        preferences.updateCapsuleAlpha(0.50f)
                        preferences.updateCapsuleBlurRadius(24f)
                        preferences.updateCapsuleEdgeStrength(0.70f)
                        preferences.updateCapsuleRefractionStrength(0.40f)
                        preferences.updateCapsuleHeight(26f)
                        preferences.updateHorizontalPadding(10f)
                        preferences.updateCornerRadius(-1f)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 严格按照 HyperCapsuleModule.java 源码绘制底色与高光：
 * 1. TRANSLUCENT: 纯透明底色块，无边框！
 * 2. GAUSSIAN_BLUR: 模拟磨砂透出，无边框！
 * 3. SOFT_GLASS & LIQUID_GLASS: 线性渐变 + 高光轮廓线（受 edgeStrength 控制）！
 * 4. ORIGINAL: 不绘制胶囊，保持系统纯字！
 */
private fun Modifier.renderHookCapsule(
    preferences: AppPreferences,
    isLeft: Boolean,
    isDarkBg: Boolean,
): Modifier {
    val enabled = if (isLeft) preferences.leftCapsuleEnabled else preferences.rightCapsuleEnabled
    if (!enabled || preferences.capsuleMaterial == CapsuleConfig.ORIGINAL) {
        return this.padding(horizontal = 4.dp, vertical = 2.dp)
    }

    val height = preferences.capsuleHeight.coerceIn(8f, 50f).dp
    val hPad = (preferences.horizontalPadding + if (isLeft) preferences.leftPaddingX else preferences.rightPaddingX).coerceAtLeast(0f).dp
    val vPad = (preferences.verticalInset + if (isLeft) preferences.leftPaddingY else preferences.rightPaddingY).coerceAtLeast(0f).dp
    val radius = if (preferences.cornerRadius >= 0f) {
        preferences.cornerRadius.dp
    } else {
        height / 2f
    }
    val shape = RoundedCornerShape(radius)

    // 与 HyperCapsuleModule.configurePaints 的光线与底色判定严格一致：
    // light = TONE_LIGHT || (TONE_AUTO && !nightMode)
    val isLightTone = preferences.capsuleTone == CapsuleConfig.TONE_LIGHT ||
        (preferences.capsuleTone == CapsuleConfig.TONE_AUTO && !isDarkBg)

    val baseAlpha = preferences.capsuleAlpha
    val baseChannel = if (isLightTone) 255 else 0

    val brush = when (preferences.capsuleMaterial) {
        CapsuleConfig.SOFT_GLASS -> {
            val rad = Math.toRadians(preferences.capsuleLightDirection.toDouble())
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = baseAlpha * 0.80f),
                    if (isLightTone) Color(170, 190, 220, (baseAlpha * 255 * (0.25f + preferences.capsuleRefractionStrength * 0.35f)).toInt())
                    else Color(20, 30, 45, (baseAlpha * 255 * (0.25f + preferences.capsuleRefractionStrength * 0.35f)).toInt()),
                ),
            )
        }
        CapsuleConfig.LIQUID_GLASS -> {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = baseAlpha * 0.88f),
                    if (isLightTone) Color(115, 165, 235, (baseAlpha * 255 * (0.20f + preferences.capsuleRefractionStrength * 0.50f)).toInt())
                    else Color(10, 24, 55, (baseAlpha * 255 * (0.20f + preferences.capsuleRefractionStrength * 0.50f)).toInt()),
                ),
            )
        }
        CapsuleConfig.GAUSSIAN_BLUR -> {
            // 高斯模糊在真机上由 NativeBlur 采样底层，在预览中呈现半透柔和磨砂质感
            SolidColor(Color(baseChannel, baseChannel, baseChannel).copy(alpha = (baseAlpha * 0.75f).coerceIn(0.15f, 0.9f)))
        }
        else -> {
            // TRANSLUCENT: 纯净半透明底色，与真机完全一致
            SolidColor(Color(baseChannel, baseChannel, baseChannel).copy(alpha = baseAlpha))
        }
    }

    // 只有 SOFT_GLASS 和 LIQUID_GLASS 材质在 Hook 中有 outline 边框！
    val outlineAlpha = when (preferences.capsuleMaterial) {
        CapsuleConfig.SOFT_GLASS -> baseAlpha * 0.60f
        CapsuleConfig.LIQUID_GLASS -> baseAlpha * 0.78f
        else -> 0f
    }

    return this
        .height(height)
        .clip(shape)
        .background(brush)
        .then(
            if (outlineAlpha > 0.05f) {
                Modifier.border(
                    width = (0.5f + preferences.capsuleEdgeStrength * 1.5f).dp,
                    color = Color.White.copy(alpha = (outlineAlpha * preferences.capsuleEdgeStrength).coerceIn(0f, 1f)),
                    shape = shape,
                )
            } else Modifier,
        )
        .padding(horizontal = hPad, vertical = vPad)
}
