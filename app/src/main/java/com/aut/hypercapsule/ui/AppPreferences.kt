package com.aut.hypercapsule.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.aut.hypercapsule.CapsuleConfig
import com.aut.hypercapsule.RemotePreferencesBridge
import com.aut.hypercapsule.SafeModeProvider

class AppPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var transientCapsuleEnabled by mutableStateOf(boolean(CapsuleConfig.TRANSIENT, true))
        private set
    var landscapeEnabled by mutableStateOf(boolean(CapsuleConfig.LANDSCAPE, true))
        private set
    var portraitEnabled by mutableStateOf(boolean(CapsuleConfig.PORTRAIT, true))
        private set
    var regularStatusBarEnabled by mutableStateOf(boolean(CapsuleConfig.REGULAR, false))
        private set
    var hideLandscapeIsland by mutableStateOf(boolean(CapsuleConfig.HIDE_ISLAND, false))
        private set
    var includePrivacyIndicator by mutableStateOf(boolean(CapsuleConfig.INCLUDE_PRIVACY, true))
        private set
    var capsuleAnimationEnabled by mutableStateOf(boolean(CapsuleConfig.ANIMATION, true))
        private set

    var capsuleMaterial by mutableStateOf(
        preferences.getString(CapsuleConfig.MATERIAL, CapsuleConfig.TRANSLUCENT)
            .takeIf { it in MATERIALS } ?: CapsuleConfig.TRANSLUCENT,
    )
        private set
    var capsuleTone by mutableStateOf(
        preferences.getString(CapsuleConfig.TONE, CapsuleConfig.TONE_AUTO)
            .takeIf { it in TONES } ?: CapsuleConfig.TONE_AUTO,
    )
        private set
    var capsuleAlpha by mutableStateOf(float(CapsuleConfig.ALPHA, .40f, 0f, 1f))
        private set
    var capsuleBlurRadius by mutableStateOf(float(CapsuleConfig.BLUR_RADIUS, 20f, 0f, 80f))
        private set
    var capsuleEdgeStrength by mutableStateOf(float(CapsuleConfig.EDGE_STRENGTH, .55f, 0f, 1f))
        private set
    var capsuleRefractionStrength by mutableStateOf(
        float(CapsuleConfig.REFRACTION_STRENGTH, .25f, 0f, 1f),
    )
        private set
    var capsuleLightDirection by mutableStateOf(float(CapsuleConfig.LIGHT_DIRECTION, 225f, 0f, 359f))
        private set
    var horizontalPadding by mutableStateOf(float(CapsuleConfig.HORIZONTAL_PADDING, 8f, 0f, 64f))
        private set
    var verticalInset by mutableStateOf(float(CapsuleConfig.VERTICAL_INSET, 2f, 0f, 32f))
        private set
    var capsuleHeight by mutableStateOf(float(CapsuleConfig.HEIGHT, 24f, 8f, 80f))
        private set
    var globalOffsetX by mutableStateOf(float(CapsuleConfig.GLOBAL_X, 0f, -200f, 200f))
        private set
    var globalOffsetY by mutableStateOf(float(CapsuleConfig.GLOBAL_Y, 0f, -200f, 200f))
        private set

    var leftCapsuleEnabled by mutableStateOf(boolean(CapsuleConfig.LEFT_ENABLED, true))
        private set
    var rightCapsuleEnabled by mutableStateOf(boolean(CapsuleConfig.RIGHT_ENABLED, true))
        private set
    var leftOffsetX by mutableStateOf(float(CapsuleConfig.LEFT_X, 0f, -200f, 200f))
        private set
    var leftOffsetY by mutableStateOf(float(CapsuleConfig.LEFT_Y, 0f, -200f, 200f))
        private set
    var rightOffsetX by mutableStateOf(float(CapsuleConfig.RIGHT_X, 0f, -200f, 200f))
        private set
    var rightOffsetY by mutableStateOf(float(CapsuleConfig.RIGHT_Y, 0f, -200f, 200f))
        private set
    var leftPaddingX by mutableStateOf(float(CapsuleConfig.LEFT_PADDING_X, 0f, -32f, 64f))
        private set
    var leftPaddingY by mutableStateOf(float(CapsuleConfig.LEFT_PADDING_Y, 0f, -16f, 32f))
        private set
    var rightPaddingX by mutableStateOf(float(CapsuleConfig.RIGHT_PADDING_X, 0f, -32f, 64f))
        private set
    var rightPaddingY by mutableStateOf(float(CapsuleConfig.RIGHT_PADDING_Y, 0f, -16f, 32f))
        private set
    var leftHorizontalAlignment by mutableStateOf(int(CapsuleConfig.LEFT_H, 0, 0, 2))
        private set
    var leftVerticalAlignment by mutableStateOf(int(CapsuleConfig.LEFT_V, 1, 0, 2))
        private set
    var rightHorizontalAlignment by mutableStateOf(int(CapsuleConfig.RIGHT_H, 2, 0, 2))
        private set
    var rightVerticalAlignment by mutableStateOf(int(CapsuleConfig.RIGHT_V, 1, 0, 2))
        private set
    var cornerRadius by mutableStateOf(float(CapsuleConfig.CORNER_RADIUS, -1f, -1f, 40f))
        private set
    var includeNotificationIcons by mutableStateOf(boolean(CapsuleConfig.INCLUDE_NOTIFICATION, true))
        private set

    var dynamicColorEnabled by mutableStateOf(boolean(KEY_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR))
        private set
    var themeColorMode by mutableStateOf(int(KEY_THEME_COLOR_MODE, DEFAULT_THEME_COLOR_MODE, 0, 2))
        private set
    var themePalette by mutableStateOf(
        preferences.getString(KEY_THEME_PALETTE, DEFAULT_THEME_PALETTE)
            .takeIf { it in PALETTES } ?: DEFAULT_THEME_PALETTE,
    )
        private set
    var floatingNavigationEnabled by mutableStateOf(
        boolean(KEY_FLOATING_NAVIGATION, DEFAULT_FLOATING_NAVIGATION),
    )
        private set
    var liquidGlassEnabled by mutableStateOf(boolean(KEY_LIQUID_GLASS, DEFAULT_LIQUID_GLASS))
        private set
    var hapticFeedbackEnabled by mutableStateOf(boolean(KEY_HAPTIC_FEEDBACK, DEFAULT_HAPTIC_FEEDBACK))
        private set

    var hookSafeMode by mutableStateOf(boolean(CapsuleConfig.SAFE_MODE, false))
        private set
    var hookCrashCount by mutableStateOf(int(CapsuleConfig.CRASH_COUNT, 0, 0, 3))
        private set
    var hookRestorePending by mutableStateOf(boolean(CapsuleConfig.RESTORE_PENDING, false))
        private set
    var hookLastCrashTime by mutableStateOf(
        runCatching { preferences.getLong(CapsuleConfig.LAST_CRASH, 0L) }.getOrDefault(0L),
    )
        private set

    init {
        refreshSafetyState()
    }

    val enabledFeatureCount: Int
        get() = listOf(
            transientCapsuleEnabled,
            landscapeEnabled,
            portraitEnabled,
            regularStatusBarEnabled,
            hideLandscapeIsland,
            includePrivacyIndicator,
            capsuleAnimationEnabled,
            leftCapsuleEnabled,
            rightCapsuleEnabled,
        ).count { it }

    fun updateTransientCapsule(value: Boolean) = updateBoolean(CapsuleConfig.TRANSIENT, value) {
        transientCapsuleEnabled = it
    }

    fun updateLandscape(value: Boolean) = updateBoolean(CapsuleConfig.LANDSCAPE, value) {
        landscapeEnabled = it
    }

    fun updatePortrait(value: Boolean) = updateBoolean(CapsuleConfig.PORTRAIT, value) {
        portraitEnabled = it
    }

    fun updateRegularStatusBar(value: Boolean) = updateBoolean(CapsuleConfig.REGULAR, value) {
        regularStatusBarEnabled = it
    }

    fun updateHideLandscapeIsland(value: Boolean) = updateBoolean(CapsuleConfig.HIDE_ISLAND, value) {
        hideLandscapeIsland = it
    }

    fun updateIncludePrivacy(value: Boolean) = updateBoolean(CapsuleConfig.INCLUDE_PRIVACY, value) {
        includePrivacyIndicator = it
    }

    fun updateCapsuleAnimation(value: Boolean) = updateBoolean(CapsuleConfig.ANIMATION, value) {
        capsuleAnimationEnabled = it
    }

    fun updateCapsuleMaterial(value: String) {
        val normalized = value.takeIf { it in MATERIALS } ?: CapsuleConfig.TRANSLUCENT
        capsuleMaterial = normalized
        updateString(CapsuleConfig.MATERIAL, normalized)
    }

    fun updateCapsuleTone(value: String) {
        val normalized = value.takeIf { it in TONES } ?: CapsuleConfig.TONE_AUTO
        capsuleTone = normalized
        updateString(CapsuleConfig.TONE, normalized)
    }

    fun updateCapsuleAlpha(value: Float) = updateFloat(CapsuleConfig.ALPHA, value, 0f, 1f) {
        capsuleAlpha = it
    }

    fun updateCapsuleBlurRadius(value: Float) = updateFloat(
        CapsuleConfig.BLUR_RADIUS,
        value,
        0f,
        80f,
    ) { capsuleBlurRadius = it }

    fun updateCapsuleEdgeStrength(value: Float) = updateFloat(
        CapsuleConfig.EDGE_STRENGTH,
        value,
        0f,
        1f,
    ) { capsuleEdgeStrength = it }

    fun updateCapsuleRefractionStrength(value: Float) = updateFloat(
        CapsuleConfig.REFRACTION_STRENGTH,
        value,
        0f,
        1f,
    ) { capsuleRefractionStrength = it }

    fun updateCapsuleLightDirection(value: Float) = updateFloat(
        CapsuleConfig.LIGHT_DIRECTION,
        value,
        0f,
        359f,
    ) { capsuleLightDirection = it }

    fun updateHorizontalPadding(value: Float) = updateFloat(
        CapsuleConfig.HORIZONTAL_PADDING,
        value,
        0f,
        64f,
    ) { horizontalPadding = it }

    fun updateVerticalInset(value: Float) = updateFloat(
        CapsuleConfig.VERTICAL_INSET,
        value,
        0f,
        32f,
    ) { verticalInset = it }

    fun updateCapsuleHeight(value: Float) = updateFloat(CapsuleConfig.HEIGHT, value, 8f, 80f) {
        capsuleHeight = it
    }

    fun updateGlobalOffsetX(value: Float) = updateFloat(CapsuleConfig.GLOBAL_X, value, -200f, 200f) {
        globalOffsetX = it
    }

    fun updateGlobalOffsetY(value: Float) = updateFloat(CapsuleConfig.GLOBAL_Y, value, -200f, 200f) {
        globalOffsetY = it
    }

    fun updateLeftCapsuleEnabled(value: Boolean) = updateBoolean(CapsuleConfig.LEFT_ENABLED, value) {
        leftCapsuleEnabled = it
    }

    fun updateRightCapsuleEnabled(value: Boolean) = updateBoolean(CapsuleConfig.RIGHT_ENABLED, value) {
        rightCapsuleEnabled = it
    }

    fun updateLeftOffsetX(value: Float) = updateFloat(CapsuleConfig.LEFT_X, value, -200f, 200f) {
        leftOffsetX = it
    }

    fun updateLeftOffsetY(value: Float) = updateFloat(CapsuleConfig.LEFT_Y, value, -200f, 200f) {
        leftOffsetY = it
    }

    fun updateRightOffsetX(value: Float) = updateFloat(CapsuleConfig.RIGHT_X, value, -200f, 200f) {
        rightOffsetX = it
    }

    fun updateRightOffsetY(value: Float) = updateFloat(CapsuleConfig.RIGHT_Y, value, -200f, 200f) {
        rightOffsetY = it
    }

    fun updateLeftPaddingX(value: Float) = updateFloat(
        CapsuleConfig.LEFT_PADDING_X,
        value,
        -32f,
        64f,
    ) { leftPaddingX = it }

    fun updateLeftPaddingY(value: Float) = updateFloat(
        CapsuleConfig.LEFT_PADDING_Y,
        value,
        -16f,
        32f,
    ) { leftPaddingY = it }

    fun updateRightPaddingX(value: Float) = updateFloat(
        CapsuleConfig.RIGHT_PADDING_X,
        value,
        -32f,
        64f,
    ) { rightPaddingX = it }

    fun updateRightPaddingY(value: Float) = updateFloat(
        CapsuleConfig.RIGHT_PADDING_Y,
        value,
        -16f,
        32f,
    ) { rightPaddingY = it }

    fun updateLeftHAlign(value: Int) = updateInt(CapsuleConfig.LEFT_H, value, 0, 2) {
        leftHorizontalAlignment = it
    }

    fun updateLeftVAlign(value: Int) = updateInt(CapsuleConfig.LEFT_V, value, 0, 2) {
        leftVerticalAlignment = it
    }

    fun updateRightHAlign(value: Int) = updateInt(CapsuleConfig.RIGHT_H, value, 0, 2) {
        rightHorizontalAlignment = it
    }

    fun updateRightVAlign(value: Int) = updateInt(CapsuleConfig.RIGHT_V, value, 0, 2) {
        rightVerticalAlignment = it
    }

    fun updateCornerRadius(value: Float) = updateFloat(CapsuleConfig.CORNER_RADIUS, value, -1f, 40f) {
        cornerRadius = it
    }

    fun updateIncludeNotification(value: Boolean) = updateBoolean(CapsuleConfig.INCLUDE_NOTIFICATION, value) {
        includeNotificationIcons = it
    }

    fun updateDynamicColor(value: Boolean) = updateBoolean(KEY_DYNAMIC_COLOR, value) {
        dynamicColorEnabled = it
    }

    fun updateThemeColorMode(value: Int) = updateInt(KEY_THEME_COLOR_MODE, value, 0, 2) {
        themeColorMode = it
    }

    fun updateThemePalette(value: String) {
        val normalized = value.takeIf { it in PALETTES } ?: DEFAULT_THEME_PALETTE
        themePalette = normalized
        updateString(KEY_THEME_PALETTE, normalized)
    }

    fun updateFloatingNavigation(value: Boolean) = updateBoolean(KEY_FLOATING_NAVIGATION, value) {
        floatingNavigationEnabled = it
    }

    fun updateLiquidGlass(value: Boolean) = updateBoolean(KEY_LIQUID_GLASS, value) {
        liquidGlassEnabled = it
    }

    fun updateHapticFeedback(value: Boolean) = updateBoolean(KEY_HAPTIC_FEEDBACK, value) {
        hapticFeedbackEnabled = it
    }

    fun refreshSafetyState() {
        val state = runCatching { SafeModeProvider.read(appContext) }.getOrNull() ?: return
        hookSafeMode = state.getBoolean(SafeModeProvider.KEY_SAFE_MODE, hookSafeMode)
        hookCrashCount = state.getInt(SafeModeProvider.KEY_CRASH_COUNT, hookCrashCount).coerceIn(0, 3)
        hookRestorePending = state.getBoolean(
            SafeModeProvider.KEY_RESTORE_PENDING,
            hookRestorePending,
        )
        hookLastCrashTime = state.getLong(
            SafeModeProvider.KEY_LAST_CRASH_TIME,
            hookLastCrashTime,
        )
    }

    fun confirmSafeModeRestore() {
        val state = runCatching { SafeModeProvider.confirm(appContext) }.getOrNull()
        hookSafeMode = state?.getBoolean(SafeModeProvider.KEY_SAFE_MODE, false) ?: false
        hookCrashCount = state?.getInt(SafeModeProvider.KEY_CRASH_COUNT, 0) ?: 0
        hookRestorePending = state?.getBoolean(SafeModeProvider.KEY_RESTORE_PENDING, false) ?: false
        hookLastCrashTime = state?.getLong(SafeModeProvider.KEY_LAST_CRASH_TIME, 0L) ?: 0L
        updateBoolean(CapsuleConfig.SAFE_MODE, hookSafeMode) { }
        updateInt(CapsuleConfig.CRASH_COUNT, hookCrashCount, 0, 3) { }
        updateBoolean(CapsuleConfig.RESTORE_PENDING, hookRestorePending) { }
        updateLong(CapsuleConfig.LAST_CRASH, hookLastCrashTime)
    }

    fun resetCapsule() {
        updateCapsuleMaterial(CapsuleConfig.TRANSLUCENT)
        updateCapsuleTone(CapsuleConfig.TONE_AUTO)
        updateCapsuleAlpha(.40f)
        updateCapsuleBlurRadius(20f)
        updateCapsuleEdgeStrength(.55f)
        updateCapsuleRefractionStrength(.25f)
        updateCapsuleLightDirection(225f)
        updateHorizontalPadding(8f)
        updateVerticalInset(2f)
        updateCapsuleHeight(24f)
        updateGlobalOffsetX(0f)
        updateGlobalOffsetY(0f)
        updateLeftOffsetX(0f)
        updateLeftOffsetY(0f)
        updateRightOffsetX(0f)
        updateRightOffsetY(0f)
        updateLeftPaddingX(0f)
        updateLeftPaddingY(0f)
        updateRightPaddingX(0f)
        updateRightPaddingY(0f)
        updateLeftHAlign(0)
        updateLeftVAlign(1)
        updateRightHAlign(2)
        updateRightVAlign(1)
        updateLeftCapsuleEnabled(true)
        updateRightCapsuleEnabled(true)
        updateCornerRadius(-1f)
        updateIncludeNotification(true)
    }

    fun resetAppearance() {
        updateDynamicColor(DEFAULT_DYNAMIC_COLOR)
        updateThemeColorMode(DEFAULT_THEME_COLOR_MODE)
        updateThemePalette(DEFAULT_THEME_PALETTE)
        updateFloatingNavigation(DEFAULT_FLOATING_NAVIGATION)
        updateLiquidGlass(DEFAULT_LIQUID_GLASS)
        updateHapticFeedback(DEFAULT_HAPTIC_FEEDBACK)
    }

    private fun boolean(key: String, default: Boolean): Boolean =
        runCatching { preferences.getBoolean(key, default) }.getOrDefault(default)

    private fun float(key: String, default: Float, min: Float, max: Float): Float =
        runCatching { preferences.getFloat(key, default) }.getOrDefault(default).coerceIn(min, max)

    private fun int(key: String, default: Int, min: Int, max: Int): Int =
        runCatching { preferences.getInt(key, default) }.getOrDefault(default).coerceIn(min, max)

    private inline fun updateBoolean(
        key: String,
        value: Boolean,
        updateState: (Boolean) -> Unit,
    ) {
        updateState(value)
        preferences.edit { putBoolean(key, value) }
        RemotePreferencesBridge.putBoolean(key, value)
    }

    private inline fun updateFloat(
        key: String,
        value: Float,
        min: Float,
        max: Float,
        updateState: (Float) -> Unit,
    ) {
        val normalized = value.coerceIn(min, max)
        updateState(normalized)
        preferences.edit { putFloat(key, normalized) }
        RemotePreferencesBridge.putFloat(key, normalized)
    }

    private inline fun updateInt(
        key: String,
        value: Int,
        min: Int,
        max: Int,
        updateState: (Int) -> Unit,
    ) {
        val normalized = value.coerceIn(min, max)
        updateState(normalized)
        preferences.edit { putInt(key, normalized) }
        RemotePreferencesBridge.putInt(key, normalized)
    }

    private fun updateString(key: String, value: String) {
        preferences.edit { putString(key, value) }
        RemotePreferencesBridge.putString(key, value)
    }

    private fun updateLong(key: String, value: Long) {
        preferences.edit { putLong(key, value) }
        RemotePreferencesBridge.putLong(key, value)
    }

    companion object {
        const val FILE_NAME = CapsuleConfig.PREFS
        const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        const val KEY_THEME_COLOR_MODE = "theme_color_mode"
        const val KEY_THEME_PALETTE = "theme_palette"
        const val KEY_FLOATING_NAVIGATION = "floating_navigation_enabled"
        const val KEY_LIQUID_GLASS = "liquid_glass_enabled"
        const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"
        const val DEFAULT_DYNAMIC_COLOR = true
        const val DEFAULT_THEME_COLOR_MODE = 0
        const val DEFAULT_THEME_PALETTE = "Expressive"
        const val DEFAULT_FLOATING_NAVIGATION = true
        const val DEFAULT_LIQUID_GLASS = false
        const val DEFAULT_HAPTIC_FEEDBACK = true
        val PALETTES = listOf("TonalSpot", "Vibrant", "Expressive", "Neutral")
        val MATERIALS = listOf(
            CapsuleConfig.ORIGINAL,
            CapsuleConfig.TRANSLUCENT,
            CapsuleConfig.GAUSSIAN_BLUR,
            CapsuleConfig.SOFT_GLASS,
            CapsuleConfig.LIQUID_GLASS,
        )
        val TONES = listOf(
            CapsuleConfig.TONE_AUTO,
            CapsuleConfig.TONE_DARK,
            CapsuleConfig.TONE_LIGHT,
        )
    }
}
