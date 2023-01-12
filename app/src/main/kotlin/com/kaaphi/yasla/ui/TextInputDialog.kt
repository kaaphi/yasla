package com.kaaphi.yasla.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputDialog(
    modifier: Modifier = Modifier,
    title: String = "Enter Text",
    initialValue: String = "",
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val text = remember { mutableStateOf(
        TextFieldValue(
        text = initialValue,
        selection = TextRange(0, initialValue.length)
    )
    ) }
    val focusRequester = remember { FocusRequester() }

    val onConfirmAction = {onConfirm.invoke(text.value.text.ifBlank { null })}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            TextField(
                value = text.value,
                onValueChange = { text.value = it },
                modifier = Modifier.focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions {
                    onConfirmAction()
                }
            )
            LaunchedEffect(focusRequester) {
                delay(100) //for bug https://issuetracker.google.com/issues/204502668
                focusRequester.requestFocus()
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirmAction()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TextInputDialogHost(
    state: TextInputDialogState
) {
    val currentState : TextInputDialogData? by state.state.collectAsState()

    currentState?.let {
        TextInputDialog(
            title = it.title,
            initialValue = it.initialValue,
            onConfirm = it.onConfirm,
            onDismiss = it.onDismiss
        )
    }
}

internal class TextInputDialogData (
    val title: String = "Enter Text",
    val initialValue: String = "",
    val onConfirm: (String?) -> Unit = {},
    val onDismiss: () -> Unit = {}
)

class TextInputDialogState {
    private val mutex = Mutex()
    internal val state = MutableStateFlow<TextInputDialogData?>(null)

    suspend fun showDialog(
        title: String = "Enter Text",
        initialValue: String = "",
        onConfirm: (String?) -> Unit = {},
        onDismiss: () -> Unit = {}
    ): Unit = mutex.withLock {
        try {
            suspendCoroutine { continuation ->
                state.value = TextInputDialogData(
                    title,
                    initialValue,
                    onConfirm = {
                        onConfirm(it)
                        continuation.resume(Unit)
                    }
                ) {
                    onDismiss()
                    continuation.resume(Unit)
                }
            }
        } finally {
            state.value = null
        }
    }
}