package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupPayload
import com.example.backup.BackupRestoreManager
import com.example.backup.RestoreMode
import com.example.data.TermuxStorageRepository
import com.example.data.TermuxStorageState
import com.example.data.local.AppDatabase
import com.example.data.local.TermuxRepository
import com.example.data.model.AppSettings
import com.example.data.model.ExecutionLog
import com.example.data.model.LiveExecutionState
import com.example.data.model.TermuxScript
import com.example.data.model.TermuxScriptFile
import com.example.service.TermuxRunner
import com.example.termux.ScriptRunState
import com.example.termux.TermuxScriptRunner
import com.example.widget.TermuxAppWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TermuxRepository(
        AppDatabase.getDatabase(application).termuxDao(),
        application
    )
    private val runner = TermuxRunner.getInstance(application)
    private val backupManager = BackupRestoreManager(application)

    // Storage Access Framework (SAF) Repository & Runner
    val storageRepository = TermuxStorageRepository(application)
    val storageState: StateFlow<TermuxStorageState> = storageRepository.storageState
    val safScripts: StateFlow<List<TermuxScriptFile>> = storageRepository.scripts

    private val scriptRunner = TermuxScriptRunner.getInstance(application)
    val scriptRunState: StateFlow<ScriptRunState> = scriptRunner.runState

    val liveState: StateFlow<LiveExecutionState> = runner.liveState
    val settings: StateFlow<AppSettings> = repository.settings
    val allLogs: StateFlow<List<ExecutionLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>("ทั้งหมด")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _isTermuxDetected = MutableStateFlow(runner.isTermuxInstalled())
    val isTermuxDetected = _isTermuxDetected.asStateFlow()

    private val _showLiveLogDialog = MutableStateFlow(false)
    val showLiveLogDialog = _showLiveLogDialog.asStateFlow()

    // Filtered Room scripts
    val filteredScripts: StateFlow<List<TermuxScript>> = combine(
        repository.allScripts,
        _searchQuery,
        _selectedCategory
    ) { scripts, query, category ->
        scripts.filter { script ->
            val matchesQuery = query.isBlank() ||
                script.name.contains(query, ignoreCase = true) ||
                script.filename.contains(query, ignoreCase = true) ||
                script.content.contains(query, ignoreCase = true)
            val matchesCategory = category == null || category == "ทั้งหมด" ||
                (category == "รายการโปรด" && script.isFavorite) ||
                script.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All categories list
    val categories: StateFlow<List<String>> = repository.allScripts.combine(_searchQuery) { scripts, _ ->
        val cats = mutableListOf("ทั้งหมด", "รายการโปรด")
        cats.addAll(scripts.map { it.category }.distinct().filter { it.isNotBlank() })
        cats.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("ทั้งหมด", "รายการโปรด"))

    init {
        viewModelScope.launch {
            repository.ensureDefaultScripts()
            refreshTermuxDetection()
            // Auto-reconnect SAF tree on startup if persistable permission exists
            storageRepository.tryAutoReconnect()
        }
    }

    // --- SAF Operations ---

    fun connectSafTreeUri(uri: Uri) {
        viewModelScope.launch {
            storageRepository.connectTreeUri(uri)
        }
    }

    fun disconnectSaf() {
        storageRepository.disconnect()
    }

    fun refreshSafScripts() {
        viewModelScope.launch {
            storageRepository.refreshScripts()
        }
    }

    fun runSafScript(
        script: TermuxScriptFile,
        mode: com.example.termux.ExecutionMode = com.example.termux.ExecutionMode.AUTO
    ) {
        scriptRunner.runScript(script, mode)
    }

    fun sendInteractiveInput(input: String) {
        scriptRunner.sendInput(input)
    }

    fun stopRunningScript() {
        scriptRunner.stopExecution()
    }

    fun readSafScriptContent(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val contentResult = storageRepository.readScriptContent(uri)
            contentResult.onSuccess { content ->
                onResult(content)
            }.onFailure {
                onResult("")
            }
        }
    }

    fun saveSafScript(
        name: String,
        content: String,
        isNewFile: Boolean,
        existingUri: Uri?,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (isNewFile || existingUri == null) {
                val result = storageRepository.createNewScript(name, content)
                result.onSuccess {
                    onDone(true, "Script saved successfully.")
                }.onFailure {
                    onDone(false, it.message ?: "Failed to create script.")
                }
            } else {
                val result = storageRepository.writeScriptContent(existingUri, content)
                result.onSuccess {
                    onDone(true, "Script saved successfully.")
                }.onFailure {
                    onDone(false, it.message ?: "Failed to save script.")
                }
            }
        }
    }

    fun renameSafScript(script: TermuxScriptFile, newName: String) {
        viewModelScope.launch {
            storageRepository.renameScript(script, newName)
        }
    }

    fun deleteSafScript(script: TermuxScriptFile) {
        viewModelScope.launch {
            storageRepository.deleteScript(script)
        }
    }

    fun duplicateSafScript(script: TermuxScriptFile) {
        viewModelScope.launch {
            storageRepository.duplicateScript(script)
        }
    }

    // --- General App Operations ---

    fun refreshTermuxDetection() {
        _isTermuxDetected.value = runner.isTermuxInstalled()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun runScript(script: TermuxScript, forceSimulation: Boolean = false) {
        _showLiveLogDialog.value = true
        runner.executeScript(script, forceSimulation)
        TermuxAppWidgetProvider.updateAllWidgets(getApplication())
    }

    fun saveScript(script: TermuxScript) {
        viewModelScope.launch {
            if (script.id == 0L) {
                repository.insertScript(script)
            } else {
                repository.updateScript(script)
            }
            TermuxAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteScript(script: TermuxScript) {
        viewModelScope.launch {
            repository.deleteScript(script)
            TermuxAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleFavorite(script: TermuxScript) {
        viewModelScope.launch {
            repository.updateScript(script.copy(isFavorite = !script.isFavorite))
            TermuxAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleWidget(script: TermuxScript) {
        viewModelScope.launch {
            repository.updateScript(script.copy(showInWidget = !script.showInWidget))
            TermuxAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun openLiveLogDialog() {
        _showLiveLogDialog.value = true
    }

    fun dismissLiveLogDialog() {
        _showLiveLogDialog.value = false
    }

    // Backup & Restore
    fun generateBackupString(scripts: List<TermuxScript>): String {
        return backupManager.generateBackupJson(scripts, settings.value)
    }

    fun backupToUri(uri: Uri, scripts: List<TermuxScript>): Boolean {
        val json = generateBackupString(scripts)
        return backupManager.writeToUri(uri, json)
    }

    fun prepareGoogleDriveShareIntent(scripts: List<TermuxScript>): Intent {
        val json = generateBackupString(scripts)
        return backupManager.createShareIntentForGoogleDrive(json)
    }

    fun inspectBackup(uri: Uri): BackupPayload? {
        val json = backupManager.readFromUri(uri) ?: return null
        return backupManager.parseBackupJson(json)
    }

    fun performRestore(payload: BackupPayload, mode: RestoreMode, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val count = backupManager.performRestore(repository, payload, mode)
                TermuxAppWidgetProvider.updateAllWidgets(getApplication())
                onResult(true, "กู้คืนสำเร็จ $count สคริปต์")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "เกิดข้อผิดพลาดในการกู้คืน")
            }
        }
    }
}
