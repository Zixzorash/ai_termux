package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class TermuxNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "termux_execution_channel"
        const val NOTIF_RUNNING_BASE_ID = 1000
        const val NOTIF_COMPLETED_BASE_ID = 2000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showRunningNotification(scriptId: Long, scriptName: String, commandPreview: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_OPEN_SCRIPT_ID", scriptId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            scriptId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_terminal_prompt)
            .setContentTitle("⚡ กำลังรันสคริปต์: $scriptName")
            .setContentText("คำสั่ง: $commandPreview")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIF_RUNNING_BASE_ID + scriptId.toInt(), notification)
        } catch (_: SecurityException) {
            // Android 13+ permission not granted yet
        }
    }

    fun showCompletedNotification(
        scriptId: Long,
        scriptName: String,
        exitCode: Int,
        durationMs: Long,
        vibrate: Boolean = true
    ) {
        // Dismiss running notification
        try {
            NotificationManagerCompat.from(context)
                .cancel(NOTIF_RUNNING_BASE_ID + scriptId.toInt())
        } catch (_: Exception) {}

        val isSuccess = exitCode == 0
        val statusSymbol = if (isSuccess) "✅ สำเร็จ" else "❌ ล้มเหลว (รหัส $exitCode)"
        val durationFormatted = String.format(java.util.Locale.US, "%.2f วินาที", durationMs / 1000.0)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_OPEN_LOG_FOR_SCRIPT", scriptId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (scriptId + 500).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_terminal_prompt)
            .setContentTitle("$statusSymbol: $scriptName")
            .setContentText("ใช้เวลา: $durationFormatted • แตะเพื่อดู Log")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (vibrate) {
            builder.setVibrate(longArrayOf(0, 200, 100, 200))
        }

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIF_COMPLETED_BASE_ID + scriptId.toInt(), builder.build())
        } catch (_: SecurityException) {
            // Permission check
        }
    }

    fun cancelNotification(scriptId: Long) {
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(NOTIF_RUNNING_BASE_ID + scriptId.toInt())
        } catch (_: Exception) {}
    }
}
