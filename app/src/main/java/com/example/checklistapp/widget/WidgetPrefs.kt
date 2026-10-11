package com.example.checklistapp.widget

import android.content.Context

object WidgetPrefs {
    private const val FILE = "widget_prefs"
    private fun key(id: Int) = "list_$id"

    fun saveListId(context: Context, appWidgetId: Int, listId: Long) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putLong(key(appWidgetId), listId).apply()
    }

    fun getListId(context: Context, appWidgetId: Int): Long? {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(key(appWidgetId), -1L)
        return if (v == -1L) null else v
    }

    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().remove(key(appWidgetId)).apply()
    }
}
