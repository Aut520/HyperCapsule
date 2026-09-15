package com.aut.hypercapsule;

import android.content.Context;
import android.os.Build;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * Compatibility gate for HyperOS 1–4 samples used by the capsule hook.
 * Capsule entry (BarTransitions.applyModeBackground) exists on OS1–OS4;
 * Super Island hide is OS3+ only (class presence is checked separately).
 */
public final class SupportedPlatform {
    public static final int ANDROID_14_SDK = 34;
    public static final int ANDROID_15_SDK = 35;
    public static final int ANDROID_16_SDK = 36;
    public static final int ANDROID_17_SDK = 37;
    public static final int HYPER_OS_1 = 1;
    public static final int HYPER_OS_2 = 2;
    public static final int HYPER_OS_3 = 3;
    public static final int HYPER_OS_4 = 4;

    private static final Pattern MAJOR = Pattern.compile("(?:OS|HyperOS)?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private SupportedPlatform() { }

    public static boolean isSupported() {
        return isSupported(Build.VERSION.SDK_INT, hyperOsMajor());
    }

    public static boolean isSupported(int sdk, int os) {
        if (sdk < ANDROID_14_SDK || sdk > ANDROID_17_SDK) {
            return false;
        }
        return os >= HYPER_OS_1 && os <= HYPER_OS_4;
    }

    /** Super Island hide controller only ships on HyperOS 3+. */
    public static boolean supportsIslandHook(int sdk, int os) {
        return isSupported(sdk, os) && os >= HYPER_OS_3;
    }

    public static boolean supportsIslandHook() {
        return supportsIslandHook(Build.VERSION.SDK_INT, hyperOsMajor());
    }

    public static boolean isSupported(Context context) {
        return isSupported();
    }

    public static int hyperOsMajor() {
        String value = firstProperty(
                "ro.mi.os.version.name",
                "ro.miui.ui.version.name",
                "ro.mi.os.version.incremental",
                "ro.build.version.incremental");
        Matcher matcher = MAJOR.matcher(value);
        return matcher.find() ? parseInt(matcher.group(1)) : -1;
    }

    public static String platformLabel() {
        return "Android " + Build.VERSION.SDK_INT + " / HyperOS " + hyperOsMajor();
    }

    private static int parseInt(String value) {
        try { return Integer.parseInt(value); } catch (Throwable ignored) { return -1; }
    }

    private static String firstProperty(String... keys) {
        for (String key : keys) {
            String value = property(key);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String property(String key) {
        try {
            Class<?> type = Class.forName("android.os.SystemProperties");
            Method get = type.getMethod("get", String.class, String.class);
            Object value = get.invoke(null, key, "");
            return value == null ? "" : value.toString().trim();
        } catch (Throwable ignored) {
            try {
                Process process = new ProcessBuilder("/system/bin/getprop", key)
                        .redirectErrorStream(true)
                        .start();
                if (!process.waitFor(300, TimeUnit.MILLISECONDS)) {
                    process.destroy();
                    return "";
                }
                return new String(process.getInputStream().readAllBytes()).trim();
            } catch (Throwable ignoredAgain) { return ""; }
        }
    }
}
