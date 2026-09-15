package com.aut.hypercapsule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SupportedPlatformTest {

    @Test
    public void supportsHyperOs1Through4OnAndroid14To17() {
        assertTrue(SupportedPlatform.isSupported(34, 1)); // OS1 / A14
        assertTrue(SupportedPlatform.isSupported(35, 2)); // OS2 / A15
        assertTrue(SupportedPlatform.isSupported(36, 3)); // OS3 / A16
        assertTrue(SupportedPlatform.isSupported(37, 4)); // OS4 / A17
    }

    @Test
    public void rejectsOutOfRangeSdkOrOs() {
        assertFalse(SupportedPlatform.isSupported(33, 1));
        assertFalse(SupportedPlatform.isSupported(34, 0));
        assertFalse(SupportedPlatform.isSupported(34, 5));
        assertFalse(SupportedPlatform.isSupported(38, 4));
    }

    @Test
    public void islandHookOnlyOnOs3AndOs4() {
        assertFalse(SupportedPlatform.supportsIslandHook(34, 1));
        assertFalse(SupportedPlatform.supportsIslandHook(35, 2));
        assertTrue(SupportedPlatform.supportsIslandHook(36, 3));
        assertTrue(SupportedPlatform.supportsIslandHook(37, 4));
    }
}
