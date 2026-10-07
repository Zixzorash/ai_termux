package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TermuxScriptFile

@Composable
fun RenameFileDialog(
    script: TermuxScriptFile,
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(script.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("เปลี่ยนชื่อไฟล์ (Rename)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("ระบุชื่อใหม่สำหรับไฟล์ shell script:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        errorMessage = null
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { Text(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = newName.trim()
                    if (clean.isBlank()) {
                        errorMessage = "กรุณาระบุชื่อไฟล์"
                        return@Button
                    }
                    if (clean.contains("/") || clean.contains("\\") || clean.contains("..")) {
                        errorMessage = "ชื่อไฟล์มีอักขระต้องห้าม"
                        return@Button
                    }
                    val finalName = if (clean.endsWith(".sh", ignoreCase = true)) clean else "$clean.sh"
                    onConfirm(finalName)
                },
                modifier = Modifier.testTag("rename_confirm_btn")
            ) {
                Text("เปลี่ยนชื่อ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
