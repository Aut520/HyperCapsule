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
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
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

    private static final Object LOCK = new Object();
    private static final Map<View, Drawable> ORIGINALS = new WeakHashMap<>();
    private static final Map<View, Integer> MODES = new WeakHashMap<>();

    private static boolean transitionHookInstalled;
    private static boolean applicationHookInstalled;
    private static boolean islandHookInstalled;
    private static boolean preferenceListenerInstalled;
    private static Context systemUiContext;
    private static SharedPreferences remotePreferences;
    private static Thread.UncaughtExceptionHandler previousExceptionHandler;

    private static final SharedPreferences.OnSharedPreferenceChangeListener PREFERENCE_LISTENER =
            (preferences, key) -> refreshAll();

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
            String className = Build.VERSION.SDK_INT == SupportedPlatform.ANDROID_15_SDK
                    ? A15_TRANSITIONS : A17_TRANSITIONS;
            Class<?> transitions = Class.forName(className, false, param.getClassLoader());
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
            installLandscapeIslandHook(param.getClassLoader());
            installCrashGuard();
            log(Log.INFO, TAG, "Installed " + className + ".applyModeBackground");
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "SystemUI hook installation failed", error);
        }
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

    private void installLandscapeIslandHook(ClassLoader loader) {
        if (islandHookInstalled || Build.VERSION.SDK_INT != SupportedPlatform.ANDROID_17_SDK) {
            return;
        }
        try {
            Class<?> controller = Class.forName(A17_ISLAND_CONTROLLER, false, loader);
            Method callback = controller.getDeclaredMethod(
                    "onIslandCountChanged", String.class, int.class, boolean.class);
            callback.setAccessible(true);
            hook(callback)
                    .setId("hypercapsule.hideLandscapeIsland")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        SharedPreferences preferences = preferences();
                        if (preferences.getBoolean(CapsuleConfig.HIDE_ISLAND, false)
                                && isLandscape()) {
                            Object[] args = chain.getArgs().toArray();
                            args[2] = false;
                            return chain.proceed(args);
                        }
                        return chain.proceed();
                    });
            islandHookInstalled = true;
            log(Log.INFO, TAG, "Installed Android 17 landscape island hook");
        } catch (ClassNotFoundException ignored) {
            log(Log.INFO, TAG, "Android 15/other sample has no island controller");
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Landscape island hook unavailable", error);
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
        int mode = (Integer) modeValue;
        synchronized (LOCK) {
            MODES.put(root, mode);
            captureOriginal(root);
        }
        updateRoot(root, mode, preferences);
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
        } catch (Throwable error) {
            Context context = systemUiContext;
            if (context != null) {
                remotePreferences = context.getSharedPreferences(
                        CapsuleConfig.PREFS, Context.MODE_PRIVATE);
            } else {
                throw error;
            }
        }
        return remotePreferences;
    }

    private static void registerPreferenceListener(SharedPreferences preferences) {
        if (preferenceListenerInstalled) return;
        synchronized (LOCK) {
            if (preferenceListenerInstalled) return;
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
        if (preferences.getBoolean(CapsuleConfig.SAFE_MODE, false)) return true;
        try {
            android.os.Bundle state = SafeModeProvider.read(root.getContext());
            return state != null && state.getBoolean(SafeModeProvider.KEY_SAFE_MODE, false);
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
                if (root != null && mode != null) {
                    root.post(() -> {
                        try {
                            updateRoot(root, mode, remotePreferences);
                        } catch (Throwable ignored) { }
                    });
                }
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
                CapsuleConfig.Values config = CapsuleConfig.read(preferences);
                if (config.safeMode || isLocked(root)
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
                if (view.getWidth() > root.getWidth() * .8f) return false;
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

        @Override public void setColorFilter(ColorFilter filter) {
            fill.setColorFilter(filter);
            outline.setColorFilter(filter);
            invalidateSelf();
        }

        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
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
