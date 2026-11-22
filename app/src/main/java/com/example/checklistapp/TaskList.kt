package com.example.checklistapp

data class TaskList(
    val id: Long = System.currentTimeMillis(),
    var name: String = "My List",
    var emoji: String = "📋",
    val createdAt: Long = System.currentTimeMillis(),
    val totalCount: Int = 0,
    val doneCount: Int = 0,
    val overdueCount: Int = 0
) {
    val percent: Int get() = if (totalCount == 0) 0 else doneCount * 100 / totalCount
}
