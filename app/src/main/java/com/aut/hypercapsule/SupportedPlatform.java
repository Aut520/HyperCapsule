package com.aut.hypercapsule;

import android.content.Context;
import android.os.Build;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/** Strict compatibility gate for the two statically analysed SystemUI samples. */
public final class SupportedPlatform {
    public static final int ANDROID_15_SDK = 35;
    public static final int ANDROID_17_SDK = 37;
    public static final int HYPER_OS_2 = 2;
    public static final int HYPER_OS_4 = 4;

    private static final Pattern MAJOR = Pattern.compile("(?:OS|HyperOS)?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private SupportedPlatform() { }

    public static boolean isSupported() {
        return isSupported(Build.VERSION.SDK_INT, hyperOsMajor());
    }

    public static boolean isSupported(int sdk, int os) {
        return (sdk == ANDROID_15_SDK && os == HYPER_OS_2)
                || (sdk == ANDROID_17_SDK && os == HYPER_OS_4);
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
