package com.aut.hypercapsule.ui.page

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.aut.hypercapsule.BuildConfig
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.component.effect.BgEffectBackground
import com.aut.hypercapsule.ui.util.DeviceInfo
import com.aut.hypercapsule.ui.util.DeviceInfoProvider
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun AboutPage(bottomContentPadding: Dp) {
    val context = LocalContext.current
    val deviceInfo = remember(context) { DeviceInfoProvider.read(context) }
    val backdrop = rememberLayerBackdrop()
    val blurActive = isRuntimeShaderSupported()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    BgEffectBackground(
        dynamicBackground = true,
        modifier = Modifier.fillMaxSize(),
        bgModifier = Modifier.layerBackdrop(backdrop),
        isFullSize = true,
        isOs3Effect = true,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = bottomContentPadding + 12.dp,
            ),
            overscrollEffect = null,
        ) {
            item {
                AboutHero(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarPadding + 448.dp)
                        .padding(top = statusBarPadding + 32.dp),
                    backdrop = backdrop,
                    blurActive = blurActive,
                )
            }
            item {
                DeviceInfoCard(
                    info = deviceInfo,
                    backdrop = backdrop,
                    blurActive = blurActive,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.author),
                    modifier = Modifier.padding(start = 12.dp, top = 22.dp, bottom = 8.dp),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
            item {
                AuthorCard(
                    context = context,
                    backdrop = backdrop,
                    blurActive = blurActive,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.projects_and_references),
                    modifier = Modifier.padding(start = 12.dp, top = 22.dp, bottom = 8.dp),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
            item {
                ReferenceCard(
                    context = context,
                    backdrop = backdrop,
                    blurActive = blurActive,
                )
            }
        }
    }
}

@Composable
private fun AboutHero(
    modifier: Modifier,
    backdrop: LayerBackdrop,
    blurActive: Boolean,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val logoShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .size(108.dp)
                .then(
                    if (blurActive) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = logoShape,
                            blurRadius = 46f,
                            colors = glassColors(alpha = 0.36f),
                        )
                    } else {
                        Modifier.background(
                            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.68f),
                            logoShape,
                        )
                    },
                )
                .clip(logoShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_capsule_logo),
                contentDescription = stringResource(R.string.app_icon_description),
                modifier = Modifier.size(92.dp),
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(top = 18.dp),
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(
                R.string.about_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            modifier = Modifier.padding(top = 8.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
private fun DeviceInfoCard(
    info: DeviceInfo,
    backdrop: LayerBackdrop,
    blurActive: Boolean,
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassBackground(backdrop, shape, blurActive),
        cornerRadius = 20.dp,
        insideMargin = PaddingValues(20.dp),
        colors = glassCardColors(blurActive),
    ) {
        Text(
            text = stringResource(R.string.device_owner_title, info.name),
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(22.dp))
        DeviceValue(stringResource(R.string.device_model), info.model)
        DeviceValue(stringResource(R.string.android_version), info.androidVersion)
        DeviceValue(
            label = stringResource(R.string.os_version),
            value = info.osVersion,
            bottomPadding = 0.dp,
        )
    }
}

@Composable
private fun DeviceValue(label: String, value: String, bottomPadding: Dp = 18.dp) {
    Text(
        text = value,
        color = MiuixTheme.colorScheme.onSurface,
        style = MiuixTheme.textStyles.title4,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = label,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.footnote1,
    )
}

@Composable
private fun AuthorCard(
    context: Context,
    backdrop: LayerBackdrop,
    blurActive: Boolean,
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassBackground(backdrop, shape, blurActive),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(14.dp),
        colors = glassCardColors(blurActive),
        pressFeedbackType = PressFeedbackType.Tilt,
        showIndication = true,
        onClick = { context.openUrl(AUTHOR_URL) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.avatar_aut520),
                contentDescription = stringResource(R.string.author_avatar_description),
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = stringResource(R.string.author_name),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.author_handle),
                    modifier = Modifier.padding(top = 2.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}

@Composable
private fun ReferenceCard(
    context: Context,
    backdrop: LayerBackdrop,
    blurActive: Boolean,
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassBackground(backdrop, shape, blurActive),
        cornerRadius = 18.dp,
        colors = glassCardColors(blurActive),
    ) {
        REFERENCE_PROJECTS.forEach { reference ->
            ArrowPreference(
                title = reference.name,
                summary = stringResource(reference.summaryRes),
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Link,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                },
                onClick = { context.openUrl(reference.url) },
            )
        }
    }
}

@Composable
private fun Modifier.glassBackground(
    backdrop: LayerBackdrop,
    shape: RoundedCornerShape,
    blurActive: Boolean,
): Modifier = if (blurActive) {
    textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = 58f,
        colors = glassColors(alpha = 0.50f),
    )
} else {
    this
}

@Composable
private fun glassColors(alpha: Float): BlurColors = BlurColors(
    blendColors = listOf(
        BlendColorEntry(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = alpha)),
    ),
)

@Composable
private fun glassCardColors(blurActive: Boolean) = CardDefaults.defaultColors(
    color = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
    contentColor = MiuixTheme.colorScheme.onSurface,
)

private data class ReferenceProject(
    val name: String,
    val summaryRes: Int,
    val url: String,
)

private val REFERENCE_PROJECTS = listOf(
    ReferenceProject("HyperIsland", R.string.reference_hyperisland, "https://github.com/1812z/HyperIsland"),
    ReferenceProject("KernelSU", R.string.reference_kernelsu, "https://github.com/tiann/KernelSU"),
    ReferenceProject("HyperCeiler", R.string.reference_hyperceiler, "https://github.com/ReChronoRain/HyperCeiler"),
    ReferenceProject("libxposed API", R.string.reference_libxposed, "https://github.com/libxposed/api"),
    ReferenceProject("compose-miuix-ui", R.string.reference_miuix, "https://github.com/compose-miuix-ui/miuix"),
)

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private const val AUTHOR_URL = "https://github.com/Aut520/"
