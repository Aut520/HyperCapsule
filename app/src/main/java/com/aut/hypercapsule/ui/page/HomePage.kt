package com.aut.hypercapsule.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aut.hypercapsule.BuildConfig
import com.aut.hypercapsule.ModuleStatus
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.component.RootPage
import com.aut.hypercapsule.ui.util.DeviceInfo
import com.aut.hypercapsule.ui.util.DeviceInfoProvider
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomePage(
    moduleStatus: ModuleStatus,
    enabledFeatureCount: Int,
    bottomContentPadding: Dp,
) {
    val context = LocalContext.current
    val deviceInfo = remember(context) { DeviceInfoProvider.read(context) }
    RootPage(
        title = stringResource(R.string.app_name),
        bottomContentPadding = bottomContentPadding,
    ) {
        item {
            StatusGrid(
                status = moduleStatus,
                enabledFeatureCount = enabledFeatureCount,
            )
        }
        if (!moduleStatus.active) {
            item {
                StatusAlertCard(
                    modifier = Modifier.padding(top = 12.dp),
                    title = stringResource(R.string.module_not_activated),
                    message = stringResource(R.string.module_waiting_summary),
                )
            }
        }
        item {
            InfoCard(
                modifier = Modifier.padding(top = 12.dp),
                status = moduleStatus,
                deviceInfo = deviceInfo,
            )
        }
    }
}

@Composable
private fun StatusGrid(
    status: ModuleStatus,
    enabledFeatureCount: Int,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusCard(status, Modifier.weight(1f).height(112.dp))
                StatCard(
                    title = stringResource(R.string.enabled_features),
                    value = enabledFeatureCount.toString(),
                    modifier = Modifier.weight(1f).height(112.dp),
                )
                StatCard(
                    title = stringResource(R.string.api_level),
                    value = "102",
                    modifier = Modifier.weight(1f).height(112.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusCard(
                    status = status,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
                Column(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        title = stringResource(R.string.enabled_features),
                        value = enabledFeatureCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = stringResource(R.string.api_level),
                        value = "102",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(status: ModuleStatus, modifier: Modifier = Modifier) {
    val active = status.active
    val accentColor = if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error
    val backgroundColor = if (active) {
        MiuixTheme.colorScheme.primaryContainer
    } else {
        MiuixTheme.colorScheme.surfaceContainerHigh
    }
    val icon: ImageVector = if (active) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline

    Card(
        modifier = modifier,
        colors = CardDefaults.defaultColors(color = backgroundColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(27.dp, 31.dp)
                    .size(110.dp),
                tint = accentColor.copy(alpha = 0.72f),
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(
                        if (active) R.string.module_activated else R.string.module_not_activated,
                    ),
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.software_version, BuildConfig.VERSION_NAME),
                    modifier = Modifier.padding(top = 2.dp),
                    color = accentColor,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = value,
                modifier = Modifier.padding(top = 2.dp),
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StatusAlertCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.errorContainer,
            contentColor = MiuixTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onErrorContainer,
                style = MiuixTheme.textStyles.headline1,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                color = MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.82f),
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

@Composable
private fun InfoCard(
    status: ModuleStatus,
    deviceInfo: DeviceInfo,
    modifier: Modifier = Modifier,
) {
    val framework = if (status.active) {
        stringResource(
            R.string.framework_details,
            status.frameworkName.ifBlank { "LSPosed" },
            status.frameworkVersion.ifBlank { status.frameworkVersionCode.toString() },
            status.apiVersion,
        )
    } else {
        stringResource(R.string.not_connected)
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            InfoText(stringResource(R.string.system_version), deviceInfo.osVersion)
            InfoText(
                stringResource(R.string.app_version),
                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            )
            InfoText(stringResource(R.string.xposed_framework), framework)
            InfoText(
                stringResource(R.string.device_model),
                deviceInfo.model,
                bottomPadding = 0.dp,
            )
        }
    }
}

@Composable
private fun InfoText(title: String, content: String, bottomPadding: Dp = 24.dp) {
    Text(
        text = title,
        color = MiuixTheme.colorScheme.onSurface,
        style = MiuixTheme.textStyles.headline1,
        fontWeight = FontWeight.Medium,
    )
    Text(
        text = content,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
    )
}
