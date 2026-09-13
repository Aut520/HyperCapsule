package com.aut.hypercapsule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.aut.hypercapsule.ui.AppPreferences;

import org.junit.Test;

import java.util.List;

public final class AppPreferencesDefaultsTest {
    @Test
    public void defaultsMatchProductRequirements() {
        assertTrue(AppPreferences.DEFAULT_TRANSIENT_CAPSULE);
        assertTrue(AppPreferences.DEFAULT_LANDSCAPE);
        assertTrue(AppPreferences.DEFAULT_PORTRAIT);
        assertFalse(AppPreferences.DEFAULT_REGULAR_STATUS_BAR);
        assertFalse(AppPreferences.DEFAULT_HIDE_LANDSCAPE_ISLAND);
        assertTrue(AppPreferences.DEFAULT_INCLUDE_PRIVACY);
        assertTrue(AppPreferences.DEFAULT_CAPSULE_ANIMATION);
        assertTrue(AppPreferences.DEFAULT_DYNAMIC_COLOR);
        assertTrue(AppPreferences.DEFAULT_FLOATING_NAVIGATION);
        assertFalse(AppPreferences.DEFAULT_LIQUID_GLASS);
        assertTrue(AppPreferences.DEFAULT_HAPTIC_FEEDBACK);
    }

    @Test
    public void preferenceKeysAreStableAndUnique() {
        List<String> keys = List.of(
                AppPreferences.KEY_TRANSIENT_CAPSULE,
                AppPreferences.KEY_LANDSCAPE,
                AppPreferences.KEY_PORTRAIT,
                AppPreferences.KEY_REGULAR_STATUS_BAR,
                AppPreferences.KEY_HIDE_LANDSCAPE_ISLAND,
                AppPreferences.KEY_INCLUDE_PRIVACY,
                AppPreferences.KEY_CAPSULE_ANIMATION,
                AppPreferences.KEY_DYNAMIC_COLOR,
                AppPreferences.KEY_FLOATING_NAVIGATION,
                AppPreferences.KEY_LIQUID_GLASS,
                AppPreferences.KEY_HAPTIC_FEEDBACK
        );

        assertEquals(keys.size(), keys.stream().distinct().count());
    }
}
