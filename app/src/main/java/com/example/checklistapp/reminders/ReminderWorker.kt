package com.example.checklistapp.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.checklistapp.ChecklistApp
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.data.ChecklistRepository
import com.example.checklistapp.R

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        if (itemId == -1L) return Result.success()

        val item = ChecklistRepository(applicationContext).getItemOnce(itemId)
            ?: return Result.success()
        if (item.isChecked) return Result.success()

        showNotification(item)
        return Result.success()
    }

    private fun showNotification(item: ChecklistItem) {
        if (Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = PendingIntent.getActivity(
            applicationContext, item.id.toInt(),
            Intent(Intent.ACTION_VIEW, Uri.parse("checklistapp://list/${item.listId}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = item.dueDate
            ?.let { "Due ${ChecklistItem.formatDeadline(it)}" }
            ?: item.notes.ifBlank { "Don't forget this task" }

        val notification = NotificationCompat
            .Builder(applicationContext, ChecklistApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(item.text)
            .setContentText(text)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(item.id.toInt(), notification)
    }

    companion object { const val KEY_ITEM_ID = "item_id" }
}
