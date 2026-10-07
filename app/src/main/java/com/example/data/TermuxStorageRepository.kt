package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.TermuxScriptFile
import com.example.utils.SafUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class TermuxStorageState {
    object Idle : TermuxStorageState()
    object Connecting : TermuxStorageState()
    data class Disconnected(val reason: String? = null) : TermuxStorageState()
    data class Connected(
        val treeUri: Uri,
        val targetDirName: String,
        val displayPath: String,
        val isInsideShortcuts: Boolean,
        val canWrite: Boolean
    ) : TermuxStorageState()
    data class PermissionRevoked(val reason: String) : TermuxStorageState()
}

class TermuxStorageRepository(private val context: Context) {

    private val preferences = TermuxPreferences(context)
    private val contentResolver = context.contentResolver

    private val _storageState = MutableStateFlow<TermuxStorageState>(TermuxStorageState.Idle)
    val storageState: StateFlow<TermuxStorageState> = _storageState.asStateFlow()

    private val _scripts = MutableStateFlow<List<TermuxScriptFile>>(emptyList())
    val scripts: StateFlow<List<TermuxScriptFile>> = _scripts.asStateFlow()

    private var currentTargetDirDoc: DocumentFile? = null

    /**
     * Attempts to auto-reconnect using saved persisted URI permission on app startup.
     */
    suspend fun tryAutoReconnect(): Boolean = withContext(Dispatchers.IO) {
        val savedUri = preferences.treeUri ?: run {
            _storageState.value = TermuxStorageState.Disconnected("Termux folder is not connected.")
            return@withContext false
        }

        // Verify if permission is still persisted in system ContentResolver
        val hasPermission = SafUtils.hasPersistedPermission(context, savedUri)
        if (!hasPermission) {
            _storageState.value = TermuxStorageState.PermissionRevoked(
                "Storage permission was revoked. Please reconnect the Termux folder."
            )
            return@withContext false
        }

        return@withContext inspectAndConnectTree(savedUri)
    }

    /**
     * Connects a new tree URI selected by the user via ACTION_OPEN_DOCUMENT_TREE.
     */
    suspend fun connectTreeUri(treeUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Take persistable permission
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(treeUri, takeFlags)

            // Save to preferences
            preferences.treeUri = treeUri
            preferences.lastConnectedTime = System.currentTimeMillis()

            val success = inspectAndConnectTree(treeUri)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Cannot access selected directory"))
            }
        } catch (e: SecurityException) {
            _storageState.value = TermuxStorageState.PermissionRevoked("SecurityException: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            _storageState.value = TermuxStorageState.Disconnected("Connection failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun inspectAndConnectTree(treeUri: Uri): Boolean {
        _storageState.value = TermuxStorageState.Connecting

        val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (treeDoc == null || !treeDoc.exists() || !treeDoc.canRead()) {
            _storageState.value = TermuxStorageState.Disconnected("Cannot access directory from provider.")
            return false
        }

        // Resolve target folder (check for hidden .shortcuts directory if user picked Home)
        val targetDir = SafUtils.resolveTargetShortcutsDirectory(treeDoc)
        currentTargetDirDoc = targetDir

        val isInsideShortcuts = targetDir.name?.equals(".shortcuts", ignoreCase = true) == true
        val displayPath = SafUtils.getDisplayPath(targetDir.uri)

        _storageState.value = TermuxStorageState.Connected(
            treeUri = treeUri,
            targetDirName = targetDir.name ?: "Termux Storage",
            displayPath = displayPath,
            isInsideShortcuts = isInsideShortcuts,
            canWrite = targetDir.canWrite()
        )

        loadScriptsFromTargetDirectory(targetDir, isInsideShortcuts)
        return true
    }

    /**
     * Reloads all .sh scripts from the connected target DocumentFile directory.
     */
    suspend fun refreshScripts(): Result<List<TermuxScriptFile>> = withContext(Dispatchers.IO) {
        val targetDir = currentTargetDirDoc ?: run {
            return@withContext Result.failure(IllegalStateException("Termux folder is not connected."))
        }

        val state = _storageState.value
        val isInsideShortcuts = (state as? TermuxStorageState.Connected)?.isInsideShortcuts ?: true

        val list = loadScriptsFromTargetDirectory(targetDir, isInsideShortcuts)
        Result.success(list)
    }

    private fun loadScriptsFromTargetDirectory(
        targetDir: DocumentFile,
        isInsideShortcuts: Boolean
    ): List<TermuxScriptFile> {
        val files = targetDir.listFiles()
        val scriptList = mutableListOf<TermuxScriptFile>()

        for (file in files) {
            if (SafUtils.isShellScript(file)) {
                val name = file.name ?: continue
                val relativePath = if (isInsideShortcuts) ".shortcuts/$name" else name
                val logicalPath = "~/$relativePath"

                scriptList.add(
                    TermuxScriptFile(
                        name = name,
                        uri = file.uri,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        relativePath = relativePath,
                        logicalPosixPath = logicalPath,
                        canWrite = file.canWrite(),
                        canDelete = file.canWrite() // Deletion follows write access in SAF
                    )
                )
            }
        }

        // Sort alphabetically by name
        scriptList.sortBy { it.name.lowercase() }
        _scripts.value = scriptList
        return scriptList
    }

    /**
     * Reads script content as UTF-8.
     */
    suspend fun readScriptContent(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = SafUtils.readDocumentContent(contentResolver, uri)
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes script content as UTF-8.
     */
    suspend fun writeScriptContent(uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SafUtils.writeDocumentContent(contentResolver, uri, content)
            refreshScripts()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new .sh file in the target directory.
     */
    suspend fun createNewScript(name: String, content: String): Result<TermuxScriptFile> = withContext(Dispatchers.IO) {
        val targetDir = currentTargetDirDoc ?: run {
            return@withContext Result.failure(IllegalStateException("Termux folder is not connected."))
        }

        val cleanName = if (name.endsWith(".sh", ignoreCase = true)) name else "$name.sh"

        // Prevent path traversal in name
        if (cleanName.contains("/") || cleanName.contains("\\") || cleanName.contains("..")) {
            return@withContext Result.failure(IllegalArgumentException("ชื่อไฟล์ไม่ถูกต้อง"))
        }

        try {
            val newFile = targetDir.createFile(SafUtils.MIME_SHELL_SCRIPT, cleanName)
                ?: return@withContext Result.failure(IllegalStateException("Cannot create file via DocumentsProvider"))

            SafUtils.writeDocumentContent(contentResolver, newFile.uri, content)
            refreshScripts()

            val created = _scripts.value.firstOrNull { it.uri == newFile.uri }
                ?: TermuxScriptFile(
                    name = cleanName,
                    uri = newFile.uri,
                    size = newFile.length(),
                    lastModified = newFile.lastModified(),
                    relativePath = cleanName,
                    logicalPosixPath = "~/$cleanName"
                )

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Renames a script file.
     */
    suspend fun renameScript(script: TermuxScriptFile, newName: String): Result<TermuxScriptFile> = withContext(Dispatchers.IO) {
        val cleanName = if (newName.endsWith(".sh", ignoreCase = true)) newName else "$newName.sh"

        if (cleanName.contains("/") || cleanName.contains("\\") || cleanName.contains("..")) {
            return@withContext Result.failure(IllegalArgumentException("ชื่อไฟล์ไม่ถูกต้อง"))
        }

        try {
            val doc = DocumentFile.fromSingleUri(context, script.uri)
                ?: return@withContext Result.failure(IllegalStateException("File not found"))

            val success = doc.renameTo(cleanName)
            if (success) {
                refreshScripts()
                val updated = _scripts.value.firstOrNull { it.name == cleanName }
                    ?: script.copy(name = cleanName)
                Result.success(updated)
            } else {
                Result.failure(IllegalStateException("Rename failed in DocumentsProvider"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a script file.
     */
    suspend fun deleteScript(script: TermuxScriptFile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = DocumentFile.fromSingleUri(context, script.uri)
                ?: return@withContext Result.failure(IllegalStateException("File not found"))

            val deleted = doc.delete()
            if (deleted) {
                refreshScripts()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Delete failed in DocumentsProvider"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Duplicates a script file.
     */
    suspend fun duplicateScript(script: TermuxScriptFile): Result<TermuxScriptFile> = withContext(Dispatchers.IO) {
        val content = readScriptContent(script.uri).getOrElse {
            return@withContext Result.failure(it)
        }

        val baseName = script.name.removeSuffix(".sh")
        val copyName = "${baseName}_copy.sh"

        createNewScript(copyName, content)
    }

    /**
     * Disconnects the current tree and clears preferences.
     */
    fun disconnect() {
        preferences.clear()
        currentTargetDirDoc = null
        _storageState.value = TermuxStorageState.Disconnected("Disconnected by user")
        _scripts.value = emptyList()
    }
}
