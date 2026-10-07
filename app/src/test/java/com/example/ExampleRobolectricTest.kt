package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.backup.BackupRestoreManager
import com.example.data.TermuxPreferences
import com.example.data.model.AppSettings
import com.example.data.model.TermuxScript
import com.example.data.model.TermuxScriptFile
import com.example.termux.TermuxCommandManager
import com.example.utils.SafUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Run Termux By IDOL_CHAMP", appName)
    }

    @Test
    fun `test backup JSON generation and parsing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = BackupRestoreManager(context)

        val scripts = listOf(
            TermuxScript(
                id = 1,
                name = "ทดสอบอัปเดต",
                filename = "test.sh",
                content = "echo 'Hello Termux'",
                category = "ทดสอบ"
            )
        )
        val settings = AppSettings()

        val json = manager.generateBackupJson(scripts, settings)
        assertTrue(json.contains("Run Termux By IDOL_CHAMP"))
        assertTrue(json.contains("test.sh"))

        val payload = manager.parseBackupJson(json)
        assertNotNull(payload)
        assertEquals(1, payload?.scriptCount)
        assertEquals("ทดสอบอัปเดต", payload?.scripts?.firstOrNull()?.name)
    }

    @Test
    fun `test TermuxScriptFile security validation`() {
        val safeScript = TermuxScriptFile(
            name = "backup_wifi.sh",
            uri = Uri.parse("content://com.termux.documents/tree/home/backup_wifi.sh"),
            logicalPosixPath = "~/.shortcuts/backup_wifi.sh"
        )
        assertTrue(safeScript.isNameSafe)

        val thaiSafeScript = TermuxScriptFile(
            name = "สำรองข้อมูล wifi.sh",
            uri = Uri.parse("content://com.termux.documents/tree/home/thai.sh"),
            logicalPosixPath = "~/.shortcuts/thai.sh"
        )
        assertTrue(thaiSafeScript.isNameSafe)

        // Traversal attack
        val traversalScript = TermuxScriptFile(
            name = "../malicious.sh",
            uri = Uri.parse("content://com.termux.documents/tree/home/bad.sh")
        )
        assertFalse(traversalScript.isNameSafe)

        // Shell injection attacks
        val injectionScript1 = TermuxScriptFile(
            name = "test;rm -rf /.sh",
            uri = Uri.parse("content://com.termux.documents/tree/home/bad2.sh")
        )
        assertFalse(injectionScript1.isNameSafe)

        val injectionScript2 = TermuxScriptFile(
            name = "echo$(evil).sh",
            uri = Uri.parse("content://com.termux.documents/tree/home/bad3.sh")
        )
        assertFalse(injectionScript2.isNameSafe)
    }

    @Test
    fun `test SafUtils helper formatters`() {
        assertEquals("0 B", SafUtils.formatFileSize(0L))
        assertEquals("500 B", SafUtils.formatFileSize(500L))
        assertEquals("2.0 KB", SafUtils.formatFileSize(2048L))

        val displayHome = SafUtils.getDisplayPath(Uri.parse("content://com.termux.documents/tree/%2Fdata%2Fdata%2Fcom.termux%2Ffiles%2Fhome"))
        assertEquals("/data/data/com.termux/files/home", displayHome)

        val displayShortcuts = SafUtils.getDisplayPath(Uri.parse("content://com.termux.documents/tree/%2Fdata%2Fdata%2Fcom.termux%2Ffiles%2Fhome%2F.shortcuts"))
        assertEquals("/data/data/com.termux/files/home/.shortcuts", displayShortcuts)
    }

    @Test
    fun `test TermuxPreferences storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = TermuxPreferences(context)
        prefs.clear()

        val testUri = Uri.parse("content://com.termux.documents/tree/home")
        prefs.treeUri = testUri
        assertEquals(testUri, prefs.treeUri)

        prefs.clear()
        assertEquals(null, prefs.treeUri)
    }
}
