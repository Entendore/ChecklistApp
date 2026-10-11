package com.example.checklistapp

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.View
import com.example.checklistapp.data.ChecklistRepository
import com.example.checklistapp.data.PrefsManager
import com.example.checklistapp.reminders.ReminderScheduler
import com.example.checklistapp.util.AttachmentStore
import com.example.checklistapp.util.StatsHelper
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class StaticReferenceGuardTest {

    private val guarded = listOf(
        MainActivity::class.java,
        ChecklistRepository::class.java,
        PrefsManager::class.java,
        StatsHelper::class.java,
        AttachmentStore::class.java,
        ReminderScheduler::class.java
    )

    @Test
    fun `no static references to Context, View, Activity or Dialog`() {
        val offenders = mutableListOf<String>()
        for (clazz in guarded) {
            clazz.declaredFields
                .filter { Modifier.isStatic(it.modifiers) }
                .filterNot { it.isSynthetic }
                .forEach { field ->
                    val t = field.type
                    val leaky = View::class.java.isAssignableFrom(t) ||
                            Activity::class.java.isAssignableFrom(t) ||
                            Dialog::class.java.isAssignableFrom(t) ||
                            Context::class.java.isAssignableFrom(t)
                    if (leaky) offenders += "${clazz.simpleName}.${field.name}"
                }
        }
        assertTrue("Static leak-risk references found: $offenders", offenders.isEmpty())
    }

    @Test
    fun `singletons never retain Context instances`() {
        for (singleton in listOf(PrefsManager, StatsHelper, AttachmentStore, ReminderScheduler)) {
            singleton::class.java.declaredFields
                .filterNot { it.isSynthetic }
                .forEach { field ->
                    field.isAccessible = true
                    val value = runCatching { field.get(singleton) }.getOrNull()
                    assertTrue(
                        "${singleton::class.java.simpleName} retains Context in '${field.name}'",
                        value !is Context
                    )
                }
        }
    }
}
