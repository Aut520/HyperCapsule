package com.aut.hypercapsule;

import android.content.SharedPreferences;
import android.content.res.Configuration;

/** Shared, clamped configuration contract between the manager app and SystemUI. */
public final class CapsuleConfig {
    public static final String PREFS = "hypercapsule_settings";

    public static final String TRANSIENT = "transient_capsule_enabled";
    public static final String LANDSCAPE = "landscape_enabled";
    public static final String PORTRAIT = "portrait_enabled";
    public static final String REGULAR = "regular_status_bar_enabled";
    public static final String HIDE_ISLAND = "hide_landscape_island";
    public static final String INCLUDE_PRIVACY = "include_privacy_indicator";
    public static final String ANIMATION = "capsule_animation_enabled";
    public static final String MATERIAL = "capsule_material";
    public static final String TONE = "capsule_tone";
    public static final String ALPHA = "capsule_alpha";
    public static final String BLUR_RADIUS = "capsule_blur_radius_dp";
    public static final String EDGE_STRENGTH = "capsule_edge_strength";
    public static final String REFRACTION_STRENGTH = "capsule_refraction_strength";
    public static final String LIGHT_DIRECTION = "capsule_light_direction_degrees";
    public static final String HORIZONTAL_PADDING = "horizontal_padding_dp";
    public static final String VERTICAL_INSET = "vertical_inset_dp";
    public static final String HEIGHT = "capsule_height_dp";
    public static final String GLOBAL_X = "global_offset_x_dp";
    public static final String GLOBAL_Y = "global_offset_y_dp";
    public static final String LEFT_X = "left_offset_x_dp";
    public static final String LEFT_Y = "left_offset_y_dp";
    public static final String RIGHT_X = "right_offset_x_dp";
    public static final String RIGHT_Y = "right_offset_y_dp";
    public static final String LEFT_ENABLED = "left_capsule_enabled";
    public static final String RIGHT_ENABLED = "right_capsule_enabled";
    public static final String LEFT_PADDING_X = "left_padding_x_dp";
    public static final String LEFT_PADDING_Y = "left_padding_y_dp";
    public static final String RIGHT_PADDING_X = "right_padding_x_dp";
    public static final String RIGHT_PADDING_Y = "right_padding_y_dp";
    public static final String LEFT_H = "left_horizontal_alignment";
    public static final String LEFT_V = "left_vertical_alignment";
    public static final String RIGHT_H = "right_horizontal_alignment";
    public static final String RIGHT_V = "right_vertical_alignment";
    public static final String CORNER_RADIUS = "capsule_corner_radius_dp";
    public static final String INCLUDE_NOTIFICATION = "include_notification_icons";
    public static final String SAFE_MODE = "hook_safe_mode";
    public static final String CRASH_COUNT = "hook_crash_count";
    public static final String LAST_CRASH = "hook_last_crash_time";
    public static final String RESTORE_PENDING = "hook_restore_pending";

    public static final String ORIGINAL = "ORIGINAL";
    public static final String TRANSLUCENT = "TRANSLUCENT";
    public static final String GAUSSIAN_BLUR = "GAUSSIAN_BLUR";
    public static final String SOFT_GLASS = "SOFT_GLASS";
    public static final String LIQUID_GLASS = "LIQUID_GLASS";
    public static final String TONE_AUTO = "AUTO";
    public static final String TONE_DARK = "DARK";
    public static final String TONE_LIGHT = "LIGHT";

    private CapsuleConfig() { }

    public static Values read(SharedPreferences p) {
        String material = p.getString(MATERIAL, TRANSLUCENT);
        if (!ORIGINAL.equals(material) && !TRANSLUCENT.equals(material)
                && !GAUSSIAN_BLUR.equals(material) && !SOFT_GLASS.equals(material)
                && !LIQUID_GLASS.equals(material)) material = TRANSLUCENT;
        String tone = p.getString(TONE, TONE_AUTO);
        if (!TONE_AUTO.equals(tone) && !TONE_DARK.equals(tone)
                && !TONE_LIGHT.equals(tone)) tone = TONE_AUTO;
        return new Values(
                p.getBoolean(TRANSIENT, true),
                p.getBoolean(LANDSCAPE, true),
                p.getBoolean(PORTRAIT, true),
                p.getBoolean(REGULAR, false),
                p.getBoolean(HIDE_ISLAND, false),
                p.getBoolean(INCLUDE_PRIVACY, true),
                p.getBoolean(ANIMATION, true),
                material,
                tone,
                clamp(p.getFloat(ALPHA, .40f), 0f, 1f),
                clamp(p.getFloat(BLUR_RADIUS, 20f), 0f, 80f),
                clamp(p.getFloat(EDGE_STRENGTH, .55f), 0f, 1f),
                clamp(p.getFloat(REFRACTION_STRENGTH, .25f), 0f, 1f),
                clamp(p.getFloat(LIGHT_DIRECTION, 225f), 0f, 359f),
                clamp(p.getFloat(HORIZONTAL_PADDING, 8f), 0f, 64f),
                clamp(p.getFloat(VERTICAL_INSET, 2f), 0f, 32f),
                clamp(p.getFloat(HEIGHT, 24f), 8f, 80f),
                clamp(p.getFloat(GLOBAL_X, 0f), -200f, 200f),
                clamp(p.getFloat(GLOBAL_Y, 0f), -200f, 200f),
                clamp(p.getFloat(LEFT_X, 0f), -200f, 200f),
                clamp(p.getFloat(LEFT_Y, 0f), -200f, 200f),
                clamp(p.getFloat(RIGHT_X, 0f), -200f, 200f),
                clamp(p.getFloat(RIGHT_Y, 0f), -200f, 200f),
                p.getBoolean(LEFT_ENABLED, true), p.getBoolean(RIGHT_ENABLED, true),
                clamp(p.getFloat(LEFT_PADDING_X, 0f), -32f, 64f),
                clamp(p.getFloat(LEFT_PADDING_Y, 0f), -16f, 32f),
                clamp(p.getFloat(RIGHT_PADDING_X, 0f), -32f, 64f),
                clamp(p.getFloat(RIGHT_PADDING_Y, 0f), -16f, 32f),
                clamp(p.getInt(LEFT_H, 0), 0, 2), clamp(p.getInt(LEFT_V, 1), 0, 2),
                clamp(p.getInt(RIGHT_H, 2), 0, 2), clamp(p.getInt(RIGHT_V, 1), 0, 2),
                clamp(p.getFloat(CORNER_RADIUS, -1f), -1f, 40f),
                p.getBoolean(INCLUDE_NOTIFICATION, true),
                p.getBoolean(SAFE_MODE, false), p.getInt(CRASH_COUNT, 0),
                p.getLong(LAST_CRASH, 0L), p.getBoolean(RESTORE_PENDING, false));
    }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    public static boolean isLandscape(Configuration configuration) {
        return configuration != null && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static final class Values {
        public final boolean transientEnabled, landscapeEnabled, portraitEnabled, regularEnabled, hideIsland, includePrivacy, animationEnabled, leftEnabled, rightEnabled, includeNotification;
        public final String material, tone;
        public final float alpha, blurRadius, edgeStrength, refractionStrength, lightDirection, horizontalPadding, verticalInset, height, globalX, globalY, leftX, leftY, rightX, rightY, leftPaddingX, leftPaddingY, rightPaddingX, rightPaddingY, cornerRadius;
        public final int leftH, leftV, rightH, rightV, crashCount;
        public final boolean safeMode, restorePending;
        public final long lastCrash;

        private Values(boolean transientEnabled, boolean landscapeEnabled, boolean portraitEnabled, boolean regularEnabled, boolean hideIsland, boolean includePrivacy, boolean animationEnabled, String material, String tone, float alpha, float blurRadius, float edgeStrength, float refractionStrength, float lightDirection, float horizontalPadding, float verticalInset, float height, float globalX, float globalY, float leftX, float leftY, float rightX, float rightY, boolean leftEnabled, boolean rightEnabled, float leftPaddingX, float leftPaddingY, float rightPaddingX, float rightPaddingY, int leftH, int leftV, int rightH, int rightV, float cornerRadius, boolean includeNotification, boolean safeMode, int crashCount, long lastCrash, boolean restorePending) {
            this.transientEnabled = transientEnabled; this.landscapeEnabled = landscapeEnabled; this.portraitEnabled = portraitEnabled; this.regularEnabled = regularEnabled; this.hideIsland = hideIsland; this.includePrivacy = includePrivacy; this.animationEnabled = animationEnabled; this.material = material; this.tone = tone; this.alpha = alpha; this.blurRadius = blurRadius; this.edgeStrength = edgeStrength; this.refractionStrength = refractionStrength; this.lightDirection = lightDirection; this.horizontalPadding = horizontalPadding; this.verticalInset = verticalInset; this.height = height; this.globalX = globalX; this.globalY = globalY; this.leftX = leftX; this.leftY = leftY; this.rightX = rightX; this.rightY = rightY; this.leftEnabled = leftEnabled; this.rightEnabled = rightEnabled; this.leftPaddingX = leftPaddingX; this.leftPaddingY = leftPaddingY; this.rightPaddingX = rightPaddingX; this.rightPaddingY = rightPaddingY; this.leftH = leftH; this.leftV = leftV; this.rightH = rightH; this.rightV = rightV; this.cornerRadius = cornerRadius; this.includeNotification = includeNotification; this.safeMode = safeMode; this.crashCount = crashCount; this.lastCrash = lastCrash; this.restorePending = restorePending;
        }
    }
}
