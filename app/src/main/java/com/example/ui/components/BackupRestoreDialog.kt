package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.backup.BackupPayload
import com.example.backup.RestoreMode
import com.example.data.model.TermuxScript

@Composable
fun BackupRestoreDialog(
    scripts: List<TermuxScript>,
    onBackupToUri: (Uri) -> Boolean,
    onBackupToGoogleDrive: () -> Unit,
    onInspectBackup: (Uri) -> BackupPayload?,
    onPerformRestore: (BackupPayload, RestoreMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Backup, 1: Restore

    // Selected payload for restore
    var inspectedPayload by remember { mutableStateOf<BackupPayload?>(null) }
    var restoreMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var restoreStatusMessage by remember { mutableStateOf<String?>(null) }

    // Launcher for creating local backup file
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val success = onBackupToUri(uri)
            if (success) {
                Toast.makeText(context, "สำรองข้อมูลลงเครื่องเรียบร้อยแล้ว", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึกไฟล์", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for selecting restore file
    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val payload = onInspectBackup(uri)
            if (payload != null) {
                inspectedPayload = payload
                restoreStatusMessage = "ตรวจสอบไฟล์สำเร็จ: พบ ${payload.scriptCount} สคริปต์"
            } else {
                inspectedPayload = null
                restoreStatusMessage = "ไฟล์สำรองไม่ถูกต้องหรือไม่สามารถอ่านได้"
                Toast.makeText(context, "ไฟล์ไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("backup_restore_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ระบบสำรองข้อมูล & กู้คืน (Backup & Restore)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("สำรองข้อมูล (Backup)")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("กู้คืนข้อมูล (Restore)")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // TAB: BACKUP
                    Text(
                        text = "ข้อมูลที่จะถูกสำรอง: สคริปต์ทั้งหมด (${scripts.size} รายการ) และการตั้งค่าแอพ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Local storage
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "1. พื้นที่จัดเก็บของเครื่อง (Local Storage)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "บันทึกเป็นไฟล์ .json ลงใน Download หรือโฟลเดอร์ใดก็ได้",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                    createBackupLauncher.launch("termux_backup_$dateStr.json")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("backup_local_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("บันทึกไฟล์สำรองลงเครื่อง")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: Google Drive
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddToDrive,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "2. สำรองข้อมูลไปยัง Google Drive",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "อัปโหลดและเก็บรักษาไฟล์สำรองบน Cloud Google Drive โดยตรง",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onBackupToGoogleDrive,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("backup_drive_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F766E)
                                )
                            ) {
                                Text("ส่งออกไปยัง Google Drive")
                            }
                        }
                    }

                } else {
                    // TAB: RESTORE
                    Text(
                        text = "เลือกไฟล์สำรอง (.json) เพื่อกู้คืนสคริปต์และการตั้งค่าเดิมกลับมา",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Select file button
                    OutlinedButton(
                        onClick = {
                            openRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_restore_file_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("เลือกไฟล์สำรอง (.json) จากเครื่องหรือ Google Drive")
                    }

                    // Inspection Preview
                    AnimatedVisibility(visible = inspectedPayload != null) {
                        inspectedPayload?.let { payload ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "รายละเอียดไฟล์สำรอง",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("• วันที่บันทึก: ${payload.timestampFormatted.ifBlank { "ไม่ระบุ" }}", fontSize = 12.sp)
                                Text("• สคริปต์ทั้งหมด: ${payload.scriptCount} รายการ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("• โฟลเดอร์ Termux: ${payload.settings.termuxShortcutsDir}", fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "เลือกรูปแบบการกู้คืน:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                // Mode: MERGE
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = restoreMode == RestoreMode.MERGE,
                                        onClick = { restoreMode = RestoreMode.MERGE }
                                    )
                                    Column(modifier = Modifier.padding(start = 4.dp)) {
                                        Text("รวมกับข้อมูลเดิม (Merge)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text("เพิ่มสคริปต์ใหม่เข้าไปโดยไม่ลบสคริปต์ที่มีอยู่เดิม", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // Mode: OVERWRITE
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = restoreMode == RestoreMode.OVERWRITE,
                                        onClick = { restoreMode = RestoreMode.OVERWRITE }
                                    )
                                    Column(modifier = Modifier.padding(start = 4.dp)) {
                                        Text("เขียนทับทั้งหมด (Overwrite)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                        Text("ลบสคริปต์ปัจจุบันออกทั้งหมดแล้วแทนที่ด้วยข้อมูลจากไฟล์สำรอง", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        onPerformRestore(payload, restoreMode)
                                        Toast.makeText(context, "กู้คืนข้อมูลสำเร็จเรียบร้อย", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_restore_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (restoreMode == RestoreMode.OVERWRITE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("ยืนยันการกู้คืนข้อมูล")
                                }
                            }
                        }
                    }

                    if (restoreStatusMessage != null && inspectedPayload == null) {
                        Text(
                            text = restoreStatusMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ปิด")
                    }
                }
            }
        }
    }
}
