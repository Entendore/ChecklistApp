package com.example.checklistapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {

    @Query("""
        SELECT l.id AS id, l.name AS name, l.emoji AS emoji, l.createdAt AS createdAt,
            IFNULL(COUNT(i.id), 0) AS totalCount,
            IFNULL(SUM(CASE WHEN i.isChecked = 1 THEN 1 ELSE 0 END), 0) AS doneCount,
            IFNULL(SUM(CASE WHEN i.isChecked = 0 AND i.dueDate IS NOT NULL
                            AND i.dueDate < CAST(strftime('%s','now') AS INTEGER) * 1000
                       THEN 1 ELSE 0 END), 0) AS overdueCount
        FROM task_lists l
        LEFT JOIN items i ON i.listId = l.id
        GROUP BY l.id
        ORDER BY l.createdAt
    """)
    fun observeListsWithCounts(): Flow<List<ListWithCounts>>

    @Query("SELECT * FROM task_lists ORDER BY createdAt")
    suspend fun getListsOnce(): List<TaskListEntity>

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getList(id: Long): TaskListEntity?

    @Insert
    suspend fun insert(list: TaskListEntity)

    @Update
    suspend fun update(list: TaskListEntity)

    @Delete
    suspend fun delete(list: TaskListEntity)

    @Query("DELETE FROM task_lists")
    suspend fun deleteAll()
}

@Dao
interface ItemDao {

    @Transaction
    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY position")
    fun observeItemsWithDetails(listId: Long): Flow<List<ItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY position")
    suspend fun getItemsOnce(listId: Long): List<ItemWithDetails>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemWithDetails(id: Long): ItemWithDetails?

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItem(id: Long): ItemEntity?

    @Query("SELECT MAX(position) FROM items WHERE listId = :listId")
    suspend fun maxPosition(listId: Long): Int?

    @Insert
    suspend fun insert(item: ItemEntity)

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("DELETE FROM items WHERE isChecked = 1 AND listId = :listId")
    suspend fun deleteCheckedIn(listId: Long)

    @Query("DELETE FROM items")
    suspend fun deleteAll()
}

@Dao
interface DetailDao {

    @Insert
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE itemId = :itemId")
    suspend fun deleteSubtasksFor(itemId: Long)

    @Query("DELETE FROM subtasks")
    suspend fun deleteAllSubtasks()

    @Query("SELECT * FROM attachments WHERE itemId = :itemId")
    suspend fun attachmentsFor(itemId: Long): List<AttachmentEntity>

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments")
    suspend fun deleteAllAttachments()
}

@Dao
interface CompletionLogDao {
    @Insert
    suspend fun log(entry: CompletionLogEntity)

    @Query("SELECT timestamp FROM completion_log ORDER BY timestamp")
    suspend fun timestamps(): List<Long>

    @Query("DELETE FROM completion_log")
    suspend fun deleteAll()
}
