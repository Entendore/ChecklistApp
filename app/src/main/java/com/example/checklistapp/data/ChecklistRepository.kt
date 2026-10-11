package com.example.checklistapp.data

import android.content.Context
import androidx.room.withTransaction
import com.example.checklistapp.Attachment
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.Priority
import com.example.checklistapp.Recurrence
import com.example.checklistapp.Subtask
import com.example.checklistapp.TaskList
import com.example.checklistapp.data.local.AppDatabase
import com.example.checklistapp.data.local.AttachmentEntity
import com.example.checklistapp.data.local.CompletionLogEntity
import com.example.checklistapp.data.local.ItemEntity
import com.example.checklistapp.data.local.SubtaskEntity
import com.example.checklistapp.data.local.TaskListEntity
import com.example.checklistapp.reminders.ReminderScheduler
import com.example.checklistapp.util.AttachmentStore
import com.example.checklistapp.widget.WidgetUpdater
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChecklistRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val listDao = db.taskListDao()
    private val itemDao = db.itemDao()
    private val detailDao = db.detailDao()
    private val logDao = db.completionLogDao()

    // region READ

    fun observeLists(): Flow<List<TaskList>> =
        listDao.observeListsWithCounts().map { rows -> rows.map { it.toDomain() } }

    fun observeItems(listId: Long): Flow<List<ChecklistItem>> =
        itemDao.observeItemsWithDetails(listId).map { rows -> rows.map { it.toDomain() } }

    suspend fun getList(id: Long): TaskList? =
        listDao.getList(id)?.toDomain(0, 0, 0)

    suspend fun getListsOnce(): List<TaskList> =
        listDao.getListsOnce().map { it.toDomain(0, 0, 0) }

    suspend fun getItemsOnce(listId: Long): List<ChecklistItem> =
        itemDao.getItemsOnce(listId).map { it.toDomain() }

    suspend fun getItemOnce(id: Long): ChecklistItem? =
        itemDao.getItemWithDetails(id)?.toDomain()

    suspend fun allItems(): List<ChecklistItem> =
        listDao.getListsOnce().flatMap { getItemsOnce(it.id) }

    suspend fun completionHistory(): List<Long> = logDao.timestamps()

    // endregion

    // region LISTS

    suspend fun addList(name: String, emoji: String): Long {
        val id = System.currentTimeMillis()
        listDao.insert(TaskListEntity(id, name, emoji, System.currentTimeMillis()))
        WidgetUpdater.refreshAll(appContext)
        return id
    }

    suspend fun renameList(list: TaskList, name: String, emoji: String) {
        listDao.getList(list.id)?.let {
            listDao.update(it.apply { this.name = name; this.emoji = emoji })
        }
        WidgetUpdater.refreshAll(appContext)
    }

    suspend fun deleteList(list: TaskList) {
        itemDao.getItemsOnce(list.id).forEach { ReminderScheduler.cancel(appContext, it.item.id) }
        listDao.getList(list.id)?.let { listDao.delete(it) }
        WidgetUpdater.refreshAll(appContext)
    }

    // endregion

    // region ITEMS

    suspend fun addItem(item: ChecklistItem) {
        val pos = (itemDao.maxPosition(item.listId) ?: -1) + 1
        db.withTransaction {
            itemDao.insert(item.toEntity(pos))
            item.subtasks.forEachIndexed { i, s ->
                detailDao.insertSubtasks(listOf(s.toEntity(item.id, i)))
            }
            item.attachments.forEach { a ->
                val path = commitIfStaged(a.localPath, item.id)
                detailDao.insertAttachment(
                    AttachmentEntity(a.id, item.id, a.fileName, path, a.mimeType)
                )
            }
        }
        scheduleReminderFor(item.copy(position = pos))
        WidgetUpdater.refreshAll(appContext)
    }

    suspend fun setItemDetails(
        item: ChecklistItem,
        subtasks: List<Subtask>,
        attachments: List<Attachment>
    ) {
        db.withTransaction {
            itemDao.getItem(item.id)?.let { e ->
                itemDao.update(e.apply {
                    this.text = item.text; this.notes = item.notes
                    this.isChecked = item.isChecked; this.priority = item.priority.name
                    this.recurrence = item.recurrence.name; this.dueDate = item.dueDate
                    this.reminderOffsetMinutes = item.reminderOffsetMinutes
                })
            }
            detailDao.deleteSubtasksFor(item.id)
            subtasks.forEachIndexed { i, s ->
                detailDao.insertSubtasks(listOf(s.toEntity(item.id, i)))
            }

            val existing = detailDao.attachmentsFor(item.id)
            existing.filter { e -> attachments.none { it.id == e.id } }.forEach { e ->
                AttachmentStore.deleteByRelativePath(appContext, e.localPath)
                detailDao.deleteAttachment(e)
            }
            attachments.filter { a -> existing.none { it.id == a.id } }.forEach { a ->
                val path = commitIfStaged(a.localPath, item.id)
                detailDao.insertAttachment(
                    AttachmentEntity(a.id, item.id, a.fileName, path, a.mimeType)
                )
            }
        }
        scheduleReminderFor(item)
        WidgetUpdater.refreshAll(appContext)
    }

    suspend fun setChecked(item: ChecklistItem, checked: Boolean): Long? {
        if (checked) {
            logDao.log(CompletionLogEntity(timestamp = System.currentTimeMillis()))

            if (item.recurrence != Recurrence.NONE) {
                val next = ChecklistItem.nextOccurrence(item.recurrence, item.dueDate)
                itemDao.getItem(item.id)?.let { e ->
                    e.dueDate = next; e.isChecked = false; e.completedAt = null
                    itemDao.update(e)
                }
                ReminderScheduler.sync(appContext, item.copy(dueDate = next, isChecked = false))
                WidgetUpdater.refreshAll(appContext)
                return next
            }

            itemDao.getItem(item.id)?.let { e ->
                e.isChecked = true; e.completedAt = System.currentTimeMillis()
                itemDao.update(e)
            }
            ReminderScheduler.cancel(appContext, item.id)
        } else {
            itemDao.getItem(item.id)?.let { e ->
                e.isChecked = false; e.completedAt = null
                itemDao.update(e)
            }
            scheduleReminderFor(item)
        }
        WidgetUpdater.refreshAll(appContext)
        return null
    }

    suspend fun deleteItem(item: ChecklistItem): Pair<ChecklistItem, Int> {
        ReminderScheduler.cancel(appContext, item.id)
        detailDao.attachmentsFor(item.id)
            .forEach { AttachmentStore.deleteByRelativePath(appContext, it.localPath) }
        val entity = itemDao.getItem(item.id)
        val index = entity?.position ?: item.position
        if (entity != null) itemDao.delete(entity)
        WidgetUpdater.refreshAll(appContext)
        return item to index
    }

    suspend fun undoDelete(item: ChecklistItem, index: Int) {
        db.withTransaction {
            itemDao.insert(item.toEntity(index))
            normalizePositions(item.listId)
        }
        scheduleReminderFor(item)
        WidgetUpdater.refreshAll(appContext)
    }

    suspend fun duplicateItem(item: ChecklistItem) {
        val newId = System.currentTimeMillis()
        val pos = (itemDao.maxPosition(item.listId) ?: -1) + 1
        db.withTransaction {
            itemDao.insert(
                item.toEntity(pos).copy(
                    id = newId, text = "${item.text} (copy)",
                    isChecked = false, completedAt = null
                )
            )
            item.subtasks.forEachIndexed { i, s ->
                detailDao.insertSubtasks(listOf(s.copy(id = newId + i + 1).toEntity(newId, i)))
            }
            item.attachments.forEach { a ->
                val copyPath = AttachmentStore.duplicateFile(appContext, a.localPath)
                    ?: return@forEach
                detailDao.insertAttachment(
                    AttachmentEntity(newId + a.id, newId, a.fileName, copyPath, a.mimeType)
                )
            }
        }
        WidgetUpdater.refreshAll(appContext)
    }

    suspend fun moveItem(item: ChecklistItem, toListId: Long) {
        val pos = (itemDao.maxPosition(toListId) ?: -1) + 1
        itemDao.getItem(item.id)?.let { e ->
            e.listId = toListId; e.position = pos
            itemDao.update(e)
        }
        WidgetUpdater.refreshAll(appContext)
    }

    suspend fun reorder(listId: Long, ordered: List<ChecklistItem>) {
        db.withTransaction {
            ordered.forEachIndexed { i, item ->
                itemDao.getItem(item.id)?.let { e -> e.position = i; itemDao.update(e) }
            }
        }
        WidgetUpdater.refreshAll(appContext)
    }

    private suspend fun normalizePositions(listId: Long) {
        val items = itemDao.getItemsOnce(listId).sortedBy { it.item.position }
        items.forEachIndexed { i, row ->
            if (row.item.position != i) { row.item.position = i; itemDao.update(row.item) }
        }
    }

    suspend fun clearCompleted(listId: Long) {
        itemDao.getItemsOnce(listId).filter { it.item.isChecked }.forEach { row ->
            ReminderScheduler.cancel(appContext, row.item.id)
            detailDao.attachmentsFor(row.item.id)
                .forEach { AttachmentStore.deleteByRelativePath(appContext, it.localPath) }
        }
        itemDao.deleteCheckedIn(listId)
        WidgetUpdater.refreshAll(appContext)
    }

    suspend fun uncheckAll(listId: Long) {
        itemDao.getItemsOnce(listId).forEach { row ->
            row.item.isChecked = false; row.item.completedAt = null
            itemDao.update(row.item)
            scheduleReminderFor(row.toDomain())
        }
        WidgetUpdater.refreshAll(appContext)
    }

    suspend fun sortBy(listId: Long, comparator: Comparator<ChecklistItem>) {
        reorder(listId, getItemsOnce(listId).sortedWith(comparator))
    }

    // endregion

    // region BACKUP / RESTORE

    data class BackupList(
        val id: Long, val name: String, val emoji: String, val createdAt: Long,
        val items: List<ChecklistItem>
    )
    private data class BackupData(
        val schemaVersion: Int = 2,
        val lists: List<BackupList>,
        val history: List<Long>
    )

    suspend fun exportJson(): String {
        val lists = listDao.getListsOnce().map { l ->
            BackupList(l.id, l.name, l.emoji, l.createdAt, getItemsOnce(l.id))
        }
        return Gson().toJson(BackupData(lists = lists, history = logDao.timestamps()))
    }

    suspend fun importJson(json: String): Boolean = try {
        val backup = Gson().fromJson(json, BackupData::class.java) ?: return false
        val restored = backup.lists ?: return false

        db.withTransaction {
            detailDao.deleteAllSubtasks()
            detailDao.deleteAllAttachments()
            itemDao.deleteAll()
            listDao.deleteAll()
            logDao.deleteAll()

            restored.forEach { l ->
                listDao.insert(TaskListEntity(l.id, l.name, l.emoji, l.createdAt))
                l.items.sortedBy { it.position }.forEachIndexed { pos, item ->
                    itemDao.insert(item.copy(listId = l.id).toEntity(pos))
                    item.subtasks.forEachIndexed { i, s ->
                        detailDao.insertSubtasks(listOf(s.toEntity(item.id, i)))
                    }
                }
            }
            backup.history?.forEach { logDao.log(CompletionLogEntity(timestamp = it)) }
        }

        restored.forEach { l ->
            getItemsOnce(l.id).forEach { scheduleReminderFor(it) }
        }
        WidgetUpdater.refreshAll(appContext)
        true
    } catch (e: Exception) {
        false
    }

    // endregion

    // region TEST HELPERS

    suspend fun nukeAll() = db.withTransaction {
        detailDao.deleteAllSubtasks()
        detailDao.deleteAllAttachments()
        itemDao.deleteAll()
        listDao.deleteAll()
        logDao.deleteAll()
    }

    suspend fun logCompletionForTests(timestamp: Long) =
        logDao.log(CompletionLogEntity(timestamp = timestamp))

    // endregion

    // region INTERNALS

    private suspend fun commitIfStaged(relPath: String, itemId: Long): String =
        if (relPath.startsWith("staging/"))
            AttachmentStore.commitStaged(appContext, relPath, itemId)
        else relPath

    private suspend fun scheduleReminderFor(item: ChecklistItem) {
        val fresh = itemDao.getItem(item.id) ?: return
        ReminderScheduler.sync(
            appContext,
            item.copy(
                isChecked = fresh.isChecked,
                dueDate = fresh.dueDate,
                reminderOffsetMinutes = fresh.reminderOffsetMinutes
            )
        )
    }

    private fun TaskListEntity.toDomain(total: Int, done: Int, overdue: Int) =
        TaskList(id, name, emoji, createdAt, total, done, overdue)

    private fun com.example.checklistapp.data.local.ListWithCounts.toDomain() =
        TaskList(list.id, list.name, list.emoji, list.createdAt, totalCount, doneCount, overdueCount)

    private fun com.example.checklistapp.data.local.ItemWithDetails.toDomain() = ChecklistItem(
        id = item.id, listId = item.listId, text = item.text, notes = item.notes,
        isChecked = item.isChecked, priority = Priority.valueOf(item.priority),
        recurrence = Recurrence.valueOf(item.recurrence), dueDate = item.dueDate,
        reminderOffsetMinutes = item.reminderOffsetMinutes, completedAt = item.completedAt,
        position = item.position, createdAt = item.createdAt
    ).apply {
        subtasks.addAll(subtasks.sortedBy { it.position }
            .map { Subtask(it.id, it.text, it.isChecked) })
        attachments.addAll(attachments.map {
            Attachment(it.id, it.fileName, it.localPath, it.mimeType)
        })
    }

    private fun ChecklistItem.toEntity(pos: Int) = ItemEntity(
        id = id, listId = listId, text = text, notes = notes, isChecked = isChecked,
        priority = priority.name, recurrence = recurrence.name, dueDate = dueDate,
        reminderOffsetMinutes = reminderOffsetMinutes, completedAt = completedAt,
        position = pos, createdAt = createdAt
    )

    private fun Subtask.toEntity(itemId: Long, pos: Int) =
        SubtaskEntity(id, itemId, text, isChecked, pos)

    // endregion
}
