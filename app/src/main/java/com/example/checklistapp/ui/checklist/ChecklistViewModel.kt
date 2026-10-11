package com.example.checklistapp.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checklistapp.Attachment
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.Subtask
import com.example.checklistapp.TaskList
import com.example.checklistapp.data.ChecklistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ChecklistViewModel(
    private val repo: ChecklistRepository,
    val listId: Long
) : ViewModel() {

    val list: StateFlow<TaskList?> = repo.observeLists()
        .map { rows -> rows.firstOrNull { it.id == listId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<List<ChecklistItem>> = repo.observeItems(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun otherLists(): List<TaskList> =
        repo.getListsOnce().filter { it.id != listId }

    suspend fun toggle(item: ChecklistItem, checked: Boolean): Long? =
        repo.setChecked(item, checked)

    suspend fun add(item: ChecklistItem) = repo.addItem(item)

    suspend fun saveDetails(item: ChecklistItem, subs: List<Subtask>, atts: List<Attachment>) =
        repo.setItemDetails(item, subs, atts)

    suspend fun delete(item: ChecklistItem): Pair<ChecklistItem, Int> = repo.deleteItem(item)
    suspend fun undoDelete(item: ChecklistItem, index: Int) = repo.undoDelete(item, index)
    suspend fun duplicate(item: ChecklistItem) = repo.duplicateItem(item)
    suspend fun move(item: ChecklistItem, toListId: Long) = repo.moveItem(item, toListId)
    suspend fun reorder(ordered: List<ChecklistItem>) = repo.reorder(listId, ordered)
    suspend fun clearCompleted() = repo.clearCompleted(listId)
    suspend fun uncheckAll() = repo.uncheckAll(listId)
    suspend fun sortByName() = repo.sortBy(listId, compareBy { it.text.lowercase() })
    suspend fun sortByPriority() = repo.sortBy(listId, compareBy { it.priority.ordinal })
    suspend fun sortByDueDate() = repo.sortBy(listId, compareBy { it.dueDate ?: Long.MAX_VALUE })

    suspend fun rename(name: String) {
        repo.getList(listId)?.let { repo.renameList(it, name, it.emoji) }
    }
}
