package com.example.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.TermuxRepository
import com.example.data.model.AppSettings
import com.example.data.model.TermuxScript
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPayload(
    val version: Int,
    val appName: String,
    val timestamp: Long,
    val timestampFormatted: String,
    val scriptCount: Int,
    val settings: AppSettings,
    val scripts: List<TermuxScript>
)

enum class RestoreMode {
    OVERWRITE,
    MERGE
}

class BackupRestoreManager(private val context: Context) {

    fun generateBackupJson(scripts: List<TermuxScript>, settings: AppSettings): String {
        val root = JSONObject()
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        root.put("version", 1)
        root.put("appName", "Run Termux By IDOL_CHAMP")
        root.put("timestamp", now)
        root.put("timestampFormatted", dateFormat.format(Date(now)))

        // Settings
        val settingsObj = JSONObject().apply {
            put("termuxShortcutsDir", settings.termuxShortcutsDir)
            put("defaultWorkDir", settings.defaultWorkDir)
            put("notifyOnFinish", settings.notifyOnFinish)
            put("notifyRunningStatus", settings.notifyRunningStatus)
            put("vibrateOnFinish", settings.vibrateOnFinish)
            put("autoOpenLogOnError", settings.autoOpenLogOnError)
            put("darkModeOption", settings.darkModeOption)
        }
        root.put("settings", settingsObj)

        // Scripts
        val scriptsArray = JSONArray()
        for (s in scripts) {
            val sObj = JSONObject().apply {
                put("name", s.name)
                put("filename", s.filename)
                put("scriptPath", s.scriptPath)
                put("content", s.content)
                put("arguments", s.arguments)
                put("workDir", s.workDir)
                put("runInBackground", s.runInBackground)
                put("category", s.category)
                put("isFavorite", s.isFavorite)
                put("showInWidget", s.showInWidget)
                put("iconName", s.iconName)
            }
            scriptsArray.put(sObj)
        }
        root.put("scripts", scriptsArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupPayload? {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val appName = root.optString("appName", "Run Termux")
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val timestampFormatted = root.optString("timestampFormatted", "")

            val settingsObj = root.optJSONObject("settings")
            val settings = if (settingsObj != null) {
                AppSettings(
                    termuxShortcutsDir = settingsObj.optString("termuxShortcutsDir", "/data/data/com.termux/files/home/.shortcuts/"),
                    defaultWorkDir = settingsObj.optString("defaultWorkDir", "/data/data/com.termux/files/home/"),
                    notifyOnFinish = settingsObj.optBoolean("notifyOnFinish", true),
                    notifyRunningStatus = settingsObj.optBoolean("notifyRunningStatus", true),
                    vibrateOnFinish = settingsObj.optBoolean("vibrateOnFinish", true),
                    autoOpenLogOnError = settingsObj.optBoolean("autoOpenLogOnError", true),
                    darkModeOption = settingsObj.optString("darkModeOption", "SYSTEM")
                )
            } else {
                AppSettings()
            }

            val scriptsArray = root.optJSONArray("scripts") ?: JSONArray()
            val scriptsList = mutableListOf<TermuxScript>()
            for (i in 0 until scriptsArray.length()) {
                val sObj = scriptsArray.getJSONObject(i)
                scriptsList.add(
                    TermuxScript(
                        name = sObj.optString("name", "สคริปต์สำรอง"),
                        filename = sObj.optString("filename", "script_$i.sh"),
                        scriptPath = sObj.optString("scriptPath", "/data/data/com.termux/files/home/.shortcuts/"),
                        content = sObj.optString("content", ""),
                        arguments = sObj.optString("arguments", ""),
                        workDir = sObj.optString("workDir", "/data/data/com.termux/files/home/.shortcuts/"),
                        runInBackground = sObj.optBoolean("runInBackground", true),
                        category = sObj.optString("category", "ทั่วไป"),
                        isFavorite = sObj.optBoolean("isFavorite", false),
                        showInWidget = sObj.optBoolean("showInWidget", true),
                        iconName = sObj.optString("iconName", "terminal")
                    )
                )
            }

            BackupPayload(
                version = version,
                appName = appName,
                timestamp = timestamp,
                timestampFormatted = timestampFormatted,
                scriptCount = scriptsList.size,
                settings = settings,
                scripts = scriptsList
            )
        } catch (_: Exception) {
            null
        }
    }

    fun writeToUri(uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun createShareIntentForGoogleDrive(backupJson: String): Intent {
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val fileName = "termux_shortcuts_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
        val backupFile = File(backupDir, fileName)
        backupFile.writeText(backupJson, Charsets.UTF_8)

        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "สำรองข้อมูล Termux Shortcuts - $fileName")
            putExtra(Intent.EXTRA_TEXT, "ไฟล์สำรองข้อมูลสำหรับแอพ Run Termux By IDOL_CHAMP")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun performRestore(
        repository: TermuxRepository,
        payload: BackupPayload,
        mode: RestoreMode
    ): Int {
        val dao = AppDatabase.getDatabase(context).termuxDao()
        if (mode == RestoreMode.OVERWRITE) {
            dao.clearAllScripts()
            dao.insertAllScripts(payload.scripts)
        } else {
            // MERGE
            for (script in payload.scripts) {
                dao.insertScript(script.copy(id = 0))
            }
        }
        repository.saveSettings(payload.settings)
        return payload.scripts.size
    }
}
