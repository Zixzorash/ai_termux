package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "termux_scripts")
data class TermuxScript(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filename: String,
    val scriptPath: String = "/data/data/com.termux/files/home/.shortcuts/",
    val content: String,
    val arguments: String = "",
    val workDir: String = "/data/data/com.termux/files/home/.shortcuts/",
    val runInBackground: Boolean = true,
    val category: String = "ทั่วไป",
    val isFavorite: Boolean = false,
    val showInWidget: Boolean = true,
    val iconName: String = "terminal",
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val lastExitCode: Int? = null,
    val lastDurationMs: Long? = null
) {
    val fullPath: String
        get() {
            val dir = if (scriptPath.endsWith("/")) scriptPath else "$scriptPath/"
            return if (filename.startsWith("/")) filename else "$dir$filename"
        }
}
