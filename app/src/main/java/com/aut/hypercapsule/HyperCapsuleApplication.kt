package com.aut.hypercapsule

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

data class ModuleStatus(
    val active: Boolean = false,
    val frameworkName: String = "",
    val frameworkVersion: String = "",
    val frameworkVersionCode: Long = 0,
    val apiVersion: Int = 0,
    val scope: List<String> = emptyList(),
)

object ModuleStatusStore {
    var status by mutableStateOf(ModuleStatus())
        private set

    internal fun update(value: ModuleStatus) {
        status = value
    }
}

class HyperCapsuleApplication : Application(), XposedServiceHelper.OnServiceListener {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        val status = runCatching {
            ModuleStatus(
                active = true,
                frameworkName = service.frameworkName,
                frameworkVersion = service.frameworkVersion,
                frameworkVersionCode = service.frameworkVersionCode,
                apiVersion = service.apiVersion,
                scope = service.scope.toList(),
            )
        }.getOrElse { ModuleStatus(active = true) }

        mainHandler.post { ModuleStatusStore.update(status) }
    }

    override fun onServiceDied(service: XposedService) {
        mainHandler.post { ModuleStatusStore.update(ModuleStatus()) }
    }
}
