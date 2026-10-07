package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppSettings
import com.example.data.model.ExecutionLog
import com.example.data.model.TermuxScript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TermuxRepository(
    private val dao: TermuxDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("termux_runner_prefs", Context.MODE_PRIVATE)

    val allScripts: Flow<List<TermuxScript>> = dao.getAllScripts()
    val widgetScripts: Flow<List<TermuxScript>> = dao.getWidgetScripts()
    val allLogs: Flow<List<ExecutionLog>> = dao.getAllLogs()

    private val _settings = MutableStateFlow(loadSettings())
    val settings = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            termuxShortcutsDir = prefs.getString("shortcuts_dir", "/data/data/com.termux/files/home/.shortcuts/")
                ?: "/data/data/com.termux/files/home/.shortcuts/",
            defaultWorkDir = prefs.getString("work_dir", "/data/data/com.termux/files/home/")
                ?: "/data/data/com.termux/files/home/",
            notifyOnFinish = prefs.getBoolean("notify_finish", true),
            notifyRunningStatus = prefs.getBoolean("notify_running", true),
            vibrateOnFinish = prefs.getBoolean("vibrate_finish", true),
            autoOpenLogOnError = prefs.getBoolean("auto_open_log_error", true),
            fallbackToLocalRunnerIfNoTermux = prefs.getBoolean("fallback_local", true),
            darkModeOption = prefs.getString("dark_mode", "SYSTEM") ?: "SYSTEM",
            defaultExecutionEngine = prefs.getString("default_exec_engine", "IN_APP_DIRECT") ?: "IN_APP_DIRECT",
            autoOpenTerminalSheetOnRun = prefs.getBoolean("auto_open_terminal_sheet", true)
        )
    }

    suspend fun saveSettings(newSettings: AppSettings) {
        prefs.edit()
            .putString("shortcuts_dir", newSettings.termuxShortcutsDir)
            .putString("work_dir", newSettings.defaultWorkDir)
            .putBoolean("notify_finish", newSettings.notifyOnFinish)
            .putBoolean("notify_running", newSettings.notifyRunningStatus)
            .putBoolean("vibrate_finish", newSettings.vibrateOnFinish)
            .putBoolean("auto_open_log_error", newSettings.autoOpenLogOnError)
            .putBoolean("fallback_local", newSettings.fallbackToLocalRunnerIfNoTermux)
            .putString("dark_mode", newSettings.darkModeOption)
            .putString("default_exec_engine", newSettings.defaultExecutionEngine)
            .putBoolean("auto_open_terminal_sheet", newSettings.autoOpenTerminalSheetOnRun)
            .apply()
        _settings.value = newSettings
    }

    suspend fun ensureDefaultScripts() {
        if (dao.getScriptCount() == 0) {
            dao.insertAllScripts(DefaultScripts.getDefaultScripts())
        }
    }

    suspend fun getScriptById(id: Long): TermuxScript? = dao.getScriptById(id)

    suspend fun insertScript(script: TermuxScript): Long = dao.insertScript(script)

    suspend fun updateScript(script: TermuxScript) = dao.updateScript(script)

    suspend fun deleteScript(script: TermuxScript) {
        dao.deleteScript(script)
        dao.deleteLogsByScript(script.id)
    }

    suspend fun deleteScriptById(id: Long) {
        dao.deleteScriptById(id)
        dao.deleteLogsByScript(id)
    }

    suspend fun insertLog(log: ExecutionLog): Long = dao.insertLog(log)

    suspend fun clearLogs() = dao.clearAllLogs()

    suspend fun getAllScriptsSync(): List<TermuxScript> {
        val list = mutableListOf<TermuxScript>()
        // From sync count or direct query
        return dao.getWidgetScriptsSync()
    }
}
