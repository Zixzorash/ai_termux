package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppSettings
import com.example.utils.StoragePermissionHelper

@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    isTermuxDetected: Boolean,
    onSave: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var shortcutsDir by remember { mutableStateOf(currentSettings.termuxShortcutsDir) }
    var workDir by remember { mutableStateOf(currentSettings.defaultWorkDir) }
    var notifyFinish by remember { mutableStateOf(currentSettings.notifyOnFinish) }
    var notifyRunning by remember { mutableStateOf(currentSettings.notifyRunningStatus) }
    var vibrateFinish by remember { mutableStateOf(currentSettings.vibrateOnFinish) }
    var fallbackLocal by remember { mutableStateOf(currentSettings.fallbackToLocalRunnerIfNoTermux) }
    var defaultEngine by remember { mutableStateOf(currentSettings.defaultExecutionEngine) }
    var autoOpenSheet by remember { mutableStateOf(currentSettings.autoOpenTerminalSheetOnRun) }

    // Storage permission status state
    var storageInfo by remember { mutableStateOf(StoragePermissionHelper.getStoragePermissionInfo(context)) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var customTestPath by remember { mutableStateOf("/sdcard/Movies/") }

    // Permission launcher for Media (Android 13+) / Storage (Android 12-)
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        storageInfo = StoragePermissionHelper.getStoragePermissionInfo(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("settings_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "การตั้งค่า (Settings)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Xiaomi 13 Pro / HyperOS 3.1 Specs Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Xiaomi 13 Pro • HyperOS 3.1",
                                color = Color(0xFFF1F5F9),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "⚡ Snapdragon 8 Gen 2 | RAM 12GB | Wi-Fi 7 (802.11be)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "🚀 ปรับแต่งประสิทธิภาพสูงสุดสำหรับ Android 16 (Global ROM)",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Execution Engine Options (MANDATORY for direct reply & in-app execution)
                Text("โหมดการรันสคริปต์เริ่มต้น", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                // Option 1: In-App Direct Interactive Engine
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { defaultEngine = "IN_APP_DIRECT" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (defaultEngine == "IN_APP_DIRECT")
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (defaultEngine == "IN_APP_DIRECT") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (defaultEngine == "IN_APP_DIRECT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "รันในแอพโดยตรง (รองรับการตอบกลับสด)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981)
                                ) {
                                    Text(
                                        text = "แนะนำ",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "รันสคริปต์ได้ทันทีโดยไม่ต้องเปิด Termux และสามารถพิมพ์ตอบคำถามสคริปต์ (เช่น ระบุชื่อไฟล์, y/n) ได้แบบเรียลไทม์",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Termux RUN_COMMAND Service
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { defaultEngine = "TERMUX_SERVICE" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (defaultEngine == "TERMUX_SERVICE")
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (defaultEngine == "TERMUX_SERVICE") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (defaultEngine == "TERMUX_SERVICE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ส่งคำสั่งไปยัง Termux (RUN_COMMAND Intent)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "ส่ง Intent ไปให้ Service ของ Termux ทำงานในสภาพแวดล้อม Termux จริง",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Open Terminal Sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("เปิดหน้าต่าง Terminal อัตโนมัติเมื่อกดรัน", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("แสดงหน้าต่าง Live Log และช่องพิมพ์ตอบกลับทันทีที่เริ่มรัน", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoOpenSheet,
                        onCheckedChange = { autoOpenSheet = it },
                        modifier = Modifier.testTag("setting_switch_auto_open_terminal")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Termux Integration Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTermuxDetected) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFF7F1D1D).copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTermuxDetected) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (isTermuxDetected) Color(0xFF34D399) else Color(0xFFF87171),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isTermuxDetected) "ตรวจพบ Termux ในเครื่องแล้ว" else "ยังไม่พบแอพพลิเคชัน Termux",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isTermuxDetected) Color(0xFF34D399) else Color(0xFFF87171)
                            )
                            Text(
                                text = if (isTermuxDetected)
                                    "พร้อมส่ง Intent คำสั่งผ่าน com.termux.RUN_COMMAND"
                                else
                                    "สามารถติดตั้ง Termux จาก F-Droid / GitHub เพื่อรันแบบเต็มระบบ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Storage Access & Permissions Card for /sdcard/Movies/, /sdcard/Download/, etc.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (storageInfo.hasAllFilesAccess) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFF1E293B)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (storageInfo.hasAllFilesAccess) Color(0xFF34D399) else Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "สิทธิ์เข้าถึงพื้นที่จัดเก็บอุปกรณ์ (/sdcard/Movies)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "อนุญาตให้สคริปต์ Shell และแอพพลิเคชันเข้าถึงไฟล์ใน /sdcard/Movies/, /storage/movies/, /sdcard/Download/ เพื่อตรวจสอบคำบรรยาย วิดีโอ และประมวลผลข้อมูล",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status All Files Access
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (storageInfo.hasAllFilesAccess)
                                        "✅ All Files Access: ได้รับสิทธิ์แล้ว"
                                    else
                                        "⚠️ All Files Access: ยังไม่ได้รับสิทธิ์",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (storageInfo.hasAllFilesAccess) Color(0xFF34D399) else Color(0xFFFBBF24)
                                )
                                Text(
                                    text = if (storageInfo.hasAllFilesAccess)
                                        "สามารถอ่าน/เขียน /sdcard/Movies/ ได้โดยตรง"
                                    else
                                        "จำเป็นสำหรับสคริปต์ที่เรียกใช้ /sdcard/Movies/ หรือโฟลเดอร์ภายนอก",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(StoragePermissionHelper.createManageAllFilesIntent(context))
                                    } catch (e: Exception) {
                                        context.startActivity(StoragePermissionHelper.createAppDetailsSettingsIntent(context))
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (storageInfo.hasAllFilesAccess) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (storageInfo.hasAllFilesAccess) "ตั้งค่าสิทธิ์" else "ขอสิทธิ์ All Files",
                                    fontSize = 11.sp,
                                    color = if (storageInfo.hasAllFilesAccess) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        if (!storageInfo.hasAllFilesAccess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (storageInfo.hasMediaPermissions) "✅ Media Permissions: ได้รับแล้ว" else "ขอสิทธิ์เข้าถึง Media (Video/Audio)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            mediaPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_MEDIA_VIDEO,
                                                    Manifest.permission.READ_MEDIA_AUDIO,
                                                    Manifest.permission.READ_MEDIA_IMAGES
                                                )
                                            )
                                        } else {
                                            mediaPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                )
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("ขอสิทธิ์ Media", fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Path Access Tester
                        Text("ทดสอบการเข้าถึงโฟลเดอร์ของอุปกรณ์:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customTestPath,
                                onValueChange = { customTestPath = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    val status = StoragePermissionHelper.testDirectoryAccess(customTestPath.trim())
                                    testResultText = buildString {
                                        append(if (status.canRead) "✅ " else "❌ ")
                                        append("${status.path}: ${status.statusMessage}\n")
                                        if (status.sampleFiles.isNotEmpty()) {
                                            append("ตัวอย่างไฟล์: ")
                                            append(status.sampleFiles.joinToString(", "))
                                        }
                                    }
                                    storageInfo = StoragePermissionHelper.getStoragePermissionInfo(context)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ทดสอบ", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val writeResult = StoragePermissionHelper.testWriteAndSaveFile(customTestPath.trim())
                                    testResultText = buildString {
                                        append(if (writeResult.success) "✅ [อ่าน/เขียน/แก้ไข/บันทึก]: " else "❌ [ข้อผิดพลาดการเขียน/บันทึก]: ")
                                        append(writeResult.message)
                                        append("\nโฟลเดอร์: ${writeResult.path}")
                                    }
                                    storageInfo = StoragePermissionHelper.getStoragePermissionInfo(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ทดสอบเขียน/บันทึกไฟล์ (R/W)", fontSize = 11.sp)
                            }
                        }

                        testResultText?.let { result ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = result,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Directory paths
                Text("โฟลเดอร์เก็บสคริปต์ Termux", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = shortcutsDir,
                    onValueChange = { shortcutsDir = it },
                    label = { Text("Shortcuts Directory") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setting_shortcuts_dir"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "ค่าเริ่มต้น: /data/data/com.termux/files/home/.shortcuts/ หรือ ~/.shortcuts/",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = workDir,
                    onValueChange = { workDir = it },
                    label = { Text("Working Directory (ค่าเริ่มต้น)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setting_work_dir"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Toggles
                Text("การแจ้งเตือนแบบเรียลไทม์", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("แจ้งเตือนเมื่อสคริปต์เสร็จสิ้น", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("แสดงสถานะ สำเร็จ/ล้มเหลว พร้อมระยะเวลา", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = notifyFinish,
                        onCheckedChange = { notifyFinish = it },
                        modifier = Modifier.testTag("setting_switch_notify_finish")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("แจ้งเตือนระหว่างรันคำสั่ง", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("แสดงแถบ Notification แบบเรียลไทม์", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = notifyRunning,
                        onCheckedChange = { notifyRunning = it },
                        modifier = Modifier.testTag("setting_switch_notify_running")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("สั่นเมื่อคำสั่งทำงานเสร็จ", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("เพิ่มการตอบสนอง Haptic Feedback", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = vibrateFinish,
                        onCheckedChange = { vibrateFinish = it },
                        modifier = Modifier.testTag("setting_switch_vibrate")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("โหมดทดสอบคำสั่งสำรอง (Fallback)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("รันผ่านเครื่องทดสอบหากยังไม่มี Termux", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = fallbackLocal,
                        onCheckedChange = { fallbackLocal = it },
                        modifier = Modifier.testTag("setting_switch_fallback")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ยกเลิก")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val updated = currentSettings.copy(
                                termuxShortcutsDir = shortcutsDir.trim(),
                                defaultWorkDir = workDir.trim(),
                                notifyOnFinish = notifyFinish,
                                notifyRunningStatus = notifyRunning,
                                vibrateOnFinish = vibrateFinish,
                                fallbackToLocalRunnerIfNoTermux = fallbackLocal,
                                defaultExecutionEngine = defaultEngine,
                                autoOpenTerminalSheetOnRun = autoOpenSheet
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_settings_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("บันทึกการตั้งค่า")
                    }
                }
            }
        }
    }
}
