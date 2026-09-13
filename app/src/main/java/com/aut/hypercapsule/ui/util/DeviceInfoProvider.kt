package com.aut.hypercapsule.ui.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.concurrent.TimeUnit

/**
 * Reads the same user-facing device values used by HyperCeiler's about card.
 * System properties are read reflectively first, with a small getprop fallback
 * for vendor builds that hide the reflection entry point.
 */
data class DeviceInfo(
    val name: String,
    val model: String,
    val androidVersion: String,
    val osVersion: String,
)

object DeviceInfoProvider {
    fun read(context: Context): DeviceInfo {
        val model = firstValid(
            property("ro.product.marketname"),
            Build.MODEL,
            Build.DEVICE,
            fallback = Build.MODEL,
        )
        val rawName = if (Build.VERSION.SDK_INT >= 36) {
            firstValid(
                property("persist.private.device_name"),
                property("persist.sys.device_name"),
                miuiDeviceName(context),
                Settings.Global.getString(context.contentResolver, "device_name"),
                model,
                fallback = model,
            )
        } else {
            firstValid(
                property("persist.sys.device_name"),
                property("persist.private.device_name"),
                miuiDeviceName(context),
                Settings.Global.getString(context.contentResolver, "device_name"),
                model,
                fallback = model,
            )
        }
        val name = formatDeviceName(rawName, model)
        val androidVersion = firstValid(
            property("ro.build.version.release"),
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT.toString(),
            fallback = Build.VERSION.SDK_INT.toString(),
        )
        val osVersion = firstValid(
            property("ro.mi.os.version.incremental"),
            property("ro.build.version.incremental"),
            Build.VERSION.INCREMENTAL,
            Build.DISPLAY,
            fallback = Build.DISPLAY,
        )
        return DeviceInfo(
            name = name,
            model = model,
            androidVersion = androidVersion,
            osVersion = osVersion,
        )
    }

    private fun miuiDeviceName(context: Context): String? = runCatching {
        val clazz = Class.forName("android.provider.MiuiSettings\$System")
        clazz.getMethod("getDeviceName", Context::class.java)
            .invoke(null, context) as? String
    }.getOrNull().takeIfValid()

    private fun property(key: String): String {
        val reflected = runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            method.invoke(null, key, "") as? String
        }.getOrNull().orEmpty().trim()
        if (reflected.isNotEmpty()) return reflected

        return runCatching {
            val process = ProcessBuilder("/system/bin/getprop", key)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroy()
                return@runCatching ""
            }
            process.inputStream.bufferedReader().use { it.readText().trim() }
        }.getOrDefault("")
    }

    private fun formatDeviceName(name: String, model: String): String {
        val cleanName = name.takeIfValid() ?: model
        val cleanModel = model.takeIfValid() ?: return cleanName
        if (cleanName.equals(cleanModel, ignoreCase = true)) return cleanModel

        // HyperOS may store the owner prefix and market name without a
        // separator (for example, "空城:)的REDMI Note 11T Pro+"). Keep the
        // user-selected prefix while making the displayed title readable.
        val suffixStart = cleanName.lastIndexOf(cleanModel, ignoreCase = true)
        if (suffixStart >= 0 && suffixStart + cleanModel.length == cleanName.length) {
            val prefix = cleanName.substring(0, suffixStart).trimEnd()
            if (prefix.isNotEmpty()) return "$prefix $cleanModel"
        }
        return cleanName
    }

    private fun firstValid(vararg values: String?, fallback: String): String =
        values.firstNotNullOfOrNull { it.takeIfValid() } ?: fallback

    private fun String?.takeIfValid(): String? = this
        ?.trim()
        ?.takeIf {
            it.isNotEmpty() &&
                !it.equals("null", ignoreCase = true) &&
                !it.equals("unknown", ignoreCase = true) &&
                !it.equals(Build.UNKNOWN, ignoreCase = true)
        }
}
