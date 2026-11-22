package com.example.checklistapp.data.local

import androidx.room.*

@Entity(tableName = "task_lists")
data class TaskListEntity(
    @PrimaryKey val id: Long,
    var name: String,
    var emoji: String,
    val createdAt: Long
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = TaskListEntity::class,
        parentColumns = ["id"], childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("listId")]
)
data class ItemEntity(
    @PrimaryKey val id: Long,
    val listId: Long,
    var text: String,
    var notes: String,
    var isChecked: Boolean,
    var priority: String,
    var recurrence: String,
    var dueDate: Long?,
    var reminderOffsetMinutes: Int?,
    var completedAt: Long?,
    var position: Int,
    val createdAt: Long
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [ForeignKey(
        entity = ItemEntity::class,
        parentColumns = ["id"], childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("itemId")]
)
data class SubtaskEntity(
    @PrimaryKey val id: Long,
    val itemId: Long,
    var text: String,
    var isChecked: Boolean,
    var position: Int
)

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(
        entity = ItemEntity::class,
        parentColumns = ["id"], childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("itemId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: Long,
    val itemId: Long,
    var fileName: String,
    var localPath: String,
    var mimeType: String
)

@Entity(tableName = "completion_log")
data class CompletionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long
)

data class ItemWithDetails(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val subtasks: List<SubtaskEntity>,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val attachments: List<AttachmentEntity>
)

data class ListWithCounts(
    @Embedded val list: TaskListEntity,
    val totalCount: Int,
    val doneCount: Int,
    val overdueCount: Int
)
