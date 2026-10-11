package com.example.checklistapp

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Priority { HIGH, MEDIUM, LOW }

enum class Recurrence(val label: String) {
    NONE("None"), DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly")
}

data class Subtask(
    val id: Long = System.currentTimeMillis(),
    var text: String = "",
    var isChecked: Boolean = false
)

data class Attachment(
    val id: Long = System.currentTimeMillis(),
    var fileName: String = "",
    var localPath: String = "",
    var mimeType: String = "application/octet-stream"
)

data class ChecklistItem(
    val id: Long = System.currentTimeMillis(),
    var listId: Long = 0,
    var text: String = "",
    var notes: String = "",
    var isChecked: Boolean = false,
    var priority: Priority = Priority.MEDIUM,
    var recurrence: Recurrence = Recurrence.NONE,
    var dueDate: Long? = null,
    var reminderOffsetMinutes: Int? = null,
    var completedAt: Long? = null,
    var position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtasks: MutableList<Subtask> = mutableListOf()
    val attachments: MutableList<Attachment> = mutableListOf()

    val subtasksDone: Int get() = subtasks.count { it.isChecked }

    fun isOverdue(): Boolean =
        !isChecked && dueDate != null && dueDate!! < System.currentTimeMillis()

    fun isDueToday(): Boolean =
        !isChecked && dueDate != null &&
                epochDay(dueDate!!) == epochDay(System.currentTimeMillis())

    companion object {
        fun formatShortDate(millis: Long): String =
            SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))

        fun formatDeadline(millis: Long): String =
            SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(millis))

        fun nextOccurrence(recurrence: Recurrence, currentDue: Long?): Long {
            val cal = Calendar.getInstance()
            if (currentDue != null && currentDue > cal.timeInMillis) cal.timeInMillis = currentDue
            when (recurrence) {
                Recurrence.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                Recurrence.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                Recurrence.MONTHLY -> cal.add(Calendar.MONTH, 1)
                Recurrence.NONE -> {}
            }
            return cal.timeInMillis
        }

        internal fun endOfToday(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis

        private fun epochDay(millis: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            return (cal.timeInMillis + cal.timeZone.getOffset(cal.timeInMillis)) / 86_400_000L
        }
    }
}
