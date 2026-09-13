package com.aut.hypercapsule.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var transientCapsuleEnabled by mutableStateOf(
        preferences.getBoolean(KEY_TRANSIENT_CAPSULE, DEFAULT_TRANSIENT_CAPSULE),
    )
        private set
    var landscapeEnabled by mutableStateOf(
        preferences.getBoolean(KEY_LANDSCAPE, DEFAULT_LANDSCAPE),
    )
        private set
    var portraitEnabled by mutableStateOf(
        preferences.getBoolean(KEY_PORTRAIT, DEFAULT_PORTRAIT),
    )
        private set
    var regularStatusBarEnabled by mutableStateOf(
        preferences.getBoolean(KEY_REGULAR_STATUS_BAR, DEFAULT_REGULAR_STATUS_BAR),
    )
        private set
    var hideLandscapeIsland by mutableStateOf(
        preferences.getBoolean(KEY_HIDE_LANDSCAPE_ISLAND, DEFAULT_HIDE_LANDSCAPE_ISLAND),
    )
        private set
    var includePrivacyIndicator by mutableStateOf(
        preferences.getBoolean(KEY_INCLUDE_PRIVACY, DEFAULT_INCLUDE_PRIVACY),
    )
        private set
    var capsuleAnimationEnabled by mutableStateOf(
        preferences.getBoolean(KEY_CAPSULE_ANIMATION, DEFAULT_CAPSULE_ANIMATION),
    )
        private set
    var dynamicColorEnabled by mutableStateOf(
        preferences.getBoolean(KEY_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR),
    )
        private set
    var floatingNavigationEnabled by mutableStateOf(
        preferences.getBoolean(KEY_FLOATING_NAVIGATION, DEFAULT_FLOATING_NAVIGATION),
    )
        private set
    var liquidGlassEnabled by mutableStateOf(
        preferences.getBoolean(KEY_LIQUID_GLASS, DEFAULT_LIQUID_GLASS),
    )
        private set
    var hapticFeedbackEnabled by mutableStateOf(
        preferences.getBoolean(KEY_HAPTIC_FEEDBACK, DEFAULT_HAPTIC_FEEDBACK),
    )
        private set

    val enabledFeatureCount: Int
        get() = listOf(
            transientCapsuleEnabled,
            landscapeEnabled,
            portraitEnabled,
            regularStatusBarEnabled,
            hideLandscapeIsland,
            includePrivacyIndicator,
            capsuleAnimationEnabled,
        ).count { it }

    fun updateTransientCapsule(value: Boolean) = update(KEY_TRANSIENT_CAPSULE, value) { transientCapsuleEnabled = it }
    fun updateLandscape(value: Boolean) = update(KEY_LANDSCAPE, value) { landscapeEnabled = it }
    fun updatePortrait(value: Boolean) = update(KEY_PORTRAIT, value) { portraitEnabled = it }
    fun updateRegularStatusBar(value: Boolean) = update(KEY_REGULAR_STATUS_BAR, value) { regularStatusBarEnabled = it }
    fun updateHideLandscapeIsland(value: Boolean) = update(KEY_HIDE_LANDSCAPE_ISLAND, value) { hideLandscapeIsland = it }
    fun updateIncludePrivacy(value: Boolean) = update(KEY_INCLUDE_PRIVACY, value) { includePrivacyIndicator = it }
    fun updateCapsuleAnimation(value: Boolean) = update(KEY_CAPSULE_ANIMATION, value) { capsuleAnimationEnabled = it }
    fun updateDynamicColor(value: Boolean) = update(KEY_DYNAMIC_COLOR, value) { dynamicColorEnabled = it }
    fun updateFloatingNavigation(value: Boolean) = update(KEY_FLOATING_NAVIGATION, value) { floatingNavigationEnabled = it }
    fun updateLiquidGlass(value: Boolean) = update(KEY_LIQUID_GLASS, value) { liquidGlassEnabled = it }
    fun updateHapticFeedback(value: Boolean) = update(KEY_HAPTIC_FEEDBACK, value) { hapticFeedbackEnabled = it }

    fun resetAppearance() {
        updateDynamicColor(DEFAULT_DYNAMIC_COLOR)
        updateFloatingNavigation(DEFAULT_FLOATING_NAVIGATION)
        updateLiquidGlass(DEFAULT_LIQUID_GLASS)
        updateHapticFeedback(DEFAULT_HAPTIC_FEEDBACK)
    }

    private inline fun update(key: String, value: Boolean, updateState: (Boolean) -> Unit) {
        updateState(value)
        preferences.edit { putBoolean(key, value) }
    }

    companion object {
        const val FILE_NAME = "hypercapsule_settings"

        const val KEY_TRANSIENT_CAPSULE = "transient_capsule_enabled"
        const val KEY_LANDSCAPE = "landscape_enabled"
        const val KEY_PORTRAIT = "portrait_enabled"
        const val KEY_REGULAR_STATUS_BAR = "regular_status_bar_enabled"
        const val KEY_HIDE_LANDSCAPE_ISLAND = "hide_landscape_island"
        const val KEY_INCLUDE_PRIVACY = "include_privacy_indicator"
        const val KEY_CAPSULE_ANIMATION = "capsule_animation_enabled"
        const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        const val KEY_FLOATING_NAVIGATION = "floating_navigation_enabled"
        const val KEY_LIQUID_GLASS = "liquid_glass_enabled"
        const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"

        const val DEFAULT_TRANSIENT_CAPSULE = true
        const val DEFAULT_LANDSCAPE = true
        const val DEFAULT_PORTRAIT = true
        const val DEFAULT_REGULAR_STATUS_BAR = false
        const val DEFAULT_HIDE_LANDSCAPE_ISLAND = false
        const val DEFAULT_INCLUDE_PRIVACY = true
        const val DEFAULT_CAPSULE_ANIMATION = true
        const val DEFAULT_DYNAMIC_COLOR = true
        const val DEFAULT_FLOATING_NAVIGATION = true
        const val DEFAULT_LIQUID_GLASS = false
        const val DEFAULT_HAPTIC_FEEDBACK = true
    }
}
