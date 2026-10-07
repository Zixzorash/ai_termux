package com.example.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import com.example.data.local.AppDatabase
import com.example.data.local.TermuxRepository
import com.example.data.model.ExecutionLog
import com.example.data.model.LiveExecutionState
import com.example.data.model.TermuxScript
import com.example.receiver.TermuxResultReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class TermuxRunner private constructor(private val context: Context) {

    private val repository = TermuxRepository(AppDatabase.getDatabase(context).termuxDao(), context)
    private val notificationHelper = TermuxNotificationHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _liveState = MutableStateFlow(LiveExecutionState())
    val liveState = _liveState.asStateFlow()

    companion object {
        const val TERMUX_PACKAGE_NAME = "com.termux"
        const val TERMUX_SERVICE_NAME = "com.termux.app.RunCommandService"
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

        @Volatile
        private var INSTANCE: TermuxRunner? = null

        fun getInstance(context: Context): TermuxRunner {
            return INSTANCE ?: synchronized(this) {
                val instance = TermuxRunner(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE_NAME, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun executeScript(script: TermuxScript, forceSimulation: Boolean = false) {
        scope.launch {
            val startTime = SystemClock.elapsedRealtime()
            val startTimestamp = System.currentTimeMillis()

            val initialLogs = mutableListOf(
                "🚀 [${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] เริ่มต้นการทำงาน...",
                "📁 สคริปต์: ${script.name} (${script.filename})",
                "📍 ตำแหน่งไฟล์: ${script.fullPath}",
                "📂 ไดเรกทอรีทำงาน: ${script.workDir}"
            )

            _liveState.value = LiveExecutionState(
                isRunning = true,
                scriptId = script.id,
                scriptName = script.name,
                outputLogs = initialLogs,
                startTime = startTime,
                statusText = "กำลังทำงาน..."
            )

            val settings = repository.settings.value
            if (settings.notifyRunningStatus) {
                notificationHelper.showRunningNotification(script.id, script.name, script.filename)
            }

            val termuxAvailable = isTermuxInstalled()

            if (termuxAvailable && !forceSimulation) {
                // Execute using official Termux RUN_COMMAND intent service
                runViaTermuxService(script, startTime, startTimestamp)
            } else {
                // Execute locally via internal shell runner or direct command runner
                runLocally(script, startTime, startTimestamp)
            }
        }
    }

    private suspend fun runViaTermuxService(script: TermuxScript, startTime: Long, startTimestamp: Long) {
        appendLog("📱 ตรวจพบแอพพลิเคชัน Termux ในเครื่อง")
        appendLog("⚡ ส่ง Intent คำสั่งไปยัง Termux:RunCommandService...")

        val callbackIntent = Intent(context, TermuxResultReceiver::class.java).apply {
            action = TermuxResultReceiver.ACTION_COMMAND_RESULT
            putExtra("EXTRA_SCRIPT_ID", script.id)
            putExtra("EXTRA_SCRIPT_NAME", script.name)
            putExtra("EXTRA_START_TIME", startTime)
            putExtra("EXTRA_TIMESTAMP", startTimestamp)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            script.id.toInt(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val termuxIntent = Intent(ACTION_RUN_COMMAND).apply {
            component = ComponentName(TERMUX_PACKAGE_NAME, TERMUX_SERVICE_NAME)
            putExtra(EXTRA_COMMAND_PATH, script.fullPath)
            if (script.arguments.isNotBlank()) {
                val argsArray = script.arguments.trim().split("\\s+".toRegex()).toTypedArray()
                putExtra(EXTRA_ARGUMENTS, argsArray)
            }
            putExtra(EXTRA_WORKDIR, script.workDir)
            putExtra(EXTRA_BACKGROUND, script.runInBackground)
            putExtra(EXTRA_SESSION_ACTION, "0")
            putExtra(EXTRA_PENDING_INTENT, pendingIntent)
        }

        try {
            context.startService(termuxIntent)
            appendLog("✅ ส่งคำสั่ง RUN_COMMAND ไปยัง Termux สำเร็จแล้ว!")
            appendLog("⏳ กำลังรอผลลัพธ์การทำงานจาก Termux Callback...")

            // We also record an initial pending/sent log
            val duration = SystemClock.elapsedRealtime() - startTime
            val log = ExecutionLog(
                scriptId = script.id,
                scriptName = script.name,
                commandExecuted = script.fullPath + if (script.arguments.isNotBlank()) " ${script.arguments}" else "",
                stdout = "คำสั่งถูกส่งไปยัง Termux Background Service แล้ว\nตำแหน่ง: ${script.fullPath}",
                stderr = "",
                exitCode = 0,
                durationMs = duration,
                timestamp = startTimestamp,
                executionMode = "Termux Service (com.termux.RUN_COMMAND)"
            )
            repository.insertLog(log)

            val updatedScript = script.copy(
                lastRunAt = startTimestamp,
                lastExitCode = 0,
                lastDurationMs = duration
            )
            repository.updateScript(updatedScript)

            // Let live state settle
            withContext(Dispatchers.Main) {
                _liveState.value = _liveState.value.copy(
                    isRunning = false,
                    exitCode = 0,
                    durationMs = duration,
                    statusText = "ส่งคำสั่งไปยัง Termux สำเร็จ"
                )
            }

            if (repository.settings.value.notifyOnFinish) {
                notificationHelper.showCompletedNotification(
                    scriptId = script.id,
                    scriptName = script.name,
                    exitCode = 0,
                    durationMs = duration,
                    vibrate = repository.settings.value.vibrateOnFinish
                )
            }

        } catch (e: Exception) {
            appendLog("❌ ไม่สามารถเรียก Termux Service: ${e.message}")
            appendLog("🔄 กำลังสลับไปรันผ่าน Local Execution...")
            runLocally(script, startTime, startTimestamp)
        }
    }

    private suspend fun runLocally(script: TermuxScript, startTime: Long, startTimestamp: Long) {
        appendLog("💻 รันคำสั่งผ่านระบบจำลองคำสั่งบนอุปกรณ์...")

        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()
        var exitCode = 0

        try {
            // Parse shell lines or commands
            val scriptLines = script.content.lines().filter { it.isNotBlank() && !it.startsWith("#") }
            val commandToRun = if (scriptLines.isNotEmpty()) scriptLines.joinToString(" && ") else "echo 'ไม่มีคำสั่งในสคริปต์'"

            appendLog("▶️ กำลังประมวลผลคำสั่ง:\n   $commandToRun")

            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", commandToRun))

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) {
                line?.let {
                    stdoutBuilder.append(it).append("\n")
                    appendLog("🟢 [OUT] $it")
                }
            }
            stdoutReader.close()

            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
            while (stderrReader.readLine().also { line = it } != null) {
                line?.let {
                    stderrBuilder.append(it).append("\n")
                    appendLog("🔴 [ERR] $it")
                }
            }
            stderrReader.close()

            exitCode = process.waitFor()

        } catch (e: Exception) {
            exitCode = 1
            val errorMsg = e.localizedMessage ?: "Unknown execution error"
            stderrBuilder.append(errorMsg).append("\n")
            appendLog("❌ ข้อผิดพลาด: $errorMsg")
        }

        val duration = SystemClock.elapsedRealtime() - startTime

        appendLog("🏁 คำสั่งเสร็จสิ้นด้วยรหัสทางออก: $exitCode (ใช้เวลา ${duration}ms)")

        val log = ExecutionLog(
            scriptId = script.id,
            scriptName = script.name,
            commandExecuted = script.content.take(150),
            stdout = stdoutBuilder.toString(),
            stderr = stderrBuilder.toString(),
            exitCode = exitCode,
            durationMs = duration,
            timestamp = startTimestamp,
            executionMode = if (isTermuxInstalled()) "Termux Local Shell" else "Local Shell Engine"
        )
        repository.insertLog(log)

        val updatedScript = script.copy(
            lastRunAt = startTimestamp,
            lastExitCode = exitCode,
            lastDurationMs = duration
        )
        repository.updateScript(updatedScript)

        withContext(Dispatchers.Main) {
            _liveState.value = _liveState.value.copy(
                isRunning = false,
                exitCode = exitCode,
                durationMs = duration,
                statusText = if (exitCode == 0) "ทำงานสำเร็จ (0)" else "ล้มเหลว ($exitCode)"
            )
        }

        if (repository.settings.value.notifyOnFinish) {
            notificationHelper.showCompletedNotification(
                scriptId = script.id,
                scriptName = script.name,
                exitCode = exitCode,
                durationMs = duration,
                vibrate = repository.settings.value.vibrateOnFinish
            )
        }
    }

    private suspend fun appendLog(logLine: String) {
        withContext(Dispatchers.Main) {
            val currentLogs = _liveState.value.outputLogs.toMutableList()
            currentLogs.add(logLine)
            _liveState.value = _liveState.value.copy(outputLogs = currentLogs)
        }
    }

    fun handleTermuxResult(
        scriptId: Long,
        scriptName: String,
        stdout: String,
        stderr: String,
        exitCode: Int,
        startTime: Long,
        startTimestamp: Long
    ) {
        scope.launch {
            val duration = if (startTime > 0) SystemClock.elapsedRealtime() - startTime else 500L
            appendLog("📥 ได้รับผลลัพธ์จาก Termux Callback:")
            if (stdout.isNotBlank()) appendLog("🟢 [OUT] $stdout")
            if (stderr.isNotBlank()) appendLog("🔴 [ERR] $stderr")
            appendLog("🏁 รหัสสถานะ: $exitCode (ใช้เวลา ${duration}ms)")

            val log = ExecutionLog(
                scriptId = scriptId,
                scriptName = scriptName,
                commandExecuted = "Termux Service Execution",
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                durationMs = duration,
                timestamp = startTimestamp,
                executionMode = "Termux Service Callback"
            )
            repository.insertLog(log)

            val currentScript = repository.getScriptById(scriptId)
            if (currentScript != null) {
                repository.updateScript(
                    currentScript.copy(
                        lastRunAt = startTimestamp,
                        lastExitCode = exitCode,
                        lastDurationMs = duration
                    )
                )
            }

            withContext(Dispatchers.Main) {
                _liveState.value = _liveState.value.copy(
                    isRunning = false,
                    exitCode = exitCode,
                    durationMs = duration,
                    statusText = if (exitCode == 0) "ทำงานสำเร็จ (0)" else "ล้มเหลว ($exitCode)"
                )
            }

            if (repository.settings.value.notifyOnFinish) {
                notificationHelper.showCompletedNotification(
                    scriptId = scriptId,
                    scriptName = scriptName,
                    exitCode = exitCode,
                    durationMs = duration,
                    vibrate = repository.settings.value.vibrateOnFinish
                )
            }
        }
    }

    fun resetLiveState() {
        _liveState.value = LiveExecutionState()
    }
}
