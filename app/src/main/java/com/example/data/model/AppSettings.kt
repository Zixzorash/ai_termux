package com.example.data.model

data class AppSettings(
    val termuxShortcutsDir: String = "/data/data/com.termux/files/home/.shortcuts/",
    val defaultWorkDir: String = "/data/data/com.termux/files/home/",
    val notifyOnFinish: Boolean = true,
    val notifyRunningStatus: Boolean = true,
    val vibrateOnFinish: Boolean = true,
    val autoOpenLogOnError: Boolean = true,
    val fallbackToLocalRunnerIfNoTermux: Boolean = true,
    val darkModeOption: String = "SYSTEM", // SYSTEM, DARK, LIGHT
    val defaultExecutionEngine: String = "IN_APP_DIRECT", // IN_APP_DIRECT (Direct with live reply) or TERMUX_SERVICE (Termux RUN_COMMAND)
    val autoOpenTerminalSheetOnRun: Boolean = true
)

data class LiveExecutionState(
    val isRunning: Boolean = false,
    val scriptId: Long = 0,
    val scriptName: String = "",
    val outputLogs: List<String> = emptyList(),
    val exitCode: Int? = null,
    val durationMs: Long = 0,
    val startTime: Long = 0,
    val statusText: String = "พร้อมใช้งาน"
)
