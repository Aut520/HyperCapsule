package com.aut.hypercapsule;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/** SystemUI implementation for the analysed Android 15/17 SystemUI samples. */
public final class HyperCapsuleModule extends XposedModule {
    private static final String TAG = "HyperCapsule";
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String A15_TRANSITIONS =
            "com.android.systemui.statusbar.phone.BarTransitions";
    private static final String A17_TRANSITIONS =
            "com.android.systemui.shared.statusbar.phone.BarTransitions";
    private static final String A17_ISLAND_CONTROLLER =
            "com.android.systemui.statusbar.StatusBarIslandControllerImpl";
    /** HyperIsland plugin queries SystemUI through this controller (OS3/OS4). */
    private static final String DYNAMIC_ISLAND_CONTROLLER =
            "com.android.systemui.statusbar.notification.DynamicIslandController";
    /**
     * Island UI lives in the SystemUI plugin package (miui.systemui.plugin), not under
     * the status-bar View tree. Class names taken from HyperIsland OS3/OS4 hooks.
     */
    private static final String ISLAND_WINDOW_VIEW_CONTROLLER =
            "miui.systemui.dynamicisland.window.DynamicIslandWindowViewController";
    private static final String ISLAND_WINDOW_VIEW =
            "miui.systemui.dynamicisland.window.DynamicIslandWindowView";
    private static final String ISLAND_CONTENT_VIEW =
            "miui.systemui.dynamicisland.window.content.DynamicIslandContentView";
    private static final String ISLAND_BASE_CONTENT_VIEW =
            "miui.systemui.dynamicisland.window.content.DynamicIslandBaseContentView";
    private static final String PLUGIN_FACTORY =
            "com.android.systemui.shared.plugins.PluginInstance$PluginFactory";
    private static final String FOCUS_NOTIFICATION_CONTROLLER =
            "miui.systemui.notification.focus.FocusNotificationController";
    private static final String ACTION_BACK_REQUEST_IMMERSIVE_MODE =
            "action_back_request_immersive_mode";
    private static final String ACTION_BACK_ADD_ISLAND = "action_back_add_island";
    private static final String ACTION_REMOVE_ISLAND = "action_remove_island";
    private static final String ACTION_REQUEST_HAS_ISLAND = "action_request_has_island";
    private static final String EXTRA_BACK_REQUEST_IMMERSIVE_MODE =
            "extra_back_request_immersive_mode";
    private static final String EXTRA_HAS_ISLAND = "extra_has_island";

    private static final Object LOCK = new Object();
    private static final Map<View, Drawable> ORIGINALS = new WeakHashMap<>();
    private static final Map<View, Integer> MODES = new WeakHashMap<>();

    private static boolean transitionHookInstalled;
    private static boolean applicationHookInstalled;
    private static boolean islandBootstrapInstalled;
    private static boolean classLoaderHooksInstalled;
    private static boolean pluginFactoryHookInstalled;
    private static boolean preferenceListenerInstalled;
    private static Context systemUiContext;
    private static SharedPreferences remotePreferences;
    private static Thread.UncaughtExceptionHandler previousExceptionHandler;

    /** Last observed status bar transition mode; 1 = transient/pull-down. */
    private static volatile int lastStatusBarMode = -1;
    /** Cached config for SystemUI hot paths; avoid re-reading remote prefs every frame. */
    private static volatile CapsuleConfig.Values cachedConfig;
    private static volatile boolean cachedHideIsland;
    private static volatile boolean cachedSafeMode;
    private static volatile long lastSafeModeCheckMs;

    /** Island instances live in the plugin window — track hide entry points, not bar views. */
    private static final Map<Object, Method> CONTENT_HIDE_METHODS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Method> WINDOW_TEMP_HIDE_METHODS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Method> FOCUS_REMOVE_METHODS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Integer> HOOKED_ISLAND_CLASS_LOADERS =
            Collections.synchronizedSet(new HashSet<>());
    private static final Set<Integer> HOOKED_ISLAND_METHODS =
            Collections.synchronizedSet(new HashSet<>());

    private static final SharedPreferences.OnSharedPreferenceChangeListener PREFERENCE_LISTENER =
            (preferences, key) -> {
                synchronized (LOCK) {
                    cachedConfig = null;
                    cachedHideIsland = preferences != null
                            && preferences.getBoolean(CapsuleConfig.HIDE_ISLAND, false);
                    cachedSafeMode = preferences != null
                            && preferences.getBoolean(CapsuleConfig.SAFE_MODE, false);
                }
                refreshAll();
                // Switch turned off (or other pref change): restore island if we hid it.
                try {
                    forceShowTrackedIsland();
                } catch (Throwable ignored) {
                }
            };

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Loaded in " + param.getProcessName());
        log(Log.INFO, TAG, "Platform " + SupportedPlatform.platformLabel());
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!SYSTEM_UI_PACKAGE.equals(param.getPackageName()) || transitionHookInstalled) return;
        if (!SupportedPlatform.isSupported()) {
            log(Log.WARN, TAG, "Hooks disabled on unsupported "
                    + SupportedPlatform.platformLabel());
            return;
        }
        try {
            Class<?> transitions = resolveTransitionsClass(param.getClassLoader());
            if (transitions == null) {
                log(Log.ERROR, TAG, "BarTransitions not found for "
                        + SupportedPlatform.platformLabel());
                return;
            }
            Method applyMode = transitions.getDeclaredMethod(
                    "applyModeBackground", int.class, boolean.class);
            applyMode.setAccessible(true);
            hook(applyMode)
                    .setId("hypercapsule.applyModeBackground")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            applyModeBackground(chain.getThisObject(), chain.getArg(0));
                        } catch (Throwable error) {
                            log(Log.WARN, TAG, "Capsule update skipped", error);
                        }
                        return result;
                    });
            transitionHookInstalled = true;
            installApplicationContextHook(param.getClassLoader());
            installIslandHideHooks(param.getClassLoader());
            installDynamicClassLoaderHooks(param.getClassLoader());
            installPluginFactoryHook(param.getClassLoader());
            installCrashGuard();
            log(Log.INFO, TAG, "Installed " + transitions.getName() + ".applyModeBackground");
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "SystemUI hook installation failed", error);
        }
    }

    /**
     * OS1/OS2 keep BarTransitions under statusbar.phone; OS3/OS4 moved it to
     * shared.statusbar.phone. Prefer the SDK hint, then try the other package.
     */
    private static Class<?> resolveTransitionsClass(ClassLoader loader) {
        boolean preferShared = Build.VERSION.SDK_INT >= SupportedPlatform.ANDROID_16_SDK;
        String[] order = preferShared
                ? new String[] { A17_TRANSITIONS, A15_TRANSITIONS }
                : new String[] { A15_TRANSITIONS, A17_TRANSITIONS };
        for (String name : order) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException ignored) {
                // try next package
            }
        }
        return null;
    }

    private void installApplicationContextHook(ClassLoader loader) {
        if (applicationHookInstalled) return;
        try {
            Method onCreate = Class.forName("android.app.Application", false, loader)
                    .getDeclaredMethod("onCreate");
            onCreate.setAccessible(true);
            hook(onCreate)
                    .setId("hypercapsule.applicationContext")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            if (chain.getThisObject() instanceof Context) {
                                systemUiContext = ((Context) chain.getThisObject())
                                        .getApplicationContext();
                            }
                            installCrashGuard();
                            // Island plugin may finish loading after Application.onCreate.
                            try {
                                ClassLoader cl = chain.getThisObject() != null
                                        ? chain.getThisObject().getClass().getClassLoader()
                                        : null;
                                if (cl != null) {
                                    installIslandHideHooks(cl);
                                    installPluginFactoryHook(cl);
                                }
                            } catch (Throwable ignored) {
                            }
                        } catch (Throwable error) {
                            log(Log.WARN, TAG, "Context setup skipped", error);
                        }
                        return result;
                    });
            applicationHookInstalled = true;
        } catch (Throwable error) {
            log(Log.INFO, TAG, "Application context hook unavailable", error);
        }
    }

    /**
     * Island hide anchors (from HyperIsland OS3/OS4), intentionally narrow:
     * hide only when the user switch is ON and the scene is landscape fullscreen
     * (or landscape + status-bar pull-down). Portrait / portrait-fullscreen /
     * landscape-with-regular-bar must stay system default.
     *
     * Do NOT force immersive Bundle replies or canEnterAppState=false — HyperIsland
     * caches those signals and can stop showing islands in every orientation.
     */
    private void installIslandHideHooks(ClassLoader loader) {
        if (!SupportedPlatform.supportsIslandHook()) {
            return;
        }
        boolean any = false;
        any |= installIslandWindowControllerHook(loader);
        any |= installIslandContentLayoutHook(loader);
        any |= installIslandWindowTempHideHook(loader);
        islandBootstrapInstalled |= any;
        if (any) {
            log(Log.INFO, TAG, "Island hide hooks ready on loader "
                    + Integer.toHexString(System.identityHashCode(loader)));
        }
    }

    /** Watch new ClassLoaders so island plugin classes get hooked after SystemUI starts. */
    private void installDynamicClassLoaderHooks(ClassLoader loader) {
        if (classLoaderHooksInstalled) return;
        String[] loaders = {
                "dalvik.system.BaseDexClassLoader",
                "dalvik.system.PathClassLoader",
                "dalvik.system.DexClassLoader",
                "dalvik.system.DelegateLastClassLoader",
        };
        boolean any = false;
        for (String name : loaders) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                for (java.lang.reflect.Constructor<?> ctor : type.getDeclaredConstructors()) {
                    try {
                        ctor.setAccessible(true);
                        hook(ctor)
                                .setId("hypercapsule.classLoader." + name)
                                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                                .intercept(chain -> {
                                    Object result = chain.proceed();
                                    try {
                                        Object candidate = chain.getThisObject();
                                        if (candidate instanceof ClassLoader) {
                                            installIslandHideHooks((ClassLoader) candidate);
                                        }
                                    } catch (Throwable ignored) {
                                    }
                                    return result;
                                });
                        any = true;
                    } catch (Throwable ignored) {
                        // some constructors may already be hooked
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // try next loader type
            }
        }
        classLoaderHooksInstalled = any;
        if (any) {
            log(Log.INFO, TAG, "Watching ClassLoaders for island plugin");
        }
    }

    private void installPluginFactoryHook(ClassLoader loader) {
        if (pluginFactoryHookInstalled) return;
        try {
            Class<?> factory = Class.forName(PLUGIN_FACTORY, false, loader);
            for (Method method : factory.getDeclaredMethods()) {
                if (!"createPluginContext".equals(method.getName())) continue;
                method.setAccessible(true);
                hook(method)
                        .setId("hypercapsule.pluginFactory")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            try {
                                if (result instanceof Context) {
                                    ClassLoader pluginCl = ((Context) result).getClassLoader();
                                    if (pluginCl != null) {
                                        installIslandHideHooks(pluginCl);
                                    }
                                }
                            } catch (Throwable ignored) {
                            }
                            return result;
                        });
                pluginFactoryHookInstalled = true;
                log(Log.INFO, TAG, "Waiting for island plugin ClassLoader via PluginFactory");
            }
        } catch (ClassNotFoundException ignored) {
            // Plugin factory may not exist on every build.
        } catch (Throwable error) {
            log(Log.WARN, TAG, "PluginFactory hook unavailable", error);
        }
    }

    private boolean installIslandWindowControllerHook(ClassLoader loader) {
        try {
            Class<?> controller = Class.forName(ISLAND_WINDOW_VIEW_CONTROLLER, false, loader);
            boolean hooked = false;
            for (Method method : controller.getDeclaredMethods()) {
                // Only statusBarAppearance — system's own "bar showing → temp-hide island".
                // Do not touch canEnterAppState / lockScreen / panel height: they break
                // island recovery in portrait when mis-applied.
                if (!"statusBarAppearance".equals(method.getName())) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || params[0] != boolean.class) continue;
                int key = System.identityHashCode(method) ^ method.getName().hashCode();
                if (!HOOKED_ISLAND_METHODS.add(key)) continue;
                method.setAccessible(true);
                hook(method)
                        .setId("hypercapsule.island.statusBarAppearance")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object[] args = chain.getArgs().toArray();
                            if (shouldHideLandscapeIsland()) {
                                args[0] = true;
                            }
                            return chain.proceed(args);
                        });
                hooked = true;
            }
            if (hooked) {
                log(Log.INFO, TAG, "Installed "
                        + ISLAND_WINDOW_VIEW_CONTROLLER + ".statusBarAppearance");
            }
            return hooked;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Island window controller hook unavailable", error);
            return false;
        }
    }

    private boolean installIslandContentLayoutHook(ClassLoader loader) {
        String[] candidates = { ISLAND_CONTENT_VIEW, ISLAND_BASE_CONTENT_VIEW };
        boolean hookedAny = false;
        for (String className : candidates) {
            try {
                Class<?> contentView = Class.forName(className, false, loader);
                Method show = null;
                Method hide = null;
                for (Method method : contentView.getDeclaredMethods()) {
                    if (method.getParameterCount() != 0) continue;
                    if ("showIslandLayout".equals(method.getName())) show = method;
                    if ("hideIslandLayout".equals(method.getName())) hide = method;
                }
                if (show == null && hide == null) continue;
                final Method hideMethod = hide;
                if (show != null) {
                    show.setAccessible(true);
                    int key = System.identityHashCode(show);
                    if (HOOKED_ISLAND_METHODS.add(key)) {
                        hook(show)
                                .setId("hypercapsule.island.showIslandLayout")
                                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                                .intercept(chain -> {
                                    Object target = chain.getThisObject();
                                    if (target != null && hideMethod != null) {
                                        CONTENT_HIDE_METHODS.put(target, hideMethod);
                                    }
                                    // Only skip show in the narrow hide scene.
                                    if (shouldHideLandscapeIsland()) {
                                        invokeNoArg(target, hideMethod);
                                        return null;
                                    }
                                    return chain.proceed();
                                });
                        hookedAny = true;
                    }
                }
                if (hide != null) {
                    hide.setAccessible(true);
                    hide.setAccessible(true);
                    final Method hideRef = hide;
                    hook(hideRef)
                            .setId("hypercapsule.island.hideIslandLayout")
                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .intercept(chain -> {
                                Object target = chain.getThisObject();
                                if (target != null && hideRef != null) {
                                    CONTENT_HIDE_METHODS.put(target, hideRef);
                                }
                                return chain.proceed();
                            });
                    hookedAny = true;
                }
                if (hookedAny) {
                    log(Log.INFO, TAG, "Installed island layout hooks on " + className);
                }
            } catch (ClassNotFoundException ignored) {
                // try next class
            } catch (Throwable error) {
                log(Log.WARN, TAG, "Island layout hook failed for " + className, error);
            }
        }
        return hookedAny;
    }

    private boolean installIslandWindowTempHideHook(ClassLoader loader) {
        try {
            Class<?> windowView = Class.forName(ISLAND_WINDOW_VIEW, false, loader);
            boolean hooked = false;
            for (Method method : windowView.getDeclaredMethods()) {
                if (!"onIslandTempHide".equals(method.getName())) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length < 1 || params[0] != boolean.class) continue;
                method.setAccessible(true);
                int key = System.identityHashCode(method);
                if (!HOOKED_ISLAND_METHODS.add(key)) continue;
                final Method target = method;
                hook(target)
                        .setId("hypercapsule.island.onIslandTempHide")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object receiver = chain.getThisObject();
                            if (receiver != null) {
                                WINDOW_TEMP_HIDE_METHODS.put(receiver, target);
                            }
                            Object[] args = chain.getArgs().toArray();
                            if (shouldHideLandscapeIsland()) {
                                args[0] = true;
                            }
                            // When hide scene ends, allow (and prefer) un-hide.
                            return chain.proceed(args);
                        });
                hooked = true;
            }
            if (hooked) {
                log(Log.INFO, TAG, "Installed " + ISLAND_WINDOW_VIEW + ".onIslandTempHide");
            }
            return hooked;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Island window temp-hide hook unavailable", error);
            return false;
        }
    }

    private static Object invokeNoArg(Object target, Method method) {
        if (target == null || method == null) return null;
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Push SystemUI's own temp-hide path onto tracked island instances. */
    private static void forceHideTrackedIsland() {
        if (!shouldHideLandscapeIsland()) return;
        invokeTrackedIslandTempHide(true);
    }

    /**
     * Recover from an over-hide: once the hide scene ends (portrait, switch off,
     * landscape but not fullscreen), ask SystemUI to restore the island window.
     */
    private static void forceShowTrackedIsland() {
        if (shouldHideLandscapeIsland()) return;
        invokeTrackedIslandTempHide(false);
    }

    private static void invokeTrackedIslandTempHide(boolean hidden) {
        try {
            Map<Object, Method> windows = new java.util.HashMap<>();
            synchronized (WINDOW_TEMP_HIDE_METHODS) {
                windows.putAll(WINDOW_TEMP_HIDE_METHODS);
            }
            for (Map.Entry<Object, Method> entry : windows.entrySet()) {
                try {
                    Method method = entry.getValue();
                    method.setAccessible(true);
                    Object[] args = new Object[method.getParameterCount()];
                    if (args.length > 0) args[0] = hidden;
                    for (int i = 1; i < args.length; i++) {
                        Class<?> type = method.getParameterTypes()[i];
                        if (type == boolean.class) args[i] = false;
                        else if (type == int.class) args[i] = 0;
                        else if (type == float.class) args[i] = 0f;
                    }
                    method.invoke(entry.getKey(), args);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void installCrashGuard() {
        if (previousExceptionHandler != null) return;
        final Thread.UncaughtExceptionHandler previous =
                Thread.getDefaultUncaughtExceptionHandler();
        previousExceptionHandler = previous;
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                if (containsModuleFrame(throwable, 0)) {
                    Context context = systemUiContext;
                    if (context != null) SafeModeProvider.recordCrash(context);
                    log(Log.ERROR, TAG, "Hook-attributed SystemUI crash", throwable);
                }
            } catch (Throwable ignored) {
                // Reporting must never interfere with Android's crash handler.
            }
            if (previous != null) previous.uncaughtException(thread, throwable);
        });
    }

    private static boolean containsModuleFrame(Throwable throwable, int depth) {
        if (throwable == null || depth > 12) return false;
        for (StackTraceElement frame : throwable.getStackTrace()) {
            if (frame.getClassName().startsWith("com.aut.hypercapsule.")) return true;
        }
        return containsModuleFrame(throwable.getCause(), depth + 1);
    }

    private void applyModeBackground(Object transition, Object modeValue) throws Exception {
        if (!(modeValue instanceof Integer)) return;
        View root = findStatusBarView(transition);
        if (root == null) return;
        systemUiContext = root.getContext().getApplicationContext();
        SharedPreferences preferences = preferences();
        registerPreferenceListener(preferences);
        if (preferences != null) {
            cachedHideIsland = preferences.getBoolean(CapsuleConfig.HIDE_ISLAND, false);
        }
        int mode = (Integer) modeValue;
        lastStatusBarMode = mode;
        synchronized (LOCK) {
            MODES.put(root, mode);
            captureOriginal(root);
        }
        updateRoot(root, mode, preferences);
        // Hide only in the narrow landscape-fullscreen scene; otherwise recover.
        if (shouldHideLandscapeIsland()) {
            if (isStatusBarVisible()) {
                forceHideTrackedIsland();
                forceHideLandscapeIslandViews(root);
            }
        } else {
            forceShowTrackedIsland();
        }
    }

    /** True when the status bar is pulled down / visible (esp. transient mode 1). */
    private static boolean isStatusBarVisible() {
        int mode = lastStatusBarMode;
        return mode == 1 || mode == 0 || mode == 4 || mode == 7;
    }

    /**
     * Hide only when ALL of:
     * 1) user switch ON
     * 2) landscape
     * 3) landscape-fullscreen scene (immersive policy / bar translucent-hidden / pull-down)
     * Portrait, portrait-fullscreen, and landscape with a normal bar stay system default.
     */
    private static boolean shouldHideLandscapeIsland() {
        SharedPreferences preferences = remotePreferences;
        if (preferences != null) {
            cachedHideIsland = preferences.getBoolean(CapsuleConfig.HIDE_ISLAND, false);
        }
        if (!cachedHideIsland) {
            return false;
        }
        if (!isLandscape()) {
            return false;
        }
        return isLandscapeFullscreenScene();
    }

    private static boolean isLandscapeFullscreenScene() {
        if (isImmersivePolicy()) {
            return true;
        }
        int mode = lastStatusBarMode;
        // Landscape + status-bar pull-down over a fullscreen app.
        if (mode == 1) {
            return true;
        }
        // Translucent / lights-out / hidden bar — typical fullscreen landscape.
        if (mode != -1 && !isNormalMode(mode)) {
            return true;
        }
        return false;
    }

    /** HyperIsland uses the same policy_control probe for immersive fullscreen. */
    private static boolean isImmersivePolicy() {
        try {
            Context context = systemUiContext;
            if (context == null) {
                return false;
            }
            String policy = Settings.Global.getString(
                    context.getContentResolver(), "policy_control");
            if (policy == null) {
                return false;
            }
            String value = policy.toLowerCase();
            return value.contains("immersive.full") || value.contains("immersive.status");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static CapsuleConfig.Values cachedConfig(SharedPreferences preferences) {
        CapsuleConfig.Values values = cachedConfig;
        if (values != null) {
            return values;
        }
        values = CapsuleConfig.read(preferences);
        cachedConfig = values;
        return values;
    }

    /** Hide island-related views under the status bar tree when switch+landscape. */
    private static void forceHideLandscapeIslandViews(View root) {
        if (root == null || !shouldHideLandscapeIsland()) {
            return;
        }
        try {
            hideIslandViewsRecursive(root, 0);
        } catch (Throwable ignored) {
        }
    }

    private static void hideIslandViewsRecursive(View view, int depth) {
        if (view == null || depth > 16) {
            return;
        }
        try {
            if (view.getId() != View.NO_ID) {
                String name = view.getResources().getResourceEntryName(view.getId());
                if (name != null) {
                    String lower = name.toLowerCase();
                    if (lower.contains("island") || lower.contains("dynamic_island")
                            || lower.contains("ongoing_activity_chip")) {
                        if (view.getVisibility() == View.VISIBLE) {
                            view.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                hideIslandViewsRecursive(group.getChildAt(i), depth + 1);
            }
        }
    }

    private static void updateRoot(View root, int mode, SharedPreferences preferences) {
        try {
            if (isSafeMode(root, preferences)) {
                restore(root);
                return;
            }
            CapsuleConfig.Values config = CapsuleConfig.read(preferences);
            boolean landscape = CapsuleConfig.isLandscape(
                    root.getResources().getConfiguration());
            boolean directionEnabled = landscape
                    ? config.landscapeEnabled : config.portraitEnabled;
            boolean temporary = mode == 1 && config.transientEnabled && directionEnabled;
            boolean regular = config.regularEnabled && directionEnabled && isNormalMode(mode);
            boolean target = !isLocked(root) && (temporary || regular)
                    && (config.leftEnabled || config.rightEnabled)
                    && !CapsuleConfig.ORIGINAL.equals(config.material);
            synchronized (LOCK) {
                Drawable original = ORIGINALS.get(root);
                if (target) {
                    if (!(root.getBackground() instanceof CapsuleDrawable)) {
                        root.setBackground(new CapsuleDrawable(root, original, preferences));
                    }
                } else if (root.getBackground() instanceof CapsuleDrawable) {
                    root.setBackground(original);
                }
                Drawable background = root.getBackground();
                if (background != null) background.invalidateSelf();
            }
        } catch (Throwable error) {
            restore(root);
        }
    }

    private static boolean isNormalMode(int mode) {
        return mode == 0 || mode == 2 || mode == 4 || mode == 6 || mode == 7;
    }

    private SharedPreferences preferences() {
        SharedPreferences cached = remotePreferences;
        if (cached != null) return cached;
        try {
            remotePreferences = getRemotePreferences(CapsuleConfig.PREFS);
            return remotePreferences;
        } catch (Throwable error) {
            // Temporary fallback only — never cache SystemUI's local prefs as the
            // remote store, otherwise a one-shot failure permanently disables config.
            Context context = systemUiContext;
            if (context != null) {
                return context.getSharedPreferences(CapsuleConfig.PREFS, Context.MODE_PRIVATE);
            }
            throw error;
        }
    }

    private static void registerPreferenceListener(SharedPreferences preferences) {
        if (preferenceListenerInstalled) return;
        synchronized (LOCK) {
            if (preferenceListenerInstalled) return;
            // Bind only to the remote store; a temporary SystemUI-local fallback
            // would never see manager-app writes and would pin the listener forever.
            if (preferences != remotePreferences) return;
            preferences.registerOnSharedPreferenceChangeListener(PREFERENCE_LISTENER);
            preferenceListenerInstalled = true;
        }
    }

    private static void captureOriginal(View root) {
        Drawable current = root.getBackground();
        if (!(current instanceof CapsuleDrawable) && !ORIGINALS.containsKey(root)) {
            ORIGINALS.put(root, current);
        }
    }

    private static View findStatusBarView(Object transition) {
        if (transition == null || !isStatusBarTransition(transition)) return null;
        Class<?> type = transition.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("mView");
                field.setAccessible(true);
                Object value = field.get(transition);
                if (!(value instanceof View)) return null;
                View candidate = (View) value;
                int containerId = candidate.getResources().getIdentifier(
                        "status_bar_container", "id", SYSTEM_UI_PACKAGE);
                if (containerId != 0 && candidate.getId() == containerId) return candidate;
                View container = containerId == 0 ? null : candidate.findViewById(containerId);
                if (container != null) return container;
                int clockId = candidate.getResources().getIdentifier(
                        "clock", "id", SYSTEM_UI_PACKAGE);
                return clockId != 0 && candidate.findViewById(clockId) != null
                        ? candidate : null;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable error) {
                return null;
            }
        }
        return null;
    }

    private static boolean isStatusBarTransition(Object transition) {
        Class<?> type = transition.getClass();
        while (type != null) {
            if (type.getName().endsWith("PhoneStatusBarTransitions")) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean isLocked(View root) {
        try {
            KeyguardManager manager = (KeyguardManager) root.getContext()
                    .getSystemService(Context.KEYGUARD_SERVICE);
            return manager != null && manager.isKeyguardLocked();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean isLandscape() {
        try {
            return systemUiContext != null && CapsuleConfig.isLandscape(
                    systemUiContext.getResources().getConfiguration());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isSafeMode(View root, SharedPreferences preferences) {
        if (preferences != null
                && preferences.getBoolean(CapsuleConfig.SAFE_MODE, false)) {
            return true;
        }
        if (cachedSafeMode) {
            return true;
        }
        // ContentProvider IPC can wake the manager app — throttle hard-path checks.
        long now = SystemClock.uptimeMillis();
        if (now - lastSafeModeCheckMs < 60_000L) {
            return false;
        }
        lastSafeModeCheckMs = now;
        try {
            android.os.Bundle state = SafeModeProvider.read(root.getContext());
            cachedSafeMode = state != null
                    && state.getBoolean(SafeModeProvider.KEY_SAFE_MODE, false);
            return cachedSafeMode;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void restore(View root) {
        synchronized (LOCK) {
            if (root.getBackground() instanceof CapsuleDrawable) {
                root.setBackground(ORIGINALS.get(root));
            }
        }
    }

    private static void refreshAll() {
        synchronized (LOCK) {
            for (Map.Entry<View, Integer> entry : MODES.entrySet()) {
                View root = entry.getKey();
                Integer mode = entry.getValue();
                if (root == null || mode == null) continue;
                final SharedPreferences prefs = remotePreferences;
                if (prefs == null) continue;
                root.post(() -> {
                    try {
                        updateRoot(root, mode, prefs);
                    } catch (Throwable ignored) { }
                });
            }
        }
    }

    private static final class CapsuleDrawable extends Drawable {
        private final View root;
        private final Drawable original;
        private final SharedPreferences preferences;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF working = new RectF();
        private final NativeBlur leftBlur = new NativeBlur();
        private final NativeBlur rightBlur = new NativeBlur();
        private int alpha = 255;
        private long firstFrame = SystemClock.uptimeMillis();

        CapsuleDrawable(View root, Drawable original, SharedPreferences preferences) {
            this.root = root;
            this.original = original;
            this.preferences = preferences;
        }

        @Override
        public void draw(Canvas canvas) {
            try {
                CapsuleConfig.Values config = cachedConfig(preferences);
                if (cachedSafeMode || isLocked(root)
                        || CapsuleConfig.ORIGINAL.equals(config.material)) {
                    drawOriginal(canvas);
                    return;
                }
                // HyperOS switches the live notification icons between drip and
                // fullscreen containers; only one of them is VISIBLE at a time.
                RectF left = config.includeNotification
                        ? union(
                                "clock",
                                "fullscreen_notification_icon_area",
                                "notification_icon_area",
                                "ongoing_activity_chip_primary",
                                "ongoing_activity_chip")
                        : union("clock");
                RectF right = union("statusIcons", "system_icons", "battery");
                if (config.includePrivacy) {
                    addUnion(right, union("privacy_container", "home_privacy_container"));
                }
                if (left.isEmpty() && right.isEmpty()) {
                    drawOriginal(canvas);
                    return;
                }
                float density = root.getResources().getDisplayMetrics().density;
                left = capsuleBounds(left, config.leftH, config.leftV,
                        config.leftPaddingX, config.leftPaddingY, config.height,
                        config.horizontalPadding, config.verticalInset, density);
                right = capsuleBounds(right, config.rightH, config.rightV,
                        config.rightPaddingX, config.rightPaddingY, config.height,
                        config.horizontalPadding, config.verticalInset, density);
                left.offset(dp(config.globalX + config.leftX, density),
                        dp(config.globalY + config.leftY, density));
                right.offset(dp(config.globalX + config.rightX, density),
                        dp(config.globalY + config.rightY, density));

                float opacity = config.alpha * alpha / 255f;
                if (config.animationEnabled) {
                    float progress = Math.min(1f, Math.max(0f,
                            (SystemClock.uptimeMillis() - firstFrame) / 180f));
                    opacity *= progress;
                    if (progress < 1f) root.postInvalidateOnAnimation();
                }
                configurePaints(config, opacity, density);
                if (config.leftEnabled && !left.isEmpty())
                    drawPill(canvas, left, config, leftBlur, opacity);
                else leftBlur.disable();
                if (config.rightEnabled && !right.isEmpty())
                    drawPill(canvas, right, config, rightBlur, opacity);
                else rightBlur.disable();
            } catch (Throwable error) {
                drawOriginal(canvas);
            }
        }

        private void drawOriginal(Canvas canvas) {
            if (original != null) original.draw(canvas);
        }

        private void configurePaints(CapsuleConfig.Values config, float opacity, float density) {
            int baseAlpha = (int) (Math.max(0f, Math.min(1f, opacity)) * 255f);
            String material = config.material;
            boolean light = CapsuleConfig.TONE_LIGHT.equals(config.tone)
                    || (CapsuleConfig.TONE_AUTO.equals(config.tone)
                    && (root.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES);
            int channel = light ? 255 : 0;
            fill.setShader(null);
            fill.setColor(Color.argb(baseAlpha, channel, channel, channel));
            fill.setStyle(Paint.Style.FILL);
            outline.setShader(null);
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(Math.max(1f, density * (0.5f + config.edgeStrength * 1.5f)));
            outline.setColor(Color.argb((int) (255f * config.edgeStrength), 255, 255, 255));
            if (CapsuleConfig.SOFT_GLASS.equals(material)) {
                float radians = (float) Math.toRadians(config.lightDirection);
                float dx = (float) Math.cos(radians) * root.getWidth();
                float dy = (float) Math.sin(radians) * root.getHeight();
                fill.setShader(new LinearGradient(0f, 0f, dx, dy,
                        Color.argb((int) (baseAlpha * .80f), 255, 255, 255),
                        Color.argb((int) (baseAlpha * (0.25f + config.refractionStrength * .35f)),
                                light ? 170 : 20, light ? 190 : 30, light ? 220 : 45),
                        Shader.TileMode.CLAMP));
                outline.setColor(Color.argb((int) (baseAlpha * .60f), 255, 255, 255));
            } else if (CapsuleConfig.LIQUID_GLASS.equals(material)) {
                float radians = (float) Math.toRadians(config.lightDirection);
                float dx = (float) Math.cos(radians) * root.getWidth();
                float dy = (float) Math.sin(radians) * root.getHeight();
                fill.setShader(new LinearGradient(0f, 0f, dx, dy,
                        Color.argb((int) (baseAlpha * .88f), 255, 255, 255),
                        Color.argb((int) (baseAlpha * (.20f + config.refractionStrength * .50f)),
                                light ? 115 : 10, light ? 165 : 24, light ? 235 : 55),
                        Shader.TileMode.CLAMP));
                outline.setColor(Color.argb((int) (baseAlpha * .78f), 255, 255, 255));
            }
        }

        private void drawPill(Canvas canvas, RectF bounds, CapsuleConfig.Values config,
                              NativeBlur blur, float opacity) {
            working.set(bounds);
            float density = root.getResources().getDisplayMetrics().density;
            float radius = config.cornerRadius >= 0f
                    ? dp(config.cornerRadius, density)
                    : working.height() / 2f;
            boolean wantsBlur = CapsuleConfig.GAUSSIAN_BLUR.equals(config.material)
                    || CapsuleConfig.SOFT_GLASS.equals(config.material)
                    || CapsuleConfig.LIQUID_GLASS.equals(config.material);
            boolean blurred = wantsBlur && blur.draw(canvas, root, working, radius,
                    Math.round(dp(config.blurRadius, root.getResources().getDisplayMetrics().density)),
                    blurColor(config, opacity));
            if (!blurred || CapsuleConfig.GAUSSIAN_BLUR.equals(config.material))
                canvas.drawRoundRect(working, radius, radius, fill);
            if (CapsuleConfig.SOFT_GLASS.equals(config.material)
                    || CapsuleConfig.LIQUID_GLASS.equals(config.material)) {
                canvas.drawRoundRect(working, radius, radius, outline);
            }
        }

        private RectF union(String... names) {
            RectF result = new RectF();
            boolean found = false;
            for (String name : names) {
                int id = root.getResources().getIdentifier(
                        name, "id", SYSTEM_UI_PACKAGE);
                if (id == 0) continue;
                View view = root.findViewById(id);
                if (view == null || view.getVisibility() != View.VISIBLE) continue;
                RectF part = new RectF();
                if (!collectContent(view, part)) continue;
                if (!found) {
                    result.set(part);
                    found = true;
                } else {
                    result.union(part);
                }
            }
            return result;
        }

        private boolean collectContent(View view, RectF out) {
            if (view.getVisibility() != View.VISIBLE || view.getWidth() <= 0
                    || view.getHeight() <= 0) return false;
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                RectF children = new RectF();
                boolean found = false;
                for (int index = 0; index < group.getChildCount(); index++) {
                    RectF child = new RectF();
                    if (collectContent(group.getChildAt(index), child)) {
                        if (!found) children.set(child); else children.union(child);
                        found = true;
                    }
                }
                if (found) {
                    out.set(children);
                    return true;
                }
                // Empty container (e.g. notification area with no icons):
                // do not use its layout bounds or the left capsule stretches
                // from the clock to the far edge of a blank slot.
                return false;
            }
            int[] location = new int[2];
            int[] rootLocation = new int[2];
            view.getLocationOnScreen(location);
            root.getLocationOnScreen(rootLocation);
            out.set(
                    location[0] - rootLocation[0],
                    location[1] - rootLocation[1],
                    location[0] - rootLocation[0] + view.getWidth(),
                    location[1] - rootLocation[1] + view.getHeight());
            return true;
        }

        private int blurColor(CapsuleConfig.Values config, float opacity) {
            boolean light = CapsuleConfig.TONE_LIGHT.equals(config.tone)
                    || (CapsuleConfig.TONE_AUTO.equals(config.tone)
                    && (root.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES);
            int channel = light ? 255 : 0;
            int alpha = Math.round(Math.max(0f, Math.min(1f, opacity)) * 255f * .30f);
            return Color.argb(alpha, channel, channel, channel);
        }

        private RectF capsuleBounds(
                RectF content,
                int horizontalAlignment,
                int verticalAlignment,
                float sidePaddingX,
                float sidePaddingY,
                float configuredHeight,
                float sharedPaddingX,
                float sharedInsetY,
                float density) {
            if (content == null || content.isEmpty()) return new RectF();

            float sharedX = dp(sharedPaddingX, density);
            float sharedY = dp(sharedInsetY, density);
            float extraX = dp(sidePaddingX, density);
            float extraY = dp(sidePaddingY, density);
            float horizontalBase = Math.max(0f, sharedX + extraX);
            float verticalMargin = Math.max(0f, sharedY + extraY);
            float alignmentExtra = Math.max(0f, Math.abs(extraX));
            float leftPadding = horizontalBase;
            float rightPadding = horizontalBase;
            if (horizontalAlignment == 0) {
                rightPadding += alignmentExtra;
            } else if (horizontalAlignment == 2) {
                leftPadding += alignmentExtra;
            } else {
                leftPadding += alignmentExtra / 2f;
                rightPadding += alignmentExtra / 2f;
            }

            float width = content.width() + leftPadding + rightPadding;

            // Height is the pill size, independent of content: the clock/icons keep
            // drawing on top and may overflow a shorter capsule. A non-positive
            // configured height falls back to content + vertical margins.
            float requested = dp(configuredHeight, density);
            float height = requested > 0f
                    ? requested
                    : content.height() + verticalMargin * 2f;
            height = Math.max(height, 8f * density);
            if (root.getHeight() > 0) {
                height = Math.min(height, root.getHeight());
            }

            // Vertical placement is relative to the clock/icons row, not the
            // container. A17 nests a ComposeView inside status_bar_container,
            // so root.getHeight() is taller than the visual content and
            // root-center drifts the pill downward.
            float top;
            if (verticalAlignment == 0) {
                top = content.top - verticalMargin;
            } else if (verticalAlignment == 2) {
                top = content.bottom - height + verticalMargin;
            } else {
                top = content.centerY() - height / 2f;
            }
            if (root.getHeight() > 0) {
                top = Math.max(0f, Math.min(top, root.getHeight() - height));
            }

            float contentCenterX = content.centerX();
            float left = contentCenterX - content.width() / 2f - leftPadding;
            return new RectF(left, top, left + width, top + height);
        }

        private static void addUnion(RectF target, RectF candidate) {
            if (candidate == null || candidate.isEmpty()) return;
            if (target.isEmpty()) target.set(candidate); else target.union(candidate);
        }

        private static float dp(float value, float density) { return value * density; }

        @Override protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            if (original != null) original.setBounds(bounds);
        }

        @Override public void setAlpha(int value) {
            alpha = Math.max(0, Math.min(255, value));
            invalidateSelf();
        }

        @Override public int getAlpha() {
            return alpha;
        }

        @Override public void setColorFilter(ColorFilter filter) {
            fill.setColorFilter(filter);
            outline.setColorFilter(filter);
            invalidateSelf();
        }

        /** Drawable#getOpacity is deprecated on newer SDKs; still required for older hosts. */
        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Reflection wrapper for Android's hidden BackgroundBlurDrawable API. */
    private static final class NativeBlur {
        private Drawable drawable;
        private View owner;
        private Method setBlurRadius;
        private Method setCornerRadius;
        private Method setSingleCornerRadius;
        private Method setColor;
        private long lastAttempt;

        boolean draw(Canvas canvas, View view, RectF bounds, float radius, int blurRadius, int color) {
            if (blurRadius <= 0 || bounds == null || bounds.isEmpty()) return false;
            if (!ensure(view)) return false;
            try {
                int left = Math.round(bounds.left);
                int top = Math.round(bounds.top);
                int right = Math.round(bounds.right);
                int bottom = Math.round(bounds.bottom);
                drawable.setBounds(left, top, right, bottom);
                if (setColor != null) setColor.invoke(drawable, color);
                if (setCornerRadius != null) {
                    setCornerRadius.invoke(drawable, radius, radius, radius, radius);
                } else if (setSingleCornerRadius != null) {
                    setSingleCornerRadius.invoke(drawable, radius);
                }
                if (setBlurRadius != null) setBlurRadius.invoke(drawable, blurRadius);
                drawable.setVisible(true, false);
                drawable.draw(canvas);
                return true;
            } catch (Throwable error) {
                clear();
                return false;
            }
        }

        void disable() {
            try {
                if (drawable != null) drawable.setVisible(false, false);
            } catch (Throwable ignored) { }
        }

        private boolean ensure(View view) {
            if (drawable != null && owner == view) return true;
            long now = SystemClock.uptimeMillis();
            if (now - lastAttempt < 400L) return false;
            lastAttempt = now;
            try {
                Method getViewRoot = findMethod(view.getClass(), "getViewRootImpl");
                if (getViewRoot == null) return false;
                Object viewRoot = getViewRoot.invoke(view);
                if (viewRoot == null) return false;
                Method create = findMethod(viewRoot.getClass(), "createBackgroundBlurDrawable");
                if (create == null) return false;
                Object candidate = create.invoke(viewRoot);
                if (!(candidate instanceof Drawable)) return false;
                Drawable created = (Drawable) candidate;
                Method radius = findMethod(created.getClass(), "setBlurRadius", int.class);
                Method corners = findMethod(created.getClass(), "setCornerRadius",
                        float.class, float.class, float.class, float.class);
                Method singleCorner = findMethod(created.getClass(), "setCornerRadius", float.class);
                Method color = findMethod(created.getClass(), "setColor", int.class);
                if (radius == null || color == null
                        || (corners == null && singleCorner == null)) return false;
                created.setCallback(view);
                drawable = created;
                owner = view;
                setBlurRadius = radius;
                setCornerRadius = corners;
                setSingleCornerRadius = singleCorner;
                setColor = color;
                return true;
            } catch (Throwable error) {
                clear();
                return false;
            }
        }

        private void clear() {
            try {
                if (drawable != null) drawable.setCallback(null);
            } catch (Throwable ignored) { }
            drawable = null;
            owner = null;
            setBlurRadius = null;
            setCornerRadius = null;
            setSingleCornerRadius = null;
            setColor = null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Throwable error) {
                return null;
            }
        }
        return null;
    }
}
