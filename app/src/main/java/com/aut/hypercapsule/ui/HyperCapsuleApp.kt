package com.aut.hypercapsule.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import com.aut.hypercapsule.ui.page.FeaturesPage
import com.aut.hypercapsule.ui.page.HomePage
import com.aut.hypercapsule.ui.page.SettingsPage
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
 * App shell: pages fill the full window so each page's own background
 * (surface or BgEffect) extends under the floating bottom bar. The bar is an
 * overlay — Scaffold bottomBar would inset the pager and paint a solid strip
 * under the pill.
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
    var systemUiDetailVisible by rememberSaveable { mutableStateOf(false) }
    var themeDetailVisible by rememberSaveable { mutableStateOf(false) }
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
    val detailsVisible = systemUiDetailVisible || themeDetailVisible

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

    fun closeSystemUiDetail() {
        if (!systemUiDetailVisible) return
        performHaptic()
        systemUiDetailVisible = false
    }

    fun closeThemeDetail() {
        if (!themeDetailVisible) return
        performHaptic()
        themeDetailVisible = false
    }

    BackHandler(enabled = detailsVisible) {
        if (themeDetailVisible) closeThemeDetail() else closeSystemUiDetail()
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
            userScrollEnabled = !detailsVisible,
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
                    FeaturesPage(
                        preferences = preferences,
                        bottomContentPadding = bottomContentPadding,
                        onOpenSystemUi = {
                            performHaptic()
                            systemUiDetailVisible = true
                        },
                    )
                }

                2 -> if (contentReady || isCurrent) {
                    SettingsPage(
                        preferences = preferences,
                        bottomContentPadding = bottomContentPadding,
                        onHaptic = ::performHaptic,
                        onOpenTheme = {
                            performHaptic()
                            themeDetailVisible = true
                        },
                    )
                }

                else -> if (contentReady || isCurrent) {
                    AboutPage(bottomContentPadding = bottomContentPadding)
                }
            }
        }

        // Overlay bottom bar — do not inset the pager; pages paint under the pill.
        AnimatedVisibility(
            visible = !detailsVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(1f),
            enter = slideInVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it },
            ) + fadeIn(),
            exit = slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                targetOffsetY = { it },
            ) + fadeOut(),
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

        AnimatedVisibility(
            visible = systemUiDetailVisible,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
            enter = slideInHorizontally(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                targetOffsetX = { it },
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaceColor),
            ) {
                SystemUiPage(
                    preferences = preferences,
                    onBack = ::closeSystemUiDetail,
                    onHaptic = ::performHaptic,
                )
            }
        }

        AnimatedVisibility(
            visible = themeDetailVisible,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
            enter = slideInHorizontally(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                targetOffsetX = { it },
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaceColor),
            ) {
                ThemePage(
                    preferences = preferences,
                    onBack = ::closeThemeDetail,
                    onHaptic = ::performHaptic,
                )
            }
        }
    }
}
