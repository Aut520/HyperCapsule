package com.aut.hypercapsule.ui.page

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.component.RootPage
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FeaturesPage(
    preferences: AppPreferences,
    bottomContentPadding: Dp,
    onOpenSystemUi: () -> Unit,
) {
    RootPage(
        title = stringResource(R.string.nav_features),
        bottomContentPadding = bottomContentPadding,
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                ArrowPreference(
                    title = stringResource(R.string.system_ui),
                    summary = stringResource(
                        R.string.enabled_feature_count,
                        preferences.enabledFeatureCount,
                    ) + " · " + stringResource(R.string.system_ui_summary),
                    startAction = {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    onClick = onOpenSystemUi,
                )
            }
        }
    }
}
