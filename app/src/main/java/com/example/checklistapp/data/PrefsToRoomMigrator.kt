package com.example.checklistapp.data

import android.content.Context
import androidx.room.withTransaction
import com.example.checklistapp.Priority
import com.example.checklistapp.Recurrence
import com.example.checklistapp.data.local.AppDatabase
import com.example.checklistapp.data.local.ItemEntity
import com.example.checklistapp.data.local.TaskListEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrefsToRoomMigrator {

    private const val PREFS_FILE = "checklist_prefs"
    private const val LEGACY_KEY = "task_lists_v3"
    private const val MIGRATED_FLAG = "migrated_to_room"

    private class LegacyItem {
        var id: Long = 0; var text = ""; var notes = ""
        var isChecked = false; var priority = Priority.MEDIUM
        var recurrence = Recurrence.NONE; var dueDate: Long? = null
        var completedAt: Long? = null; var createdAt: Long = 0
    }
    private class LegacyList {
        var id: Long = 0; var name = ""; var emoji = "📋"
        var items: List<LegacyItem> = emptyList(); var createdAt: Long = 0
    }

    suspend fun migrateIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (prefs.getBoolean(MIGRATED_FLAG, false)) return@withContext

        val json = prefs.getString(LEGACY_KEY, null) ?: run {
            prefs.edit().putBoolean(MIGRATED_FLAG, true).apply()
            return@withContext
        }

        val db = AppDatabase.get(context)
        try {
            val lists: List<LegacyList> = Gson().fromJson(
                json, object : TypeToken<List<LegacyList>>() {}.type
            )
            db.withTransaction {
                lists.forEach { l ->
                    db.taskListDao().insert(TaskListEntity(l.id, l.name, l.emoji, l.createdAt))
                    l.items.forEachIndexed { pos, i ->
                        db.itemDao().insert(
                            ItemEntity(
                                id = i.id, listId = l.id, text = i.text, notes = i.notes,
                                isChecked = i.isChecked, priority = i.priority.name,
                                recurrence = i.recurrence.name, dueDate = i.dueDate,
                                reminderOffsetMinutes = null, completedAt = i.completedAt,
                                position = pos, createdAt = i.createdAt
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // corrupt legacy data: start clean rather than crash
        }
        prefs.edit().putBoolean(MIGRATED_FLAG, true).apply()
    }
}
