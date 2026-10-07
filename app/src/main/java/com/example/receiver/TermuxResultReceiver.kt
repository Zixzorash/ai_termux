package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.TermuxRunner
import com.example.termux.TermuxScriptRunner

class TermuxResultReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMMAND_RESULT = "com.aistudio.runtermux.ACTION_COMMAND_RESULT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COMMAND_RESULT) {
            val scriptId = intent.getLongExtra("EXTRA_SCRIPT_ID", 0L)
            val scriptName = intent.getStringExtra("EXTRA_SCRIPT_NAME") ?: "คำสั่ง Termux"
            val startTime = intent.getLongExtra("EXTRA_START_TIME", 0L)
            val timestamp = intent.getLongExtra("EXTRA_TIMESTAMP", System.currentTimeMillis())

            val stdout = intent.getStringExtra("stdout") ?: intent.getStringExtra("result_stdout") ?: ""
            val stderr = intent.getStringExtra("stderr") ?: intent.getStringExtra("result_stderr") ?: ""
            val exitCode = intent.getIntExtra("exitCode", intent.getIntExtra("result_code", 0))

            TermuxScriptRunner.getInstance(context).handleExecutionResult(
                scriptId = scriptId,
                scriptName = scriptName,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                startTime = startTime,
                timestamp = timestamp
            )
            TermuxRunner.getInstance(context).handleTermuxResult(
                scriptId = scriptId,
                scriptName = scriptName,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                startTime = startTime,
                startTimestamp = timestamp
            )
        }
    }
}
