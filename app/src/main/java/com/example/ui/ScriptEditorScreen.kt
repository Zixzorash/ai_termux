package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TermuxScriptFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    script: TermuxScriptFile?,
    initialContent: String,
    onSave: (name: String, content: String, isNewFile: Boolean) -> Unit,
    onRun: ((TermuxScriptFile) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNewFile = script == null

    var filename by remember {
        mutableStateOf(script?.name ?: "my_script.sh")
    }
    var content by remember {
        mutableStateOf(
            if (isNewFile && initialContent.isBlank()) {
                "#!/data/data/com.termux/files/usr/bin/bash\n\necho \"Hello from Termux\"\n"
            } else {
                initialContent
            }
        )
    }

    var showSaveAsDialog by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isNewFile) "New Script" else "Edit: ${script?.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (isNewFile) "Create in Termux storage" else (script?.logicalPosixPath ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "กลับ"
                        )
                    }
                },
                actions = {
                    if (!isNewFile && onRun != null && script != null) {
                        IconButton(
                            onClick = { onRun(script) },
                            modifier = Modifier.testTag("editor_run_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "รัน",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (!isNewFile) {
                        IconButton(
                            onClick = { showSaveAsDialog = true },
                            modifier = Modifier.testTag("editor_save_as_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAs,
                                contentDescription = "Save As"
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanName = if (filename.endsWith(".sh", ignoreCase = true)) filename else "$filename.sh"
                            if (cleanName.isBlank()) {
                                Toast.makeText(context, "กรุณาระบุชื่อไฟล์", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSave(cleanName.trim(), content, isNewFile)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("editor_save_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filename input row (if new file or editing)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ไฟล์:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = filename,
                        onValueChange = { filename = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editor_filename_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        ),
                        placeholder = { Text("script.sh") }
                    )
                }
            }

            // Quick Shell Snippets Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {
                        if (!content.startsWith("#!/data/data/com.termux/files/usr/bin/bash")) {
                            content = "#!/data/data/com.termux/files/usr/bin/bash\n$content"
                        }
                    },
                    label = { Text("#!/bin/bash", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF38BDF8))
                )

                AssistChip(
                    onClick = { content += "\necho \"\"" },
                    label = { Text("echo", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF4ADE80))
                )

                AssistChip(
                    onClick = { content += "\${}" },
                    label = { Text("\${VAR}", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFFBBF24))
                )

                AssistChip(
                    onClick = { content += "\nif [  ]; then\n  \nfi" },
                    label = { Text("if ... fi", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF93C5FD))
                )

                AssistChip(
                    onClick = { content += " | " },
                    label = { Text("pipe |", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFCBD5E1))
                )

                AssistChip(
                    onClick = { content += " && " },
                    label = { Text("&&", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFCBD5E1))
                )

                AssistChip(
                    onClick = { content += "\npkg update -y && pkg upgrade -y\n" },
                    label = { Text("pkg update", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF34D399))
                )
            }

            // Monospace Script Editor Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0A0F1D))
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .testTag("editor_content_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFFE2E8F0)
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0A0F1D),
                        unfocusedContainerColor = Color(0xFF0A0F1D),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            // Footer Status Bar
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lineCount = content.count { it == '\n' } + 1
                    Text(
                        text = "บรรทัด: $lineCount | ตัวอักษร: ${content.length} | UTF-8",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "Android 16 SAF Mode",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showSaveAsDialog) {
        var saveAsName by remember { mutableStateOf("${filename.removeSuffix(".sh")}_copy.sh") }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("บันทึกเป็นชื่อใหม่ (Save As)") },
            text = {
                Column {
                    Text("กรุณาระบุชื่อไฟล์ .sh ใหม่สำหรับบันทึกสำเนา:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveAsName,
                        onValueChange = { saveAsName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = if (saveAsName.endsWith(".sh", ignoreCase = true)) saveAsName else "$saveAsName.sh"
                        showSaveAsDialog = false
                        onSave(cleanName.trim(), content, true)
                    }
                ) {
                    Text("บันทึกสำเนา")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSaveAsDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}
