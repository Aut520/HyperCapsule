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
    val platformSupported: Boolean = false,
    val platformLabel: String = "",
)

object ModuleStatusStore {
    var status by mutableStateOf(ModuleStatus())
        private set

    internal fun update(value: ModuleStatus) {
        status = value
    }
}

object RemotePreferencesBridge {
    @Volatile private var preferences: android.content.SharedPreferences? = null
    @Volatile private var context: android.content.Context? = null
    fun initialize(value: android.content.Context) { context = value.applicationContext }
    fun attach(service: XposedService) {
        runCatching {
            preferences = service.getRemotePreferences(com.aut.hypercapsule.ui.AppPreferences.FILE_NAME)
            val local = context?.getSharedPreferences(com.aut.hypercapsule.ui.AppPreferences.FILE_NAME, android.content.Context.MODE_PRIVATE)
            val remote = preferences ?: return@runCatching
            local?.all?.forEach { (key, value) ->
                when (value) {
                    is Boolean -> remote.edit()?.putBoolean(key, value)?.apply()
                    is Int -> remote.edit()?.putInt(key, value)?.apply()
                    is Float -> remote.edit()?.putFloat(key, value)?.apply()
                    is String -> remote.edit()?.putString(key, value)?.apply()
                }
            }
        }
    }
    fun putBoolean(key: String, value: Boolean) {
        runCatching { preferences?.edit()?.putBoolean(key, value)?.apply() }
    }
    fun putString(key: String, value: String) { runCatching { preferences?.edit()?.putString(key, value)?.apply() } }
    fun putFloat(key: String, value: Float) { runCatching { preferences?.edit()?.putFloat(key, value)?.apply() } }
    fun putInt(key: String, value: Int) { runCatching { preferences?.edit()?.putInt(key, value)?.apply() } }
    fun putLong(key: String, value: Long) { runCatching { preferences?.edit()?.putLong(key, value)?.apply() } }
}

class HyperCapsuleApplication : Application(), XposedServiceHelper.OnServiceListener {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        RemotePreferencesBridge.initialize(this)
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        RemotePreferencesBridge.attach(service)
        val status = runCatching {
            ModuleStatus(
                active = true,
                frameworkName = service.frameworkName,
                frameworkVersion = service.frameworkVersion,
                frameworkVersionCode = service.frameworkVersionCode,
                apiVersion = service.apiVersion,
                scope = service.scope.toList(),
                platformSupported = SupportedPlatform.isSupported(),
                platformLabel = SupportedPlatform.platformLabel(),
            )
        }.getOrElse { ModuleStatus(active = true) }

        mainHandler.post { ModuleStatusStore.update(status) }
    }

    override fun onServiceDied(service: XposedService) {
        mainHandler.post { ModuleStatusStore.update(ModuleStatus()) }
    }
}
