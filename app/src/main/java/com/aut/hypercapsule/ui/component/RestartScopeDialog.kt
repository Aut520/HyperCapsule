package com.aut.hypercapsule.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.aut.hypercapsule.R
import com.aut.hypercapsule.ui.util.RootShell
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 快速重启作用域。对齐 HyperCeiler：默认全不选，必须勾选「系统界面」后才能确认。
 */
@Composable
fun RestartScopeDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val systemUiLabel = stringResource(R.string.system_ui)
    val command = "killall com.android.systemui"
    var systemUiChecked by remember(show) { mutableStateOf(false) }
    var restarting by remember(show) { mutableStateOf(false) }
    var error by remember(show) { mutableStateOf<String?>(null) }
    val rootRequired = stringResource(R.string.restart_root_required)
    val hint = stringResource(R.string.restart_scope_hint)

    WindowDialog(
        show = show,
        title = stringResource(R.string.restart_scope),
        summary = stringResource(R.string.restart_scope_summary),
        onDismissRequest = { if (!restarting) onDismiss() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ScopeChoiceRow(
                label = systemUiLabel,
                checked = systemUiChecked,
                enabled = !restarting,
                onClick = { systemUiChecked = !systemUiChecked },
            )
            Text(
                text = hint,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        error?.let {
            Text(
                text = it,
                color = RestartErrorColor,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(R.string.cancel),
                enabled = !restarting,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.confirm),
                enabled = !restarting && systemUiChecked,
                onClick = {
                    if (!systemUiChecked) return@TextButton
                    restarting = true
                    error = null
                    scope.launch {
                        RootShell.run(command)
                            .onSuccess { onDismiss() }
                            .onFailure { error = rootRequired }
                        restarting = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun ScopeChoiceRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.headline1,
        )
        Spacer(Modifier.width(12.dp))
        Checkbox(
            state = ToggleableState(checked),
            onClick = onClick,
            enabled = enabled,
        )
    }
}

private val RestartErrorColor = Color(0xFFFF5A52)
