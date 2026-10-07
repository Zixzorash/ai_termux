package com.example.termux

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.data.local.AppDatabase
import com.example.data.model.ExecutionLog
import com.example.data.model.TermuxScriptFile
import com.example.receiver.TermuxResultReceiver
import com.example.service.TermuxNotificationHelper
import com.example.utils.SafUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

enum class ExecutionMode {
    AUTO,
    TERMUX_SERVICE,
    IN_APP_DIRECT
}

data class ScriptRunState(
    val isRunning: Boolean = false,
    val scriptName: String = "",
    val logicalPath: String = "",
    val outputLogs: List<String> = emptyList(),
    val exitCode: Int? = null,
    val durationMs: Long = 0,
    val statusMessage: String = "",
    val supportsLiveOutput: Boolean = true,
    val activeScriptFile: TermuxScriptFile? = null,
    val isWaitingForInput: Boolean = false
)

class TermuxScriptRunner private constructor(private val context: Context) {

    private val commandManager = TermuxCommandManager(context)
    private val notificationHelper = TermuxNotificationHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dao = AppDatabase.getDatabase(context).termuxDao()

    private val _runState = MutableStateFlow(ScriptRunState())
    val runState: StateFlow<ScriptRunState> = _runState.asStateFlow()

    @Volatile
    private var activeProcess: Process? = null
    @Volatile
    private var activeProcessWriter: OutputStreamWriter? = null

    companion object {
        @Volatile
        private var INSTANCE: TermuxScriptRunner? = null

        fun getInstance(context: Context): TermuxScriptRunner {
            return INSTANCE ?: synchronized(this) {
                val instance = TermuxScriptRunner(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Executes a Termux script file.
     * Can execute via Direct In-App Runner (with interactive live response) or Termux RUN_COMMAND service.
     */
    fun runScript(script: TermuxScriptFile, mode: ExecutionMode = ExecutionMode.AUTO) {
        scope.launch {
            val startTime = SystemClock.elapsedRealtime()
            val startTimestamp = System.currentTimeMillis()

            // 1. Security validation: Anti-traversal & Anti-injection check
            if (!script.isNameSafe) {
                withContext(Dispatchers.Main) {
                    _runState.value = ScriptRunState(
                        isRunning = false,
                        scriptName = script.name,
                        logicalPath = script.logicalPosixPath,
                        outputLogs = listOf(
                            "❌ ปฏิเสธการรันคำสั่ง: ชื่อไฟล์สคริปต์ไม่ปลอดภัย (อาจมี ../ หรือ shell injection characters)",
                            "ชื่อไฟล์: ${script.name}"
                        ),
                        exitCode = -1,
                        statusMessage = "ความปลอดภัย: ปฏิเสธการรันชื่อไฟล์ที่อาจก่อให้เกิดอันตราย",
                        activeScriptFile = script,
                        isWaitingForInput = false
                    )
                }
                return@launch
            }

            // 2. Prepare initial state & notification
            val initialLogs = mutableListOf(
                "🚀 [${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] เริ่มต้นการรันสคริปต์...",
                "📜 สคริปต์: ${script.name}",
                "📍 Logical Path: ${script.logicalPosixPath}",
                "⚡ กำลังเริ่มโปรเซส (พิมพ์ตอบกลับสคริปต์สดได้ทันที)..."
            )

            withContext(Dispatchers.Main) {
                _runState.value = ScriptRunState(
                    isRunning = true,
                    scriptName = script.name,
                    logicalPath = script.logicalPosixPath,
                    outputLogs = initialLogs,
                    statusMessage = "กำลังประมวลผลคำสั่ง...",
                    activeScriptFile = script,
                    isWaitingForInput = false
                )
            }

            notificationHelper.showRunningNotification(
                scriptId = script.name.hashCode().toLong(),
                scriptName = script.name,
                commandPreview = script.logicalPosixPath
            )

            // 3. Execution routing
            when (mode) {
                ExecutionMode.IN_APP_DIRECT -> {
                    executeDirectlyInApp(script, startTime, startTimestamp)
                }
                ExecutionMode.TERMUX_SERVICE -> {
                    executeViaRunCommandService(script, startTime, startTimestamp, fallbackToDirect = false)
                }
                ExecutionMode.AUTO -> {
                    // In AUTO mode: Check preferences or default to IN_APP_DIRECT for full interactive support
                    val prefs = context.getSharedPreferences("termux_runner_prefs", Context.MODE_PRIVATE)
                    val configuredEngine = prefs.getString("default_exec_engine", "IN_APP_DIRECT") ?: "IN_APP_DIRECT"

                    if (configuredEngine == "TERMUX_SERVICE") {
                        executeViaRunCommandService(script, startTime, startTimestamp, fallbackToDirect = true)
                    } else {
                        executeDirectlyInApp(script, startTime, startTimestamp)
                    }
                }
            }
        }
    }

    /**
     * Executes the script via Termux RUN_COMMAND service.
     */
    private suspend fun executeViaRunCommandService(
        script: TermuxScriptFile,
        startTime: Long,
        startTimestamp: Long,
        fallbackToDirect: Boolean
    ) {
        val termuxInfo = commandManager.checkTermuxInstallation()
        if (termuxInfo is TermuxVersionInfo.NotInstalled) {
            appendLog("⚠️ ไม่พบแอพ Termux ในเครื่อง สลับไปรันโดยตรงในแอพทันที...")
            executeDirectlyInApp(script, startTime, startTimestamp)
            return
        }

        appendLog("⚡ ส่ง Intent ไปยัง Termux RunCommandService...")

        val scriptId = script.name.hashCode().toLong()

        val callbackIntent = Intent(context, TermuxResultReceiver::class.java).apply {
            action = TermuxResultReceiver.ACTION_COMMAND_RESULT
            putExtra("EXTRA_SCRIPT_ID", scriptId)
            putExtra("EXTRA_SCRIPT_NAME", script.name)
            putExtra("EXTRA_START_TIME", startTime)
            putExtra("EXTRA_TIMESTAMP", startTimestamp)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (scriptId and 0xFFFF).toInt(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val bashPath = "/data/data/com.termux/files/usr/bin/bash"
        val scriptFullPath = "/data/data/com.termux/files/home/${script.relativePath}"

        val termuxIntent = Intent(TermuxCommandManager.ACTION_RUN_COMMAND).apply {
            component = ComponentName(TermuxCommandManager.TERMUX_PACKAGE, TermuxCommandManager.RUN_COMMAND_SERVICE)
            putExtra(TermuxCommandManager.EXTRA_RUN_COMMAND_PATH, bashPath)
            putExtra(TermuxCommandManager.EXTRA_RUN_COMMAND_ARGUMENTS, arrayOf(scriptFullPath))
            putExtra(TermuxCommandManager.EXTRA_RUN_COMMAND_WORKDIR, "/data/data/com.termux/files/home")
            putExtra(TermuxCommandManager.EXTRA_RUN_COMMAND_BACKGROUND, true)
            putExtra(TermuxCommandManager.EXTRA_RUN_COMMAND_SESSION_ACTION, "0")
            putExtra(TermuxCommandManager.EXTRA_RUN_COMMAND_PENDING_INTENT, pendingIntent)
        }

        try {
            context.startService(termuxIntent)
            appendLog("✅ ส่งคำสั่ง RUN_COMMAND ไปยัง Termux สำเร็จแล้ว!")
            appendLog("⏳ กำลังรอผลลัพธ์จาก Termux Callback...")

            val duration = SystemClock.elapsedRealtime() - startTime
            withContext(Dispatchers.Main) {
                _runState.value = _runState.value.copy(
                    isRunning = false,
                    durationMs = duration,
                    statusMessage = "ส่งคำสั่งไปยัง Termux สำเร็จ (รอ Callback)",
                    supportsLiveOutput = true,
                    isWaitingForInput = false
                )
            }
        } catch (e: Exception) {
            appendLog("⚠️ ไม่สามารถรันผ่าน Termux Service: ${e.message}")
            if (fallbackToDirect) {
                appendLog("🔄 กำลังสลับไปรันสคริปต์โดยตรงในแอพนี้ให้ทันที...")
                executeDirectlyInApp(script, startTime, startTimestamp)
            } else {
                finishWithError(script, -1, "ไม่สามารถเรียก Termux Service: ${e.message}")
            }
        }
    }

    /**
     * Executes the script directly inside this app process runner,
     * reading contents from SAF and streaming real stdout & stderr line by line.
     * Fully supports interactive STDIN responses (e.g. read -p "Enter filename: ").
     */
    suspend fun executeDirectlyInApp(
        script: TermuxScriptFile,
        startTime: Long = SystemClock.elapsedRealtime(),
        startTimestamp: Long = System.currentTimeMillis()
    ) {
        appendLog("⚙️ เริ่มต้นรันสคริปต์โดยตรงในแอพ (Direct In-App Engine)...")

        val scriptContent = try {
            SafUtils.readDocumentContent(context.contentResolver, script.uri)
        } catch (e: Exception) {
            appendLog("❌ ไม่สามารถอ่านเนื้อหาไฟล์จาก SAF: ${e.message}")
            finishWithError(script, -1, "Cannot read SAF document")
            return
        }

        if (scriptContent.isBlank()) {
            appendLog("⚠️ ไฟล์สคริปต์ว่างเปล่า ไม่มีคำสั่งให้ทำงาน")
            finishWithError(script, 0, "Empty script")
            return
        }

        // Prepare script content with Bash compatibility layer (supporting read -p on mksh/Android sh)
        val compatibleScriptContent = injectBashCompatibility(scriptContent)

        // Write to temporary executable cache file in app private cache
        val scriptDir = File(context.cacheDir, "script_exec_cache").apply { mkdirs() }
        val tempScriptFile = File(scriptDir, "run_${System.currentTimeMillis()}.sh")
        try {
            tempScriptFile.writeText(compatibleScriptContent, Charsets.UTF_8)
            tempScriptFile.setExecutable(true, false)

            appendLog("▶️ กำลังประมวลผล Shell Script (พร้อมรับคำตอบผ่านแอพโดยตรง)...")

            val pb = ProcessBuilder("sh", tempScriptFile.absolutePath)
                .directory(context.filesDir)

            pb.environment().apply {
                put("HOME", context.filesDir.absolutePath)
                put("TMPDIR", context.cacheDir.absolutePath)
                put("TERM", "xterm-256color")
                put("LANG", "en_US.UTF-8")
                put("LC_ALL", "en_US.UTF-8")
                put("PATH", "/system/bin:/system/xbin:/vendor/bin:/apex/com.android.runtime/bin")
            }

            val process = pb.start()
            activeProcess = process
            val writer = OutputStreamWriter(process.outputStream, Charsets.UTF_8)
            activeProcessWriter = writer

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            // Read STDOUT in real-time supporting unbuffered prompts (e.g. read -p "Enter filename: ")
            val stdoutJob = scope.launch(Dispatchers.IO) {
                val reader = InputStreamReader(process.inputStream, Charsets.UTF_8)
                val charBuffer = CharArray(512)
                val lineBuffer = StringBuilder()
                var readCount: Int
                while (reader.read(charBuffer).also { readCount = it } != -1) {
                    for (i in 0 until readCount) {
                        val c = charBuffer[i]
                        if (c == '\n' || c == '\r') {
                            if (lineBuffer.isNotEmpty()) {
                                val line = lineBuffer.toString()
                                stdoutBuilder.append(line).append("\n")
                                appendLog("🟢 [OUT] $line")
                                lineBuffer.clear()
                            }
                        } else {
                            lineBuffer.append(c)
                        }
                    }
                    if (lineBuffer.isNotEmpty() && !reader.ready()) {
                        val partial = lineBuffer.toString()
                        stdoutBuilder.append(partial)
                        appendLog("🟢 [PROMPT] $partial")
                        withContext(Dispatchers.Main) {
                            _runState.value = _runState.value.copy(isWaitingForInput = true)
                        }
                        lineBuffer.clear()
                    }
                }
                if (lineBuffer.isNotEmpty()) {
                    val line = lineBuffer.toString()
                    stdoutBuilder.append(line).append("\n")
                    appendLog("🟢 [OUT] $line")
                }
                reader.close()
            }

            // Read STDERR in real-time
            val stderrJob = scope.launch(Dispatchers.IO) {
                val reader = BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        stderrBuilder.append(it).append("\n")
                        appendLog("🔴 [ERR] $it")
                    }
                }
                reader.close()
            }

            stdoutJob.join()
            stderrJob.join()

            val exitCode = process.waitFor()
            val duration = SystemClock.elapsedRealtime() - startTime

            appendLog("🏁 สคริปต์ทำงานเสร็จสิ้นด้วยรหัสสถานะ: $exitCode (ใช้เวลา ${duration}ms)")

            withContext(Dispatchers.Main) {
                _runState.value = _runState.value.copy(
                    isRunning = false,
                    exitCode = exitCode,
                    durationMs = duration,
                    statusMessage = if (exitCode == 0) "ทำงานสำเร็จ (0)" else "ล้มเหลว ($exitCode)",
                    supportsLiveOutput = true,
                    isWaitingForInput = false
                )
            }

            // Save to execution logs
            dao.insertLog(
                ExecutionLog(
                    scriptId = script.name.hashCode().toLong(),
                    scriptName = script.name,
                    commandExecuted = "sh ${script.name}",
                    stdout = stdoutBuilder.toString(),
                    stderr = stderrBuilder.toString(),
                    exitCode = exitCode,
                    durationMs = duration,
                    timestamp = startTimestamp,
                    executionMode = "Direct In-App Shell"
                )
            )

            notificationHelper.showCompletedNotification(
                scriptId = script.name.hashCode().toLong(),
                scriptName = script.name,
                exitCode = exitCode,
                durationMs = duration
            )

        } catch (e: Exception) {
            appendLog("❌ เกิดข้อผิดพลาดขณะรันคำสั่ง: ${e.message}")
            finishWithError(script, -1, e.message ?: "Execution error")
        } finally {
            activeProcess = null
            activeProcessWriter = null
            tempScriptFile.delete()
        }
    }

    /**
     * Sends interactive STDIN input (e.g. filename, answer, text) to the running script process.
     */
    fun sendInput(input: String) {
        scope.launch(Dispatchers.IO) {
            val proc = activeProcess
            val writer = activeProcessWriter
            if (proc != null && proc.isAlive && writer != null) {
                try {
                    writer.write(input + "\n")
                    writer.flush()
                    appendLog("💬 [IN] $input")
                    withContext(Dispatchers.Main) {
                        _runState.value = _runState.value.copy(isWaitingForInput = false)
                    }
                } catch (e: Exception) {
                    appendLog("⚠️ ข้อผิดพลาดในการส่ง Input: ${e.message}")
                }
            } else {
                appendLog("⚠️ ไม่มีโปรเซสที่กำลังรอรับ Input ในขณะนี้")
            }
        }
    }

    /**
     * Terminates the active in-app script process if running.
     */
    fun stopExecution() {
        scope.launch {
            try {
                activeProcess?.destroyForcibly()
                activeProcess = null
                activeProcessWriter = null
                appendLog("⏹️ ยกเลิกการทำงานของสคริปต์แล้ว")
                withContext(Dispatchers.Main) {
                    _runState.value = _runState.value.copy(
                        isRunning = false,
                        statusMessage = "ยกเลิกการทำงานแล้ว",
                        isWaitingForInput = false
                    )
                }
            } catch (e: Exception) {
                appendLog("⚠️ ไม่สามารถยกเลิกโปรเซส: ${e.message}")
            }
        }
    }

    private suspend fun appendLog(logLine: String) {
        withContext(Dispatchers.Main) {
            val current = _runState.value.outputLogs.toMutableList()
            current.add(logLine)
            _runState.value = _runState.value.copy(outputLogs = current)
        }
    }

    private suspend fun finishWithError(script: TermuxScriptFile, exitCode: Int, reason: String) {
        withContext(Dispatchers.Main) {
            _runState.value = _runState.value.copy(
                isRunning = false,
                exitCode = exitCode,
                statusMessage = reason,
                isWaitingForInput = false
            )
        }
        notificationHelper.cancelNotification(script.name.hashCode().toLong())
    }

    fun handleExecutionResult(
        scriptId: Long,
        scriptName: String,
        stdout: String,
        stderr: String,
        exitCode: Int,
        startTime: Long,
        timestamp: Long
    ) {
        scope.launch {
            val duration = if (startTime > 0) SystemClock.elapsedRealtime() - startTime else 250L

            appendLog("📥 ได้รับผลลัพธ์จาก Termux Callback:")
            if (stdout.isNotBlank()) appendLog("🟢 [STDOUT]\n$stdout")
            if (stderr.isNotBlank()) appendLog("🔴 [STDERR]\n$stderr")
            appendLog("🏁 รหัสสถานะ (Exit Code): $exitCode (ใช้เวลา ${duration}ms)")

            withContext(Dispatchers.Main) {
                _runState.value = _runState.value.copy(
                    isRunning = false,
                    exitCode = exitCode,
                    durationMs = duration,
                    statusMessage = if (exitCode == 0) "ทำงานสำเร็จ (0)" else "ล้มเหลว ($exitCode)",
                    isWaitingForInput = false
                )
            }

            dao.insertLog(
                ExecutionLog(
                    scriptId = scriptId,
                    scriptName = scriptName,
                    commandExecuted = "com.termux.RUN_COMMAND",
                    stdout = stdout,
                    stderr = stderr,
                    exitCode = exitCode,
                    durationMs = duration,
                    timestamp = timestamp,
                    executionMode = "Termux RUN_COMMAND Callback"
                )
            )

            notificationHelper.showCompletedNotification(
                scriptId = scriptId,
                scriptName = scriptName,
                exitCode = exitCode,
                durationMs = duration
            )
        }
    }

    fun resetRunState() {
        _runState.value = ScriptRunState()
    }

    /**
     * Injects a Bash-compatibility layer into user shell scripts for Android mksh/sh environments.
     * This adds support for `read -p "Prompt: " var` and common bash extensions without crashing.
     */
    private fun injectBashCompatibility(originalContent: String): String {
        val compatLayer = buildString {
            appendLine("# === AI Studio Bash Compatibility Layer for Android Shell (mksh/sh) ===")
            appendLine("read() {")
            appendLine("    _prompt=\"\"")
            appendLine("    _raw=\"\"")
            appendLine("    _vars=\"\"")
            appendLine("    while [ $# -gt 0 ]; do")
            appendLine("        case \"$1\" in")
            appendLine("            -p)")
            appendLine("                _prompt=\"$2\"")
            appendLine("                shift 2 2>/dev/null || shift")
            appendLine("                ;;")
            appendLine("            -r)")
            appendLine("                _raw=\"-r\"")
            appendLine("                shift")
            appendLine("                ;;")
            appendLine("            -s|-e)")
            appendLine("                shift")
            appendLine("                ;;")
            appendLine("            -n|-t|-u|-d)")
            appendLine("                shift 2 2>/dev/null || shift")
            appendLine("                ;;")
            appendLine("            -*)")
            appendLine("                shift")
            appendLine("                ;;")
            appendLine("            *)")
            appendLine("                _vars=\"\$_vars \$1\"")
            appendLine("                shift")
            appendLine("                ;;")
            appendLine("        esac")
            appendLine("    done")
            appendLine("    if [ -n \"\$_prompt\" ]; then")
            appendLine("        printf \"%b\" \"\$_prompt\"")
            appendLine("    fi")
            appendLine("    if [ -z \"\$_vars\" ]; then")
            appendLine("        command read \$_raw REPLY")
            appendLine("    else")
            appendLine("        command read \$_raw \$_vars")
            appendLine("    fi")
            appendLine("}")
            appendLine("# ================================================================")
        }

        val lines = originalContent.lines()
        return if (lines.isNotEmpty() && lines[0].startsWith("#!")) {
            // Keep original shebang on first line
            lines[0] + "\n" + compatLayer + lines.drop(1).joinToString("\n")
        } else {
            "#!/system/bin/sh\n" + compatLayer + originalContent
        }
    }
}
