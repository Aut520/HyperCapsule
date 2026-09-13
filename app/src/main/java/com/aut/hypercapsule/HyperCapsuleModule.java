package com.aut.hypercapsule;

import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public final class HyperCapsuleModule extends XposedModule {
    private static final String TAG = "HyperCapsule";
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Module loaded in " + param.getProcessName());
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!SYSTEM_UI_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        // Hook installation is intentionally deferred until the analyzed targets are finalized.
        log(Log.INFO, TAG, "SystemUI class loader is ready");
    }
}
