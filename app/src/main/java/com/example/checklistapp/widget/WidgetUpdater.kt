package com.example.checklistapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdater {
    suspend fun refreshAll(context: Context) {
        ChecklistGlanceWidget.updateAll(context)
    }
}
