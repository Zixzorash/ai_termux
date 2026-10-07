package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.TermuxScript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TermuxAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager? = null, appWidgetIds: IntArray? = null) {
            val manager = appWidgetManager ?: AppWidgetManager.getInstance(context)
            val ids = appWidgetIds ?: manager.getAppWidgetIds(
                ComponentName(context, TermuxAppWidgetProvider::class.java)
            )

            if (ids.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getDatabase(context).termuxDao()
                val scripts = dao.getWidgetScriptsSync()

                for (appWidgetId in ids) {
                    val views = RemoteViews(context.packageName, R.layout.widget_termux_layout)

                    // Open app on title tap
                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val openAppPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

                    // Refresh button
                    val refreshIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                        action = WidgetActionReceiver.ACTION_WIDGET_REFRESH
                    }
                    val refreshPending = PendingIntent.getBroadcast(
                        context,
                        99,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPending)

                    // Configure slots
                    configureSlot(context, views, R.id.widget_item_1, R.id.widget_text_1, scripts.getOrNull(0), 1)
                    configureSlot(context, views, R.id.widget_item_2, R.id.widget_text_2, scripts.getOrNull(1), 2)
                    configureSlot(context, views, R.id.widget_item_3, R.id.widget_text_3, scripts.getOrNull(2), 3)

                    manager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun configureSlot(
            context: Context,
            views: RemoteViews,
            slotId: Int,
            textId: Int,
            script: TermuxScript?,
            slotIndex: Int
        ) {
            if (script != null) {
                views.setViewVisibility(slotId, View.VISIBLE)
                val displayName = script.name.take(16)
                views.setTextViewText(textId, displayName)

                val runIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = WidgetActionReceiver.ACTION_WIDGET_RUN
                    putExtra(WidgetActionReceiver.EXTRA_SCRIPT_ID, script.id)
                }
                val runPending = PendingIntent.getBroadcast(
                    context,
                    slotIndex * 100 + script.id.toInt(),
                    runIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(slotId, runPending)
            } else {
                views.setViewVisibility(slotId, View.GONE)
            }
        }
    }
}
