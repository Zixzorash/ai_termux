package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.service.TermuxRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WIDGET_RUN = "com.aistudio.runtermux.ACTION_WIDGET_RUN"
        const val ACTION_WIDGET_REFRESH = "com.aistudio.runtermux.ACTION_WIDGET_REFRESH"
        const val EXTRA_SCRIPT_ID = "EXTRA_WIDGET_SCRIPT_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_RUN -> {
                val scriptId = intent.getLongExtra(EXTRA_SCRIPT_ID, -1L)
                if (scriptId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getDatabase(context).termuxDao()
                        val script = dao.getScriptById(scriptId)
                        if (script != null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "⚡ กำลังรัน: ${script.name}", Toast.LENGTH_SHORT).show()
                            }
                            TermuxRunner.getInstance(context).executeScript(script)
                            // Update widget status
                            TermuxAppWidgetProvider.updateAllWidgets(context)
                        }
                    }
                }
            }
            ACTION_WIDGET_REFRESH -> {
                TermuxAppWidgetProvider.updateAllWidgets(context)
                Toast.makeText(context, "อัปเดตวิดเจ็ตเรียบร้อย", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
