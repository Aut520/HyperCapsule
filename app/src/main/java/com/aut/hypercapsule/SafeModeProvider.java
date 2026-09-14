package com.aut.hypercapsule;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

import java.util.Objects;

/**
 * Small, caller-checked state channel used by the SystemUI process to report a
 * hook-attributed uncaught exception. Ordinary process restarts never call it.
 */
public final class SafeModeProvider extends ContentProvider {
    public static final String AUTHORITY = "com.aut.hypercapsule.safe-mode";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY);
    public static final String METHOD_STATE = "state";
    public static final String METHOD_RECORD_CRASH = "record_crash";
    public static final String METHOD_CONFIRM = "confirm";
    public static final String RESULT_AUTHORIZED = "authorized";
    public static final String KEY_SAFE_MODE = "hook_safe_mode";
    public static final String KEY_CRASH_COUNT = "hook_crash_count";
    public static final String KEY_LAST_CRASH_TIME = "hook_last_crash_time";
    public static final String KEY_RESTORE_PENDING = "hook_restore_pending";

    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String PREFS = CapsuleConfig.PREFS;
    private static final Object LOCK = new Object();

    @Override public boolean onCreate() {
        return getContext() != null;
    }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        Context context = getContext();
        if (context == null || !isAllowedCaller(context)) return unauthorized();
        synchronized (LOCK) {
            android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (METHOD_RECORD_CRASH.equals(method)) {
                int count = prefs.getInt(KEY_CRASH_COUNT, 0) + 1;
                boolean safe = count >= 3;
                prefs.edit()
                        .putInt(KEY_CRASH_COUNT, count)
                        .putLong(KEY_LAST_CRASH_TIME, System.currentTimeMillis())
                        .putBoolean(KEY_SAFE_MODE, safe)
                        .putBoolean(KEY_RESTORE_PENDING, safe)
                        .apply();
            } else if (METHOD_CONFIRM.equals(method)) {
                if (Binder.getCallingUid() == Process.myUid()) {
                    prefs.edit()
                            .putInt(KEY_CRASH_COUNT, 0)
                            .putLong(KEY_LAST_CRASH_TIME, 0L)
                            .putBoolean(KEY_SAFE_MODE, false)
                            .putBoolean(KEY_RESTORE_PENDING, false)
                            .apply();
                }
            }
            return state(prefs);
        }
    }

    private static Bundle state(android.content.SharedPreferences prefs) {
        Bundle result = new Bundle();
        result.putBoolean(RESULT_AUTHORIZED, true);
        result.putBoolean(KEY_SAFE_MODE, prefs.getBoolean(KEY_SAFE_MODE, false));
        result.putInt(KEY_CRASH_COUNT, prefs.getInt(KEY_CRASH_COUNT, 0));
        result.putLong(KEY_LAST_CRASH_TIME, prefs.getLong(KEY_LAST_CRASH_TIME, 0L));
        result.putBoolean(KEY_RESTORE_PENDING, prefs.getBoolean(KEY_RESTORE_PENDING, false));
        return result;
    }

    private static Bundle unauthorized() {
        Bundle result = new Bundle();
        result.putBoolean(RESULT_AUTHORIZED, false);
        return result;
    }

    private static boolean isAllowedCaller(Context context) {
        int caller = Binder.getCallingUid();
        if (caller == Process.myUid()) return true;
        try {
            String[] packages = context.getPackageManager().getPackagesForUid(caller);
            if (packages == null) return false;
            for (String name : packages) if (SYSTEM_UI_PACKAGE.equals(name)) return true;
        } catch (Throwable ignored) { }
        return false;
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }

    public static Bundle read(Context context) {
        try {
            Bundle result = context.getContentResolver().call(URI, METHOD_STATE, null, null);
            return result != null && result.getBoolean(RESULT_AUTHORIZED, false) ? result : null;
        } catch (Throwable ignored) { return null; }
    }

    public static Bundle recordCrash(Context context) {
        try {
            Bundle result = context.getContentResolver().call(URI, METHOD_RECORD_CRASH, null, null);
            return result != null && result.getBoolean(RESULT_AUTHORIZED, false) ? result : null;
        } catch (Throwable ignored) { return null; }
    }

    public static Bundle confirm(Context context) {
        try {
            Bundle result = context.getContentResolver().call(URI, METHOD_CONFIRM, null, null);
            return result != null && result.getBoolean(RESULT_AUTHORIZED, false) ? result : null;
        } catch (Throwable ignored) { return null; }
    }
}
