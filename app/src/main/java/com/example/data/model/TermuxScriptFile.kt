package com.example.data.model

import android.net.Uri

data class TermuxScriptFile(
    val name: String,
    val uri: Uri,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val relativePath: String = "",
    val logicalPosixPath: String = "",
    val canWrite: Boolean = true,
    val canDelete: Boolean = true
) {
    /**
     * Checks if the script name is safe from path traversal and shell injection.
     */
    val isNameSafe: Boolean
        get() {
            if (name.contains("..") || name.contains("/") || name.contains("\\")) return false
            if (name.contains(";") || name.contains("&") || name.contains("|") || name.contains("$")) return false
            if (name.contains("`") || name.contains(">") || name.contains("<")) return false
            return name.endsWith(".sh", ignoreCase = true)
        }
}
