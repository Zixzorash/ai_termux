package com.example.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SafUtils {

    const val MIME_SHELL_SCRIPT = "application/x-sh"
    const val MIME_TEXT_PLAIN = "text/plain"
    const val MIME_OCTET_STREAM = "application/octet-stream"

    /**
     * Checks if the given URI has an active persisted permission in ContentResolver.
     */
    fun hasPersistedPermission(context: Context, uri: Uri): Boolean {
        val persistedPermissions: List<UriPermission> = context.contentResolver.persistedUriPermissions
        return persistedPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
    }

    /**
     * Finds or resolves the target folder (such as .shortcuts) within a user-selected Tree.
     * Note: .shortcuts is a hidden directory (starts with .), so we must NOT filter out hidden folders.
     */
    fun resolveTargetShortcutsDirectory(treeDoc: DocumentFile): DocumentFile {
        val rootName = treeDoc.name ?: ""

        // If the user already picked the .shortcuts folder directly
        if (rootName.equals(".shortcuts", ignoreCase = true) || rootName.endsWith(".shortcuts")) {
            return treeDoc
        }

        // Check if there is a child directory named .shortcuts inside
        val childFiles = treeDoc.listFiles()
        val shortcutsChild = childFiles.firstOrNull { child ->
            child.isDirectory && (child.name?.equals(".shortcuts", ignoreCase = true) == true)
        }

        if (shortcutsChild != null) {
            return shortcutsChild
        }

        // Return treeDoc as fallback so scripts can be listed and managed in the selected directory
        return treeDoc
    }

    /**
     * Checks if a document represents a shell script file (*.sh or shell script MIME).
     */
    fun isShellScript(file: DocumentFile): Boolean {
        if (!file.isFile) return false
        val name = file.name ?: return false
        return name.endsWith(".sh", ignoreCase = true)
    }

    /**
     * Safely reads file contents as UTF-8 string using ContentResolver.
     * Preserves all shell syntax, newlines, quotes, heredocs, and shell variables.
     */
    fun readDocumentContent(contentResolver: ContentResolver, uri: Uri): String {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } ?: throw IllegalStateException("Cannot open input stream for $uri")
    }

    /**
     * Safely writes file contents as UTF-8 string using ContentResolver.
     */
    fun writeDocumentContent(contentResolver: ContentResolver, uri: Uri, content: String) {
        contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                writer.write(content)
                writer.flush()
            }
        } ?: throw IllegalStateException("Cannot open output stream for $uri")
    }

    /**
     * Formats bytes into a human-readable size string (B, KB, MB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }

    /**
     * Formats timestamp into a localized date-time string.
     */
    fun formatLastModified(timestamp: Long): String {
        if (timestamp <= 0) return "ไม่ระบุ"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Computes human-readable display path for a tree URI.
     */
    fun getDisplayPath(uri: Uri): String {
        val decoded = Uri.decode(uri.toString())
        return when {
            decoded.contains(".shortcuts") -> "/data/data/com.termux/files/home/.shortcuts"
            decoded.contains("/home") -> "/data/data/com.termux/files/home"
            else -> {
                val docId = try {
                    DocumentsContract.getTreeDocumentId(uri)
                } catch (_: Exception) {
                    uri.lastPathSegment ?: uri.toString()
                }
                "Termux: $docId"
            }
        }
    }
}
