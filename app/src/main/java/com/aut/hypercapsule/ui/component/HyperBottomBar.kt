package com.aut.hypercapsule.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aut.hypercapsule.ui.component.navigation.FloatingBottomBar
import com.aut.hypercapsule.ui.component.navigation.FloatingBottomBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class BottomDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

/**
 * Mirrors KernelSU BottomBarMiuix: solid NavigationBar, or FloatingBottomBar
 * when floating is on. The floating pill is centered in a full-width slot
 * (KernelSU aligns BottomCenter around IntrinsicSize.Min).
 */
@Composable
fun HyperBottomBar(
    destinations: List<BottomDestination>,
    selectedIndex: Int,
    floating: Boolean,
    liquidGlass: Boolean,
    backdrop: LayerBackdrop?,
    onSelected: (Int) -> Unit,
) {
    val items = destinations.map { destination ->
        NavigationItem(
            label = stringResource(destination.labelRes),
            icon = destination.icon,
        )
    }
    if (!floating || backdrop == null) {
        NavigationBar(
            color = MiuixTheme.colorScheme.surface,
            showDivider = true,
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
        return
    }

    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        .let { inset -> if (inset != 0.dp) 8.dp + inset else 28.dp }
    // KernelSU: full-width slot + align(BottomCenter) on the bar itself.
    // contentAlignment alone is not enough when Scaffold measures wrap-content.
    Box(modifier = Modifier.fillMaxWidth()) {
        FloatingBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Consume taps so the pager underneath does not steal gestures
                // (same as KernelSU BottomBarMiuix).
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            backdrop = backdrop,
            tabsCount = items.size,
            isBlurEnabled = liquidGlass,
        ) { activateTab ->
            items.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    selected = selectedIndex == index,
                    onClick = { activateTab(index) },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        }
    }
}
