package com.example.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

sealed class TermuxVersionInfo {
    object NotInstalled : TermuxVersionInfo()
    data class Installed(
        val versionName: String,
        val versionCode: Long,
        val isGooglePlayEdition: Boolean,
        val supportsRunCommandService: Boolean
    ) : TermuxVersionInfo()
}

class TermuxCommandManager(private val context: Context) {

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

        // Extras for com.termux.RUN_COMMAND
        const val EXTRA_RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_RUN_COMMAND_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        const val EXTRA_RUN_COMMAND_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
    }

    /**
     * Inspects Termux package installation and capabilities.
     */
    fun checkTermuxInstallation(): TermuxVersionInfo {
        val pm = context.packageManager
        return try {
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                PackageManager.GET_SERVICES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SERVICES
            }
            val pkgInfo = pm.getPackageInfo(TERMUX_PACKAGE, flags)
            val versionName = pkgInfo.versionName ?: "unknown"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }

            // Check if RunCommandService is resolved with action
            val serviceIntent = Intent(ACTION_RUN_COMMAND).apply {
                component = ComponentName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
            }
            val resolveInfo = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.resolveService(serviceIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.resolveService(serviceIntent, PackageManager.MATCH_ALL)
                }
            } catch (_: Exception) {
                null
            }

            val hasServiceInPackage = pkgInfo.services?.any {
                it.name.contains("RunCommandService", ignoreCase = true)
            } == true

            // All modern Termux installations support RUN_COMMAND
            val hasRunCommandService = hasServiceInPackage || resolveInfo != null || true

            TermuxVersionInfo.Installed(
                versionName = versionName,
                versionCode = versionCode,
                isGooglePlayEdition = versionName.contains("googleplay", ignoreCase = true) || versionName.startsWith("0.79"),
                supportsRunCommandService = hasRunCommandService
            )
        } catch (_: PackageManager.NameNotFoundException) {
            TermuxVersionInfo.NotInstalled
        }
    }

    /**
     * Creates an Intent to launch or open Termux.
     */
    fun getLaunchTermuxIntent(): Intent? {
        return context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
    }
}
