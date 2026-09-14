package com.aut.hypercapsule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.aut.hypercapsule.ui.AppPreferences;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AppPreferencesDefaultsTest {
    @Test
    public void capsuleDefaultsMatchProductRequirements() {
        assertTrue(CapsuleConfig.read(new InMemoryPreferences()).transientEnabled);
        assertTrue(CapsuleConfig.read(new InMemoryPreferences()).landscapeEnabled);
        assertTrue(CapsuleConfig.read(new InMemoryPreferences()).portraitEnabled);
        assertFalse(CapsuleConfig.read(new InMemoryPreferences()).regularEnabled);
        assertFalse(CapsuleConfig.read(new InMemoryPreferences()).hideIsland);
        assertTrue(CapsuleConfig.read(new InMemoryPreferences()).includePrivacy);
        assertTrue(CapsuleConfig.read(new InMemoryPreferences()).animationEnabled);
        assertTrue(CapsuleConfig.read(new InMemoryPreferences()).includeNotification);
        assertEquals(CapsuleConfig.TRANSLUCENT, CapsuleConfig.read(new InMemoryPreferences()).material);
        assertEquals(CapsuleConfig.TONE_AUTO, CapsuleConfig.read(new InMemoryPreferences()).tone);
    }

    @Test
    public void managerAppearanceDefaults() {
        assertTrue(AppPreferences.DEFAULT_DYNAMIC_COLOR);
        assertTrue(AppPreferences.DEFAULT_FLOATING_NAVIGATION);
        assertFalse(AppPreferences.DEFAULT_LIQUID_GLASS);
        assertTrue(AppPreferences.DEFAULT_HAPTIC_FEEDBACK);
    }

    @Test
    public void preferenceKeysAreStableAndUnique() {
        List<String> keys = Arrays.asList(
                CapsuleConfig.TRANSIENT,
                CapsuleConfig.LANDSCAPE,
                CapsuleConfig.PORTRAIT,
                CapsuleConfig.REGULAR,
                CapsuleConfig.HIDE_ISLAND,
                CapsuleConfig.INCLUDE_PRIVACY,
                CapsuleConfig.ANIMATION,
                CapsuleConfig.LEFT_ENABLED,
                CapsuleConfig.RIGHT_ENABLED,
                CapsuleConfig.INCLUDE_NOTIFICATION,
                AppPreferences.KEY_DYNAMIC_COLOR,
                AppPreferences.KEY_THEME_COLOR_MODE,
                AppPreferences.KEY_THEME_PALETTE,
                AppPreferences.KEY_FLOATING_NAVIGATION,
                AppPreferences.KEY_LIQUID_GLASS,
                AppPreferences.KEY_HAPTIC_FEEDBACK
        );

        Set<String> unique = new HashSet<>(keys);
        assertEquals(keys.size(), unique.size());
    }

    @Test
    public void materialAndToneTokensAreUnique() {
        List<String> materials = Arrays.asList(
                CapsuleConfig.ORIGINAL,
                CapsuleConfig.TRANSLUCENT,
                CapsuleConfig.GAUSSIAN_BLUR,
                CapsuleConfig.SOFT_GLASS,
                CapsuleConfig.LIQUID_GLASS
        );
        List<String> tones = Arrays.asList(
                CapsuleConfig.TONE_AUTO,
                CapsuleConfig.TONE_DARK,
                CapsuleConfig.TONE_LIGHT
        );
        assertEquals(materials.size(), new HashSet<>(materials).size());
        assertEquals(tones.size(), new HashSet<>(tones).size());
        for (String tone : tones) {
            assertFalse("tone collides with material: " + tone, materials.contains(tone));
        }
    }

    /** Minimal SharedPreferences used only to exercise CapsuleConfig defaults. */
    private static final class InMemoryPreferences implements android.content.SharedPreferences {
        @Override public java.util.Map<String, ?> getAll() { return java.util.Collections.emptyMap(); }
        @Override public String getString(String key, String defValue) { return defValue; }
        @Override public java.util.Set<String> getStringSet(String key, java.util.Set<String> defValues) { return defValues; }
        @Override public int getInt(String key, int defValue) { return defValue; }
        @Override public long getLong(String key, long defValue) { return defValue; }
        @Override public float getFloat(String key, float defValue) { return defValue; }
        @Override public boolean getBoolean(String key, boolean defValue) { return defValue; }
        @Override public boolean contains(String key) { return false; }
        @Override public android.content.SharedPreferences.Editor edit() { throw new UnsupportedOperationException(); }
        @Override public void registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener listener) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener listener) { }
    }
}
