package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import com.example.ui.components.StoragePermissionBanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TermuxStorageState
import com.example.data.model.TermuxScriptFile
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.LogHistorySheet
import com.example.ui.components.RenameFileDialog
import com.example.ui.components.RunResultBottomSheet
import com.example.ui.components.SettingsDialog
import com.example.utils.SafUtils
import com.example.utils.StoragePermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxScriptsScreen(
    viewModel: MainViewModel,
    onOpenEditorForScript: (TermuxScriptFile?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val storageState by viewModel.storageState.collectAsStateWithLifecycle()
    val storagePermissionInfo by viewModel.storagePermissionInfo.collectAsStateWithLifecycle()
    val scripts by viewModel.safScripts.collectAsStateWithLifecycle()
    val runState by viewModel.scriptRunState.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isTermuxDetected by viewModel.isTermuxDetected.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("ทั้งหมด") }

    // Favorite set state
    var favoriteSet by remember { mutableStateOf(setOf<String>()) }

    // Dialog & Sheet States
    var scriptToRename by remember { mutableStateOf<TermuxScriptFile?>(null) }
    var scriptToDelete by remember { mutableStateOf<TermuxScriptFile?>(null) }
    var scriptForDetails by remember { mutableStateOf<TermuxScriptFile?>(null) }
    var showRunSheet by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }

    // SAF Tree Picker Launcher
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.connectSafTreeUri(uri)
        }
    }

    val filterCategories = listOf("ทั้งหมด", "รายการโปรด", ".shortcuts", "แก้ไขล่าสุด")

    val filteredScripts = scripts.filter { script ->
        val matchesQuery = searchQuery.isBlank() ||
            script.name.contains(searchQuery, ignoreCase = true) ||
            script.logicalPosixPath.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (selectedFilterCategory) {
            "รายการโปรด" -> favoriteSet.contains(script.name)
            ".shortcuts" -> script.relativePath.contains(".shortcuts") || script.logicalPosixPath.contains(".shortcuts")
            "แก้ไขล่าสุด" -> script.lastModified > 0
            else -> true
        }

        matchesQuery && matchesCategory
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Run Termux",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = "By IDOL_CHAMP • HyperOS 3.1 • Termux SAF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                },
                actions = {
                    // History Button
                    IconButton(
                        onClick = { showLogsSheet = true },
                        modifier = Modifier.testTag("saf_action_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "ประวัติการรัน",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Backup & Restore Button
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("saf_action_backup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "สำรองข้อมูลและกู้คืน",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Settings Button (Prominently placed in Top Bar)
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("saf_action_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "การตั้งค่า",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            val isConnected = storageState is TermuxStorageState.Connected
            if (isConnected) {
                FloatingActionButton(
                    onClick = { onOpenEditorForScript(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("new_script_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "เพิ่มสคริปต์")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("เพิ่มสคริปต์", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Storage Permission Alert Banner
            item {
                StoragePermissionBanner(
                    storageInfo = storagePermissionInfo,
                    onPermissionUpdated = { viewModel.refreshStoragePermissions() }
                )
            }

            // Hardware Status & Xiaomi 13 Pro Header Card (Harmonized with Shortcuts tab)
            item {
                SafHardwareHeaderCard(
                    state = storageState,
                    storagePermissionInfo = storagePermissionInfo,
                    isTermuxDetected = isTermuxDetected,
                    onConnectClick = { openDocumentTreeLauncher.launch(null) },
                    onChangeFolderClick = { openDocumentTreeLauncher.launch(null) },
                    onDisconnectClick = { viewModel.disconnectSaf() },
                    onOpenSettingsClick = { showSettingsDialog = true }
                )
            }

            // Live execution banner if currently running
            if (runState.isRunning) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRunSheet = true }
                            .testTag("saf_running_banner_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0369A1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "กำลังรัน: ${runState.scriptName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (runState.isWaitingForInput)
                                            "🟢 กำลังรอรับคำตอบจากคุณ แตะเพื่อพิมพ์ตอบกลับทันที!"
                                        else
                                            "แตะเพื่อดูสดและพิมพ์ตอบกลับสคริปต์ได้ทันที",
                                        color = Color(0xFFC7E7FF),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                text = "ดูสด ❯",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Search Bar & Filter Chips (Visible if connected)
            if (storageState is TermuxStorageState.Connected) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ค้นหาไฟล์สคริปต์ .sh หรือคำสั่ง...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "ล้างการค้นหา")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saf_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Filter Chips Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterCategories.forEach { category ->
                            val isSelected = selectedFilterCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilterCategory = category },
                                label = { Text(category, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Scripts List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "สคริปต์ในโฟลเดอร์ (${filteredScripts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.refreshSafScripts() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "รีเฟรชไฟล์", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Scripts List
                if (filteredScripts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "ไม่พบสคริปต์ที่ค้นหา" else "ยังไม่มีไฟล์ Shell Script (.sh)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "แตะปุ่ม '+ เพิ่มสคริปต์' ด้านล่างเพื่อเริ่มสร้างไฟล์แรกผ่าน SAF",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredScripts, key = { it.uri.toString() }) { scriptFile ->
                        val isFav = favoriteSet.contains(scriptFile.name)
                        SafScriptItemCard(
                            script = scriptFile,
                            isFavorite = isFav,
                            onToggleFavorite = {
                                favoriteSet = if (favoriteSet.contains(scriptFile.name)) {
                                    favoriteSet - scriptFile.name
                                } else {
                                    favoriteSet + scriptFile.name
                                }
                            },
                            onRun = {
                                viewModel.runSafScript(it)
                                showRunSheet = true
                            },
                            onEdit = { onOpenEditorForScript(it) },
                            onRename = { scriptToRename = it },
                            onDuplicate = { viewModel.duplicateSafScript(it) },
                            onDelete = { scriptToDelete = it },
                            onDetails = { scriptForDetails = it },
                            onReadContent = { uri, callback ->
                                viewModel.readSafScriptContent(uri, callback)
                            }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    scriptToRename?.let { target ->
        RenameFileDialog(
            script = target,
            onConfirm = { newName ->
                viewModel.renameSafScript(target, newName)
                scriptToRename = null
            },
            onDismiss = { scriptToRename = null }
        )
    }

    // Details Dialog
    scriptForDetails?.let { target ->
        FileDetailsDialog(
            script = target,
            onDismiss = { scriptForDetails = null }
        )
    }

    // Delete Confirmation Dialog
    scriptToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            title = { Text("ลบไฟล์สคริปต์", fontWeight = FontWeight.Bold) },
            text = {
                Text("คุณแน่ใจหรือไม่ว่าต้องการลบไฟล์ '${target.name}' ออกจากพื้นที่จัดเก็บของ Termux ผ่าน SAF?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSafScript(target)
                        scriptToDelete = null
                        Toast.makeText(context, "ลบไฟล์เรียบร้อย", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ลบไฟล์")
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToDelete = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    // Run Result Bottom Sheet with interactive STDIN input
    if (showRunSheet) {
        RunResultBottomSheet(
            state = runState,
            onDismiss = { showRunSheet = false },
            onSendInput = { inputText ->
                viewModel.sendInteractiveInput(inputText)
            },
            onStopExecution = {
                viewModel.stopRunningScript()
            },
            onRerunInApp = { targetScript ->
                viewModel.runSafScript(targetScript, com.example.termux.ExecutionMode.IN_APP_DIRECT)
            },
            onRerunViaTermux = { targetScript ->
                viewModel.runSafScript(targetScript, com.example.termux.ExecutionMode.TERMUX_SERVICE)
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            isTermuxDetected = isTermuxDetected,
            onSave = { newSettings ->
                viewModel.saveSettings(newSettings)
                Toast.makeText(context, "บันทึกการตั้งค่าเรียบร้อย", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Backup & Restore Dialog
    if (showBackupDialog) {
        BackupRestoreDialog(
            scripts = emptyList(),
            onBackupToUri = { uri -> viewModel.backupToUri(uri, emptyList()) },
            onBackupToGoogleDrive = {
                val shareIntent = viewModel.prepareGoogleDriveShareIntent(emptyList())
                context.startActivity(Intent.createChooser(shareIntent, "บันทึกหรือแชร์ไปยัง Google Drive"))
            },
            onInspectBackup = { uri -> viewModel.inspectBackup(uri) },
            onPerformRestore = { payload, mode ->
                viewModel.performRestore(payload, mode) { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    // History Log Sheet
    if (showLogsSheet) {
        LogHistorySheet(
            logs = allLogs,
            onClearLogs = viewModel::clearAllLogs,
            onDismiss = { showLogsSheet = false }
        )
    }
}

@Composable
private fun SafHardwareHeaderCard(
    state: TermuxStorageState,
    storagePermissionInfo: StoragePermissionHelper.StoragePermissionInfo,
    isTermuxDetected: Boolean,
    onConnectClick: () -> Unit,
    onChangeFolderClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("storage_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Xiaomi 13 Pro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Snapdragon 8 Gen 2 • RAM 12GB",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HyperOS 3.1 • Android 16 • Wi-Fi 7 Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Storage Permissions Status Badge (e.g. /sdcard/Movies/ access)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (storagePermissionInfo.hasAllFilesAccess) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = if (storagePermissionInfo.hasAllFilesAccess) Color(0xFF34D399) else Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (storagePermissionInfo.hasAllFilesAccess)
                                "สิทธิ์เข้าถึงอุปกรณ์ (/sdcard/Movies): พร้อมใช้งาน"
                            else
                                "สิทธิ์ไฟล์เครื่อง: แตะการตั้งค่าเพื่อเปิด All Files",
                            fontSize = 11.sp,
                            color = if (storagePermissionInfo.hasAllFilesAccess) Color(0xFF34D399) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = if (storagePermissionInfo.hasAllFilesAccess) "✅ All Files" else "⚙️ ตั้งค่า",
                        fontSize = 10.sp,
                        color = if (storagePermissionInfo.hasAllFilesAccess) Color(0xFF34D399) else Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenSettingsClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Storage Connection Status Box
            when (state) {
                is TermuxStorageState.Connected -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Termux Storage: เชื่อมต่อ SAF สำเร็จ",
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "ตอบกลับสดได้",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = state.displayPath,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "📁 สคริปต์ใน ~/.shortcuts/ • แตะรันเพื่อดูสดและพิมพ์ตอบกลับสคริปต์ผ่านแอพได้ทันที",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChangeFolderClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("เปลี่ยนโฟลเดอร์", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenSettingsClick,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("saf_header_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("การตั้งค่า", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onDisconnectClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ตัดการเชื่อมต่อ", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                is TermuxStorageState.PermissionRevoked -> {
                    Text(
                        text = state.reason,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("เชื่อมต่อโฟลเดอร์ Termux ใหม่อีกครั้ง")
                    }
                }

                else -> {
                    Text(
                        text = "เลือกโฟลเดอร์ Termux Home หรือ ~/.shortcuts ผ่าน Android Storage Access Framework (SAF) เพื่อจัดการและรันไฟล์ .sh พร้อมตอบกลับสด",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConnectClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("connect_termux_folder_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("เชื่อมต่อโฟลเดอร์ Termux", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenSettingsClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("saf_header_settings_btn_unconnected")
                        ) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("การตั้งค่า", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SafScriptItemCard(
    script: TermuxScriptFile,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRun: (TermuxScriptFile) -> Unit,
    onEdit: (TermuxScriptFile) -> Unit,
    onRename: (TermuxScriptFile) -> Unit,
    onDuplicate: (TermuxScriptFile) -> Unit,
    onDelete: (TermuxScriptFile) -> Unit,
    onDetails: (TermuxScriptFile) -> Unit,
    onReadContent: (Uri, (String) -> Unit) -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var showCodePreview by remember { mutableStateOf(false) }
    var codeContent by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saf_script_card_${script.name}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Category Chip / Badges & Action Menu (Identical to Shortcuts ScriptCard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Shell Script",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // File Size Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = SafUtils.formatFileSize(script.size),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Favorite Star Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("saf_fav_btn_${script.name}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "ลบจากรายการโปรด" else "เพิ่มในรายการโปรด",
                            tint = if (isFavorite) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Manage Menu
                    Box {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("saf_menu_btn_${script.name}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "จัดการสคริปต์",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("รันสคริปต์ (Run)") },
                                onClick = {
                                    expandedMenu = false
                                    onRun(script)
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("แก้ไขโค้ด (Edit)") },
                                onClick = {
                                    expandedMenu = false
                                    onEdit(script)
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showCodePreview) "ซ่อนโค้ด Bash" else "ดูโค้ด Bash") },
                                onClick = {
                                    expandedMenu = false
                                    if (!showCodePreview && codeContent == null) {
                                        onReadContent(script.uri) { content ->
                                            codeContent = content
                                        }
                                    }
                                    showCodePreview = !showCodePreview
                                },
                                leadingIcon = { Icon(Icons.Default.Code, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("เปลี่ยนชื่อไฟล์ (Rename)") },
                                onClick = {
                                    expandedMenu = false
                                    onRename(script)
                                },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("สร้างสำเนา (Duplicate)") },
                                onClick = {
                                    expandedMenu = false
                                    onDuplicate(script)
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("รายละเอียด (Details)") },
                                onClick = {
                                    expandedMenu = false
                                    onDetails(script)
                                },
                                leadingIcon = { Icon(Icons.Default.Info, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("ลบสคริปต์ (Delete)", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    expandedMenu = false
                                    onDelete(script)
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Script Name (Bold titleMedium)
            Text(
                text = script.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Filename & Path chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = script.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = script.logicalPosixPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Code Preview Section
            AnimatedVisibility(visible = showCodePreview) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "เนื้อหาสคริปต์ Bash:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = codeContent ?: "กำลังโหลดโค้ด...",
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last Modified Date & Action Buttons (Edit + Run)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Modified Date & Status
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = SafUtils.formatLastModified(script.lastModified),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Action Buttons: Edit & Primary Run Button
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onEdit(script) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("saf_edit_btn_${script.name}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "แก้ไข", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("แก้ไข", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onRun(script) },
                        modifier = Modifier.testTag("saf_run_btn_${script.name}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "รันสคริปต์",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "รันสคริปต์",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
