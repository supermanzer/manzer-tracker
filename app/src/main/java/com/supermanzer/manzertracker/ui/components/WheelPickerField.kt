package com.supermanzer.manzertracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

// Read-only field that opens a WheelNumberPicker in a dialog. The wheel edits a pending copy,
// so Cancel leaves the value alone and only OK reports the new one.
@Composable
fun WheelPickerField(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dialogTitle: String = label,
    valueLabel: (Int) -> String = { it.toString() }
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = valueLabel(value),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        // A read-only text field swallows taps, so a transparent layer on top catches them.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showPicker = true }
        )
    }

    if (showPicker) {
        var pending by remember { mutableStateOf(value.coerceIn(range)) }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(dialogTitle) },
            text = {
                WheelNumberPicker(
                    initialValue = pending,
                    range = range,
                    onValueChange = { pending = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(pending)
                    showPicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
