package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LiveExecutionState

@Composable
fun LiveTerminalDialog(
    state: LiveExecutionState,
    onDismiss: () -> Unit,
    onRerun: () -> Unit,
    onSendInput: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }

    // Auto scroll down as new logs arrive
    LaunchedEffect(state.outputLogs.size) {
        if (state.outputLogs.isNotEmpty()) {
            listState.animateScrollToItem(state.outputLogs.size - 1)
        }
    }

    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("live_terminal_dialog"),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Terminal Title Bar (MacOS/Linux style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Window Dots
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = if (state.scriptName.isNotBlank()) "Terminal • ${state.scriptName}" else "Termux Live Console",
                            color = Color(0xFFF1F5F9),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Running status badge
                        if (state.isRunning) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF38BDF8))
                                            .alpha(cursorAlpha)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "กำลังรัน...",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (state.exitCode != null) {
                            val isSuccess = state.exitCode == 0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isSuccess) "สำเร็จ (0)" else "ล้มเหลว (${state.exitCode})",
                                    color = if (isSuccess) Color(0xFF34D399) else Color(0xFFF87171),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("close_terminal_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ปิดหน้าต่าง",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Sub-info Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131D33))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "สถานะ: ${state.statusText}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    if (state.durationMs > 0) {
                        Text(
                            text = "ระยะเวลา: ${state.durationMs}ms",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                // Terminal Log Display Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFF0A0F1D))
                        .padding(14.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.outputLogs) { logLine ->
                            val textColor = when {
                                logLine.contains("[ERR]") || logLine.contains("ข้อผิดพลาด") || logLine.contains("❌") -> Color(0xFFF87171)
                                logLine.contains("[OUT]") || logLine.contains("✅") || logLine.contains("สำเร็จ") -> Color(0xFF4ADE80)
                                logLine.contains("[PROMPT]") -> Color(0xFFFBBF24)
                                logLine.contains("[IN]") || logLine.contains("💬") -> Color(0xFF38BDF8)
                                logLine.contains("🚀") || logLine.contains("📍") || logLine.contains("⚡") -> Color(0xFF38BDF8)
                                logLine.contains("⚠️") || logLine.contains("⏳") -> Color(0xFFFBBF24)
                                else -> Color(0xFFCBD5E1)
                            }

                            Text(
                                text = logLine,
                                color = textColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        // Terminal prompt line
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "termux@hyperos:~# ",
                                    color = Color(0xFF38BDF8),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "▋",
                                    color = Color(0xFF10B981),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.alpha(cursorAlpha)
                                )
                            }
                        }
                    }
                }

                // Interactive STDIN Prompt Bar
                if (onSendInput != null) {
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "❯",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, end = 6.dp)
                                )

                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            "พิมพ์ตอบกลับสคริปต์ (เช่น ชื่อไฟล์, y/n, ข้อความ)...",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("live_interactive_input"),
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
                                    modifier = Modifier.testTag("live_send_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "ส่งข้อมูล",
                                        tint = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            // Quick suggestion chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 4.dp),
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
                            }
                        }
                    }
                }

                // Action Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Copy Log
                        OutlinedButton(
                            onClick = {
                                val fullLog = state.outputLogs.joinToString("\n")
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Termux Log", fullLog))
                                Toast.makeText(context, "คัดลอก Log เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_log_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "คัดลอก Log",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("คัดลอก", fontSize = 12.sp, color = Color(0xFFE2E8F0))
                        }

                        // Share Log
                        OutlinedButton(
                            onClick = {
                                val fullLog = state.outputLogs.joinToString("\n")
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Log สคริปต์ Termux: ${state.scriptName}")
                                    putExtra(Intent.EXTRA_TEXT, fullLog)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "แชร์ Log การทำงาน"))
                            },
                            modifier = Modifier.testTag("share_log_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "แชร์ Log",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("แชร์", fontSize = 12.sp, color = Color(0xFFE2E8F0))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!state.isRunning) {
                            Button(
                                onClick = onRerun,
                                modifier = Modifier.testTag("rerun_script_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "รันซ้ำ",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("รันซ้ำ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("dismiss_terminal_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("ปิด", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
