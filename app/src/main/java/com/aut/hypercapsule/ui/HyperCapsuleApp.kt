package com.aut.hypercapsule.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aut.hypercapsule.ModuleStatusStore
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.component.BottomDestination
import com.aut.hypercapsule.ui.component.HyperBottomBar
import com.aut.hypercapsule.ui.page.AboutPage
import com.aut.hypercapsule.ui.page.HomePage
import com.aut.hypercapsule.ui.page.SystemUiPage
import com.aut.hypercapsule.ui.page.ThemePage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * App 宿主：扁平化 4 个顶层主 Tab（首页、胶囊设置、主题外观、关于）。
 * 消除过往中间跳板页与二级滑动遮罩，保持导航与操作的极致直接。
 */
@Composable
fun HyperCapsuleApp(preferences: AppPreferences) {
    val destinations = remember {
        listOf(
            BottomDestination(R.string.nav_home, MiuixIcons.Home),
            BottomDestination(R.string.nav_features, MiuixIcons.Tune),
            BottomDestination(R.string.nav_settings, MiuixIcons.Settings),
            BottomDestination(R.string.nav_about, MiuixIcons.Info),
        )
    }
    val pagerState = rememberPagerState(pageCount = { destinations.size })
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var selectedPage by rememberSaveable { mutableIntStateOf(0) }
    var pageAnimationJob by remember { mutableStateOf<Job?>(null) }
    var pageAnimationTarget by remember { mutableStateOf<Int?>(null) }
    var contentReady by remember { mutableStateOf(false) }
    val blurSupported = isRenderEffectSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val glassActive = preferences.floatingNavigationEnabled &&
        preferences.liquidGlassEnabled && blurSupported
    val bottomContentPadding = if (preferences.floatingNavigationEnabled) 122.dp else 96.dp
    val settledPage = pagerState.settledPage

    fun performHaptic() {
        if (preferences.hapticFeedbackEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    fun selectPage(index: Int) {
        if (index == selectedPage && pagerState.settledPage == index) return
        selectedPage = index
        performHaptic()
        pageAnimationJob?.cancel()
        pageAnimationTarget = index
        pageAnimationJob = scope.launch {
            try {
                pagerState.animateScrollToPage(
                    page = index,
                    animationSpec = tween(
                        durationMillis = 320,
                        easing = FastOutSlowInEasing,
                    ),
                )
            } finally {
                if (pageAnimationTarget == index) {
                    pageAnimationTarget = null
                }
            }
        }
    }

    BackHandler(enabled = selectedPage != 0) {
        selectPage(0)
    }

    LaunchedEffect(Unit) {
        contentReady = true
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (pageAnimationTarget == null) {
                    selectedPage = page
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .then(if (glassActive) Modifier.layerBackdrop(backdrop) else Modifier),
            userScrollEnabled = true,
            beyondViewportPageCount = if (contentReady) 1 else 0,
        ) { page ->
            val isCurrent = page == settledPage || page == pagerState.currentPage
            if (!contentReady && !isCurrent) return@HorizontalPager
            when (page) {
                0 -> if (contentReady || isCurrent) {
                    HomePage(
                        moduleStatus = ModuleStatusStore.status,
                        enabledFeatureCount = preferences.enabledFeatureCount,
                        bottomContentPadding = bottomContentPadding,
                    )
                }

                1 -> if (contentReady || isCurrent) {
                    SystemUiPage(
                        preferences = preferences,
                        bottomContentPadding = bottomContentPadding,
                        onHaptic = ::performHaptic,
                    )
                }

                2 -> if (contentReady || isCurrent) {
                    ThemePage(
                        preferences = preferences,
                        bottomContentPadding = bottomContentPadding,
                        onHaptic = ::performHaptic,
                    )
                }

                else -> if (contentReady || isCurrent) {
                    AboutPage(bottomContentPadding = bottomContentPadding)
                }
            }
        }

        // 常驻底部导航栏
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(1f),
        ) {
            HyperBottomBar(
                destinations = destinations,
                selectedIndex = selectedPage,
                floating = preferences.floatingNavigationEnabled,
                liquidGlass = preferences.liquidGlassEnabled,
                backdrop = backdrop.takeIf { blurSupported },
                onSelected = ::selectPage,
            )
        }
    }
}
