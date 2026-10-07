package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TermuxScriptFile
import com.example.termux.ScriptRunState
import com.example.termux.TermuxCommandManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunResultBottomSheet(
    state: ScriptRunState,
    onDismiss: () -> Unit,
    onSendInput: (String) -> Unit,
    onStopExecution: () -> Unit,
    onRerunInApp: ((TermuxScriptFile) -> Unit)? = null,
    onRerunViaTermux: ((TermuxScriptFile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }

    // Auto-scroll on new log lines
    LaunchedEffect(state.outputLogs.size) {
        if (state.outputLogs.isNotEmpty()) {
            listState.animateScrollToItem(state.outputLogs.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("run_result_bottom_sheet"),
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (state.scriptName.isNotBlank()) "Running: ${state.scriptName}" else "Termux Script Runner",
                            color = Color(0xFFF1F5F9),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.logicalPath.ifBlank { "~/.shortcuts/" },
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isRunning) {
                        IconButton(
                            onClick = onStopExecution,
                            modifier = Modifier.testTag("stop_execution_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "หยุดทำงาน", tint = Color(0xFFEF4444))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF94A3B8))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (state.isRunning) Color(0xFF0284C7).copy(alpha = 0.25f)
                else if (state.exitCode == 0) Color(0xFF10B981).copy(alpha = 0.25f)
                else Color(0xFFEF4444).copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isRunning) Color(0xFF38BDF8)
                                    else if (state.exitCode == 0) Color(0xFF4ADE80)
                                    else Color(0xFFF87171)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isRunning) {
                                if (state.isWaitingForInput) "รอคำตอบจากคุณ (พิมพ์ชื่อไฟล์/คำตอบด้านล่างได้ทันที)..." else "กำลังรันสคริปต์ (พร้อมรับคำตอบ)..."
                            } else {
                                state.statusMessage.ifBlank { "ทำงานเสร็จสิ้น" }
                            },
                            color = Color(0xFFF1F5F9),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (state.durationMs > 0) {
                        Text(
                            text = "${state.durationMs}ms",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Execution Mode Switcher Buttons (when completed)
            if (!state.isRunning && state.activeScriptFile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            state.activeScriptFile?.let { onRerunInApp?.invoke(it) }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("รันในแอพโดยตรง", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            state.activeScriptFile?.let { onRerunViaTermux?.invoke(it) }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("รันผ่าน Termux", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Monospace Terminal Output Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0F1D))
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.outputLogs) { line ->
                        val textColor = when {
                            line.contains("[STDERR]") || line.contains("[ERR]") || line.contains("❌") || line.contains("Error") -> Color(0xFFF87171)
                            line.contains("[STDOUT]") || line.contains("[OUT]") || line.contains("✅") -> Color(0xFF4ADE80)
                            line.contains("[PROMPT]") -> Color(0xFFFBBF24)
                            line.contains("[IN]") || line.contains("💬") -> Color(0xFF38BDF8)
                            line.contains("🚀") || line.contains("⚡") -> Color(0xFF67E8F9)
                            line.contains("⚠️") -> Color(0xFFFCD34D)
                            else -> Color(0xFFCBD5E1)
                        }

                        Text(
                            text = line,
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive STDIN Direct Answer Box
            Surface(
                color = Color(0xFF131D33),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (state.isRunning) 1.5.dp else 1.dp,
                        color = if (state.isRunning) Color(0xFF38BDF8) else Color(0xFF334155),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QuestionAnswer,
                        contentDescription = null,
                        tint = if (state.isRunning) Color(0xFF38BDF8) else Color(0xFF64748B),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(start = 2.dp)
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                if (state.isRunning) "พิมพ์ตอบสคริปต์ (เช่น ระบุชื่อไฟล์, y/n, ตัวเลข)..." else "พิมพ์ข้อความตอบกลับสคริปต์...",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_interactive_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() || state.isRunning) {
                                    onSendInput(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC)
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || state.isRunning) {
                                onSendInput(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.testTag("terminal_send_input_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ส่งข้อมูลตอบกลับสคริปต์ทันที",
                            tint = if (state.isRunning) Color(0xFF38BDF8) else Color(0xFF64748B)
                        )
                    }
                }
            }

            // Quick Direct Answer Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { onSendInput("y") },
                    label = { Text("y (Yes)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF4ADE80))
                )
                AssistChip(
                    onClick = { onSendInput("n") },
                    label = { Text("n (No)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFF87171))
                )
                AssistChip(
                    onClick = { onSendInput("") },
                    label = { Text("⏎ Enter", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF38BDF8))
                )
                AssistChip(
                    onClick = { onSendInput("test.txt") },
                    label = { Text("test.txt", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFE2E8F0))
                )
                AssistChip(
                    onClick = { onSendInput("backup.sh") },
                    label = { Text("backup.sh", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFE2E8F0))
                )
                AssistChip(
                    onClick = { onSendInput("1") },
                    label = { Text("1", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFCBD5E1))
                )
                AssistChip(
                    onClick = { onSendInput("2") },
                    label = { Text("2", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFCBD5E1))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val fullLog = state.outputLogs.joinToString("\n")
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Termux Log", fullLog))
                            Toast.makeText(context, "คัดลอกข้อความ Log แล้ว", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("คัดลอก", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val launchIntent = TermuxCommandManager(context).getLaunchTermuxIntent()
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(context, "ไม่พบแอพ Termux", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp), tint = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("เปิด Termux", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        if (state.isRunning) {
                            onStopExecution()
                        } else {
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (state.isRunning) "Stop" else "ปิด")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
