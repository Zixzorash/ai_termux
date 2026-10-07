package com.example.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Helper utility for managing storage permissions on Android (including Android 11 to 16 HyperOS).
 * Handles All Files Access (MANAGE_EXTERNAL_STORAGE) and Media Permissions
 * to allow scripts and the app to access directories like /sdcard/Movies/, /sdcard/Download/, etc.
 */
object StoragePermissionHelper {

    data class StorageDirectoryStatus(
        val path: String,
        val exists: Boolean,
        val canRead: Boolean,
        val canWrite: Boolean,
        val fileCount: Int,
        val sampleFiles: List<String> = emptyList(),
        val statusMessage: String = ""
    )

    data class StoragePermissionInfo(
        val hasAllFilesAccess: Boolean,
        val hasMediaPermissions: Boolean,
        val isFullStorageGranted: Boolean,
        val moviesDirectoryStatus: StorageDirectoryStatus,
        val downloadDirectoryStatus: StorageDirectoryStatus
    )

    /**
     * Checks if the app has All Files Access (MANAGE_EXTERNAL_STORAGE).
     * On Android 11+ (API 30+), this allows direct POSIX read/write to /sdcard/Movies, /sdcard/Download, etc.
     */
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Checks if the app has media or standard storage permissions.
     */
    fun hasMediaPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val video = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            val images = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            video || audio || images
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks overall storage permission status.
     */
    fun getStoragePermissionInfo(context: Context): StoragePermissionInfo {
        val allFiles = hasAllFilesAccess()
        val media = hasMediaPermissions(context)
        val moviesStatus = testDirectoryAccess("/sdcard/Movies")
        val downloadStatus = testDirectoryAccess("/sdcard/Download")

        return StoragePermissionInfo(
            hasAllFilesAccess = allFiles,
            hasMediaPermissions = media,
            isFullStorageGranted = allFiles || media,
            moviesDirectoryStatus = moviesStatus,
            downloadDirectoryStatus = downloadStatus
        )
    }

    /**
     * Returns an array of storage or media permissions that should be requested via runtime dialog.
     */
    fun getRequiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Launches the All Files Access system settings screen for the app.
     */
    fun requestAllFilesAccess(context: Context) {
        try {
            val intent = createManageAllFilesIntent(context)
            context.startActivity(intent)
        } catch (e: Exception) {
            val appDetailsIntent = createAppDetailsSettingsIntent(context)
            context.startActivity(appDetailsIntent)
        }
    }

    data class FileOperationResult(
        val success: Boolean,
        val path: String,
        val message: String
    )

    /**
     * Explicitly tests Reading, Writing, Modifying, and Saving a test file in the given directory
     * (e.g. /sdcard/Movies or /storage/emulated/0/Movies).
     */
    fun testWriteAndSaveFile(targetDir: String = "/sdcard/Movies"): FileOperationResult {
        return try {
            val dir = File(targetDir)
            val actualDir = if (dir.exists()) {
                dir
            } else {
                val alt = File("/storage/emulated/0/${targetDir.removePrefix("/sdcard/").removePrefix("/storage/")}")
                if (!alt.exists()) {
                    alt.mkdirs()
                }
                alt
            }

            if (!actualDir.exists()) {
                val created = actualDir.mkdirs()
                if (!created && !actualDir.exists()) {
                    return FileOperationResult(
                        success = false,
                        path = targetDir,
                        message = "ไม่สามารถสร้างไดเรกทอรี $targetDir ได้ (ต้องการสิทธิ์ All Files Access)"
                    )
                }
            }

            val testFile = File(actualDir, ".runtermux_permission_test_${System.currentTimeMillis()}.tmp")
            // 1. Write / Save
            val content = "RunTermux permission test created at: ${java.util.Date()}\nStatus: Read/Write/Modify/Save OK"
            testFile.writeText(content)

            // 2. Read verification
            val readContent = testFile.readText()
            if (readContent != content) {
                return FileOperationResult(
                    success = false,
                    path = testFile.absolutePath,
                    message = "เขียนไฟล์ได้แต่อ่านเนื้อหาไม่ตรงกัน"
                )
            }

            // 3. Modify
            testFile.appendText("\n[Appended line for modify check]")

            // 4. Cleanup
            val deleted = testFile.delete()

            FileOperationResult(
                success = true,
                path = actualDir.absolutePath,
                message = "สำเร็จ! แอพมีสิทธิ์ อ่าน (Read), เขียน (Write), แก้ไข (Modify), และบันทึก (Save) ในโฟลเดอร์นี้ได้อย่างสมบูรณ์"
            )
        } catch (e: Exception) {
            FileOperationResult(
                success = false,
                path = targetDir,
                message = "ข้อผิดพลาดสิทธิ์: ${e.message} (กรุณาเปิดสิทธิ์ All Files Access)"
            )
        }
    }

    /**
     * Creates an Intent to open the All Files Access settings page for this app.
     */
    fun createManageAllFilesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Creates an Intent to open the Application Details Settings page.
     */
    fun createAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Tests accessibility of a given file system path (e.g., /sdcard/Movies or /storage/emulated/0/Movies).
     */
    fun testDirectoryAccess(path: String): StorageDirectoryStatus {
        return try {
            val dir = File(path)
            if (!dir.exists()) {
                // Try alternate path for /storage/movies or /storage/emulated/0/Movies
                val altDir = when {
                    path.contains("/Movies", ignoreCase = true) -> File("/storage/emulated/0/Movies")
                    path.contains("/Download", ignoreCase = true) -> File("/storage/emulated/0/Download")
                    else -> dir
                }

                if (!altDir.exists()) {
                    return StorageDirectoryStatus(
                        path = path,
                        exists = false,
                        canRead = false,
                        canWrite = false,
                        fileCount = 0,
                        sampleFiles = emptyList(),
                        statusMessage = "ไม่พบไดเรกทอรีนี้ในอุปกรณ์"
                    )
                }
            }

            val target = if (dir.exists()) dir else File("/storage/emulated/0/${path.removePrefix("/sdcard/").removePrefix("/storage/")}")
            val canRead = target.canRead()
            val canWrite = target.canWrite()
            val files = target.listFiles()
            val count = files?.size ?: 0
            val sample = files?.take(5)?.map { it.name } ?: emptyList()

            StorageDirectoryStatus(
                path = target.absolutePath,
                exists = true,
                canRead = canRead,
                canWrite = canWrite,
                fileCount = count,
                sampleFiles = sample,
                statusMessage = if (canRead) "พร้อมใช้งาน (พบ $count รายการ)" else "ไม่มีสิทธิ์อ่านไฟล์ กรุณาเปิดสิทธิ์ All Files Access"
            )
        } catch (e: Exception) {
            StorageDirectoryStatus(
                path = path,
                exists = false,
                canRead = false,
                canWrite = false,
                fileCount = 0,
                sampleFiles = emptyList(),
                statusMessage = "ข้อผิดพลาด: ${e.message}"
            )
        }
    }
}
