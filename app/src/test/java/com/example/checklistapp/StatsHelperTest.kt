package com.example.checklistapp

import com.example.checklistapp.util.StatsHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class StatsHelperTest {

    private val DAY = TimeUnit.HOURS.toMillis(24)

    private fun millisAt(dayOffset: Int, hour: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun `empty history has zero streak`() =
        assertEquals(0, StatsHelper.currentStreak(emptyList()))

    @Test fun `completion today only gives streak of 1`() =
        assertEquals(1, StatsHelper.currentStreak(listOf(System.currentTimeMillis())))

    @Test fun `completion yesterday only keeps the streak alive at 1`() =
        assertEquals(1, StatsHelper.currentStreak(listOf(System.currentTimeMillis() - DAY)))

    @Test fun `three consecutive days ending today gives streak of 3`() {
        val now = System.currentTimeMillis()
        assertEquals(3, StatsHelper.currentStreak(listOf(now, now - DAY, now - 2 * DAY)))
    }

    @Test fun `a missed day breaks the streak`() {
        val now = System.currentTimeMillis()
        assertEquals(1, StatsHelper.currentStreak(listOf(now, now - 2 * DAY)))
    }

    @Test fun `multiple completions on the same day count once`() {
        val now = System.currentTimeMillis()
        assertEquals(1, StatsHelper.currentStreak(listOf(now, now, now - 1000)))
    }

    @Test fun `compute aggregates all statistics correctly`() {
        val lists = listOf(TaskList(id = 1), TaskList(id = 2))
        val items = listOf(
            ChecklistItem(id = 1, text = "done", isChecked = true),
            ChecklistItem(id = 2, text = "active"),
            ChecklistItem(id = 3, text = "overdue", dueDate = millisAt(-1, 9)),
            ChecklistItem(id = 4, text = "dueToday", dueDate = millisAt(0, 12))
        )
        val s = StatsHelper.compute(lists, items, listOf(System.currentTimeMillis()))

        assertEquals(2, s.lists); assertEquals(4, s.tasks); assertEquals(1, s.done)
        assertEquals(1, s.overdue); assertEquals(1, s.dueToday)
        assertEquals(1, s.doneThisWeek); assertEquals(1, s.streak)
    }
}
