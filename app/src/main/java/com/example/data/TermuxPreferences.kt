package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class TermuxPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "termux_saf_preferences"
        private const val KEY_TREE_URI = "key_persisted_tree_uri"
        private const val KEY_LAST_CONNECTED_TIME = "key_last_connected_time"
        private const val KEY_AUTO_ENTER_SHORTCUTS = "key_auto_enter_shortcuts"
    }

    var treeUri: Uri?
        get() {
            val uriStr = prefs.getString(KEY_TREE_URI, null) ?: return null
            return try {
                Uri.parse(uriStr)
            } catch (_: Exception) {
                null
            }
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_TREE_URI)
                } else {
                    putString(KEY_TREE_URI, value.toString())
                }
                apply()
            }
        }

    var lastConnectedTime: Long
        get() = prefs.getLong(KEY_LAST_CONNECTED_TIME, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_CONNECTED_TIME, value).apply()
        }

    var autoEnterShortcuts: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ENTER_SHORTCUTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_ENTER_SHORTCUTS, value).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
