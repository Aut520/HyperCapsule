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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.aut.hypercapsule.ModuleStatusStore
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.component.BottomDestination
import com.aut.hypercapsule.ui.component.HyperBottomBar
import com.aut.hypercapsule.ui.page.AboutPage
import com.aut.hypercapsule.ui.page.FeaturesPage
import com.aut.hypercapsule.ui.page.HomePage
import com.aut.hypercapsule.ui.page.SettingsPage
import com.aut.hypercapsule.ui.page.SystemUiPage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HyperCapsuleApp(preferences: AppPreferences) {
    val destinations = remember {
        listOf(
            BottomDestination(R.string.nav_home, Icons.Rounded.Cottage),
            BottomDestination(R.string.nav_features, Icons.Rounded.Tune),
            BottomDestination(R.string.nav_settings, Icons.Rounded.Settings),
            BottomDestination(R.string.nav_about, Icons.Rounded.Info),
        )
    }
    val pagerState = rememberPagerState(pageCount = { destinations.size })
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var selectedPage by rememberSaveable { mutableIntStateOf(0) }
    var pageAnimationJob by remember { mutableStateOf<Job?>(null) }
    var pageAnimationTarget by remember { mutableStateOf<Int?>(null) }
    var systemUiDetailVisible by rememberSaveable { mutableStateOf(false) }
    val blurSupported = isRenderEffectSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val glassActive = preferences.floatingNavigationEnabled &&
        preferences.liquidGlassEnabled && blurSupported
    val bottomContentPadding = if (preferences.floatingNavigationEnabled) 122.dp else 96.dp

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

    BackHandler(enabled = systemUiDetailVisible, onBack = ::closeSystemUiDetail)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
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
            userScrollEnabled = !systemUiDetailVisible,
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> HomePage(
                    moduleStatus = ModuleStatusStore.status,
                    enabledFeatureCount = preferences.enabledFeatureCount,
                    bottomContentPadding = bottomContentPadding,
                )

                1 -> FeaturesPage(
                    preferences = preferences,
                    bottomContentPadding = bottomContentPadding,
                    onOpenSystemUi = {
                        performHaptic()
                        systemUiDetailVisible = true
                    },
                )

                2 -> SettingsPage(
                    preferences = preferences,
                    bottomContentPadding = bottomContentPadding,
                    onHaptic = ::performHaptic,
                )

                else -> AboutPage(bottomContentPadding = bottomContentPadding)
            }
        }

        AnimatedVisibility(
            visible = !systemUiDetailVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
    }
}
