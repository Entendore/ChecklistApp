package com.example.checklistapp.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.checklistapp.ChecklistItem
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private fun workName(itemId: Long) = "reminder_$itemId"

    fun sync(context: Context, item: ChecklistItem) {
        cancel(context, item.id)
        val offset = item.reminderOffsetMinutes ?: return
        val due = item.dueDate ?: return
        if (item.isChecked) return

        val delay = (due - offset * 60_000L) - System.currentTimeMillis()
        if (delay <= 0) return

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_ITEM_ID to item.id))
            .addTag("reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(item.id), ExistingWorkPolicy.REPLACE, request
        )
    }

    fun cancel(context: Context, itemId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(itemId))
    }
}
