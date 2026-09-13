package com.aut.hypercapsule.ui.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.aut.hypercapsule.ui.component.navigation.IosLiquidGlassNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class BottomDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

@Composable
fun HyperBottomBar(
    destinations: List<BottomDestination>,
    selectedIndex: Int,
    floating: Boolean,
    liquidGlass: Boolean,
    backdrop: LayerBackdrop?,
    onSelected: (Int) -> Unit,
) {
    if (!floating) {
        NavigationBar(
            color = MiuixTheme.colorScheme.surface,
            showDivider = true,
        ) {
            destinations.forEachIndexed { index, destination ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    icon = destination.icon,
                    label = stringResource(destination.labelRes),
                )
            }
        }
        return
    }

    IosLiquidGlassNavigationBar(
        items = destinations.map { destination ->
            NavigationItem(
                label = stringResource(destination.labelRes),
                icon = destination.icon,
            )
        },
        selectedIndex = selectedIndex,
        onItemClick = onSelected,
        backdrop = backdrop,
        isBlurActive = liquidGlass && backdrop != null,
    )
}
