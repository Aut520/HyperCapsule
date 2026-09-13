package com.aut.hypercapsule.ui.page

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.component.RootPage
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    preferences: AppPreferences,
    bottomContentPadding: Dp,
    onHaptic: () -> Unit,
    onOpenTheme: () -> Unit,
) {
    RootPage(
        title = stringResource(R.string.nav_settings),
        bottomContentPadding = bottomContentPadding,
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                BasicComponent(
                    title = stringResource(R.string.theme_page_title),
                    summary = stringResource(R.string.theme_page_summary),
                    onClick = {
                        onHaptic()
                        onOpenTheme()
                    },
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Theme,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                )
            }
        }
    }
}
