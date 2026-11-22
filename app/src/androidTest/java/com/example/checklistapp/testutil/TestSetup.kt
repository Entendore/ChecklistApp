package com.example.checklistapp.testutil

import android.content.Context
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.data.ChecklistRepository
import com.example.checklistapp.data.PrefsManager
import com.example.checklistapp.TaskList
import kotlinx.coroutines.runBlocking

object TestSetup {

    fun isolate(context: Context) = runBlocking {
        context.getSharedPreferences("checklist_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        ChecklistRepository(context).nukeAll()
    }

    fun seedList(
        context: Context, id: Long, name: String, emoji: String,
        items: List<ChecklistItem>
    ): TaskList = runBlocking {
        val repo = ChecklistRepository(context)
        repo.addList(name, emoji)
        items.forEach { repo.addItem(it.copy(listId = id)) }
        repo.getList(id)!!
    }
}
