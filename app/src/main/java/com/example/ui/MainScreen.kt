package com.example.ui

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TermuxScript
import com.example.data.model.TermuxScriptFile
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.LiveTerminalDialog
import com.example.ui.components.LogHistorySheet
import com.example.ui.components.ScriptCard
import com.example.ui.components.ScriptEditorDialog
import com.example.ui.components.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Navigation Tab state: 0 = Termux SAF Scripts, 1 = Quick Shortcuts & Widget
    var currentTab by remember { mutableIntStateOf(0) }

    // SAF Editor state
    var isSafEditorOpen by remember { mutableStateOf(false) }
    var editingSafScript by remember { mutableStateOf<TermuxScriptFile?>(null) }
    var safEditorContent by remember { mutableStateOf("") }

    if (isSafEditorOpen) {
        ScriptEditorScreen(
            script = editingSafScript,
            initialContent = safEditorContent,
            onSave = { name, content, isNewFile ->
                viewModel.saveSafScript(name, content, isNewFile, editingSafScript?.uri) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) {
                        isSafEditorOpen = false
                    }
                }
            },
            onRun = { scriptFile ->
                viewModel.runSafScript(scriptFile)
            },
            onNavigateBack = {
                isSafEditorOpen = false
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Termux Files (SAF)") },
                    label = { Text("Termux SAF (.sh)") },
                    modifier = Modifier.testTag("tab_saf_scripts")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Quick Shortcuts") },
                    label = { Text("ช็อตคัต & Widget") },
                    modifier = Modifier.testTag("tab_shortcuts")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentTab == 0) {
                TermuxScriptsScreen(
                    viewModel = viewModel,
                    onOpenEditorForScript = { scriptFile ->
                        editingSafScript = scriptFile
                        if (scriptFile != null) {
                            viewModel.readSafScriptContent(scriptFile.uri) { content ->
                                safEditorContent = content
                                isSafEditorOpen = true
                            }
                        } else {
                            safEditorContent = ""
                            isSafEditorOpen = true
                        }
                    }
                )
            } else {
                ShortcutsDashboardTab(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutsDashboardTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val scripts by viewModel.filteredScripts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isTermuxDetected by viewModel.isTermuxDetected.collectAsStateWithLifecycle()
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val showLiveDialog by viewModel.showLiveLogDialog.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var scriptToEdit by remember { mutableStateOf<TermuxScript?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var scriptToDelete by remember { mutableStateOf<TermuxScript?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }

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
                            text = "By IDOL_CHAMP • HyperOS 3.1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLogsSheet = true },
                        modifier = Modifier.testTag("action_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "ประวัติการรัน",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("action_backup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "สำรองข้อมูลและกู้คืน",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("action_settings_btn")
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
            FloatingActionButton(
                onClick = {
                    scriptToEdit = null
                    showEditorDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_script_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "เพิ่มช็อตคัต")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("เพิ่มช็อตคัต", fontWeight = FontWeight.Bold)
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
            // Hardware Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isTermuxDetected) Color(0xFF10B981) else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isTermuxDetected) "Termux พร้อมใช้งาน" else "โหมดทดสอบคำสั่งสำรอง (Fallback)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Xiaomi 13 Pro",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "📱 จัดการไฟล์จริงผ่านแท็บ 'Termux SAF' • รันช็อตคัตด่วนและตั้งค่าหน้าจอหลัก",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("ค้นหาช็อตคัตหรือคำสั่ง...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_scripts_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Category Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCategorySelected(category) },
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

            // Live execution banner if currently running
            if (liveState.isRunning) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openLiveLogDialog() }
                            .testTag("running_banner_card"),
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
                                        text = "กำลังรัน: ${liveState.scriptName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "แตะเพื่อเปิดหน้าต่างดู Log แบบเรียลไทม์",
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

            // List of Script Cards
            if (scripts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "ไม่พบช็อตคัตที่ค้นหา" else "ยังไม่มีช็อตคัตในระบบ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(scripts, key = { it.id }) { script ->
                    ScriptCard(
                        script = script,
                        onRun = { s -> viewModel.runScript(s) },
                        onEdit = { s ->
                            scriptToEdit = s
                            showEditorDialog = true
                        },
                        onDelete = { s -> scriptToDelete = s },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleWidget = viewModel::toggleWidget
                    )
                }
            }
        }
    }

    // Live Terminal Dialog
    if (showLiveDialog) {
        LiveTerminalDialog(
            state = liveState,
            onDismiss = viewModel::dismissLiveLogDialog,
            onRerun = {
                val script = scripts.find { it.id == liveState.scriptId }
                if (script != null) {
                    viewModel.runScript(script)
                }
            },
            onSendInput = { input ->
                viewModel.sendInteractiveInput(input)
            }
        )
    }

    // Script Editor Dialog (Room shortcuts)
    if (showEditorDialog) {
        ScriptEditorDialog(
            initialScript = scriptToEdit,
            existingCategories = categories,
            onDismiss = { showEditorDialog = false },
            onSave = { savedScript ->
                viewModel.saveScript(savedScript)
                showEditorDialog = false
                Toast.makeText(context, "บันทึกช็อตคัตสำเร็จ", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Backup & Restore Dialog
    if (showBackupDialog) {
        BackupRestoreDialog(
            scripts = scripts,
            onBackupToUri = { uri -> viewModel.backupToUri(uri, scripts) },
            onBackupToGoogleDrive = {
                val shareIntent = viewModel.prepareGoogleDriveShareIntent(scripts)
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

    // Log History Bottom Sheet
    if (showLogsSheet) {
        LogHistorySheet(
            logs = allLogs,
            onClearLogs = viewModel::clearAllLogs,
            onDismiss = { showLogsSheet = false }
        )
    }

    // Delete Script Confirmation Dialog
    if (scriptToDelete != null) {
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            title = { Text("ยืนยันการลบช็อตคัต") },
            text = { Text("คุณแน่ใจหรือไม่ว่าต้องการลบ '${scriptToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scriptToDelete?.let { viewModel.deleteScript(it) }
                        scriptToDelete = null
                        Toast.makeText(context, "ลบช็อตคัตเรียบร้อย", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ลบ")
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToDelete = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}
