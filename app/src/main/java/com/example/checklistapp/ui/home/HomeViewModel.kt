package com.example.checklistapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.Priority
import com.example.checklistapp.Recurrence
import com.example.checklistapp.data.ChecklistRepository
import com.example.checklistapp.TaskList
import com.example.checklistapp.util.StatsHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: ChecklistRepository) : ViewModel() {

    val lists: StateFlow<List<TaskList>> = repo.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addList(name: String, emoji: String) =
        viewModelScope.launch { repo.addList(name, emoji) }

    fun renameList(list: TaskList, name: String, emoji: String) =
        viewModelScope.launch { repo.renameList(list, name, emoji) }

    fun deleteList(list: TaskList) = viewModelScope.launch { repo.deleteList(list) }

    suspend fun stats(): StatsHelper.Stats =
        StatsHelper.compute(lists.value, repo.allItems(), repo.completionHistory())

    suspend fun export(): String = repo.exportJson()
    suspend fun import(json: String): Boolean = repo.importJson(json)

    suspend fun seedSampleData() {
        val groceries = repo.addList("Groceries", "🛒")
        repo.addItem(ChecklistItem(listId = groceries, text = "Milk", priority = Priority.HIGH))
        repo.addItem(ChecklistItem(listId = groceries, text = "Eggs"))
        repo.addItem(ChecklistItem(listId = groceries, text = "Bread", priority = Priority.LOW))

        val todo = repo.addList("To-Do", "✅")
        repo.addItem(
            ChecklistItem(
                listId = todo, text = "Welcome to Checklist! 👋",
                notes = "Tap + to add tasks with subtasks, files, deadlines & reminders"
            )
        )
        repo.addItem(
            ChecklistItem(
                listId = todo, text = "Sample deadline: tomorrow 5 PM",
                priority = Priority.HIGH,
                dueDate = ChecklistItem.endOfToday() + 86_400_000L
            )
        )
        repo.addItem(
            ChecklistItem(
                listId = todo, text = "Water the plants",
                recurrence = Recurrence.WEEKLY, reminderOffsetMinutes = 60
            )
        )
    }
}
