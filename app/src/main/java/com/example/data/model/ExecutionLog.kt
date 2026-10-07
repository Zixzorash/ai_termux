package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "execution_logs")
data class ExecutionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scriptId: Long,
    val scriptName: String,
    val commandExecuted: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val executionMode: String = "Termux Service"
)
