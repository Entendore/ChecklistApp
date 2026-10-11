package com.example.checklistapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.checklistapp.data.PrefsToRoomMigrator
import com.example.checklistapp.util.AttachmentStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChecklistApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        AttachmentStore.cleanupStaging(this)
        appScope.launch { PrefsToRoomMigrator.migrateIfNeeded(this@ChecklistApp) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REMINDERS, "Task reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Deadline reminders for checklist items" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object { const val CHANNEL_REMINDERS = "reminders" }
}
