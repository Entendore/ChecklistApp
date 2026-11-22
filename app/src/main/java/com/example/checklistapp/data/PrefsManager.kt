package com.example.checklistapp.data

import android.content.Context

object PrefsManager {
    private const val FILE = "checklist_prefs"
    private const val KEY_FIRST_RUN = "first_run_done"

    fun isFirstRun(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_FIRST_RUN, true)

    fun markFirstRunDone(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }
}
