package com.example.checklistapp.util

import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.TaskList
import java.util.TimeZone
import java.util.TreeSet
import java.util.concurrent.TimeUnit

object StatsHelper {

    private fun epochDay(millis: Long): Long =
        TimeUnit.MILLISECONDS.toDays(millis + TimeZone.getDefault().getOffset(millis))

    fun currentStreak(history: List<Long>): Int {
        if (history.isEmpty()) return 0
        val days = TreeSet<Long>().apply { history.forEach { add(epochDay(it)) } }

        var expected = epochDay(System.currentTimeMillis())
        if (!days.contains(expected)) {
            expected -= 1
            if (!days.contains(expected)) return 0
        }
        var streak = 0
        while (days.contains(expected)) { streak++; expected -= 1 }
        return streak
    }

    data class Stats(
        val lists: Int, val tasks: Int, val done: Int,
        val overdue: Int, val dueToday: Int,
        val doneThisWeek: Int, val streak: Int
    )

    fun compute(lists: List<TaskList>, items: List<ChecklistItem>, history: List<Long>): Stats {
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        return Stats(
            lists = lists.size,
            tasks = items.size,
            done = items.count { it.isChecked },
            overdue = items.count { it.isOverdue() },
            dueToday = items.count { it.isDueToday() },
            doneThisWeek = history.count { it >= weekAgo },
            streak = currentStreak(history)
        )
    }
}
