package com.m57.hermescontrol.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.m57.hermescontrol.BuildConfig
import com.m57.hermescontrol.R
import com.m57.hermescontrol.applock.AlertDialog
import com.m57.hermescontrol.ui.chat.ChatViewModel

@Composable
internal fun ExtensionConsent(
    viewModel: ChatViewModel,
    conversationTitle: String,
) {
    DisposableEffect(viewModel) {
        ExtensionBroker.attach(viewModel)
        onDispose { ExtensionBroker.detach(viewModel) }
    }
    val requested by ExtensionBroker.consent.collectAsState()
    if (requested) {
        var draft by remember { mutableStateOf(false) }
        var display by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = ExtensionBroker::revoke,
            title = { Text(stringResource(R.string.extension_consent_title)) },
            text = {
                Column {
                    Text(ExtensionBroker.TEST_PACKAGE)
                    Text(BuildConfig.EXTENSION_TEST_SIGNER)
                    Text(conversationTitle)
                    Text(stringResource(R.string.extension_consent_duration))
                    Row {
                        Checkbox(checked = draft, onCheckedChange = { draft = it })
                        Text(stringResource(R.string.extension_draft_permission))
                    }
                    Row {
                        Checkbox(checked = display, onCheckedChange = { display = it })
                        Text(stringResource(R.string.extension_display_permission))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { ExtensionBroker.approve(draft, display) }, enabled = draft || display) {
                    Text(stringResource(R.string.extension_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = ExtensionBroker::revoke) { Text(stringResource(R.string.extension_deny)) }
            },
        )
    }
}

@Composable
internal fun ExtensionRevokeAction() {
    val enabled by ExtensionBroker.enabled.collectAsState()
    if (enabled) {
        TextButton(onClick = ExtensionBroker::revoke) { Text(stringResource(R.string.extension_revoke)) }
    }
}
