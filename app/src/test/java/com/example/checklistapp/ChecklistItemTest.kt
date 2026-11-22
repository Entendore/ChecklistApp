package com.example.checklistapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ChecklistItemTest {

    private fun baseDate(): Long = Calendar.getInstance().apply {
        set(2030, Calendar.MARCH, 15, 12, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun millisAt(dayOffset: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun `null due date is never overdue`() =
        assertFalse(ChecklistItem(text = "X").isOverdue())

    @Test fun `item due 5 minutes ago IS overdue`() =
        assertTrue(
            ChecklistItem(text = "X", dueDate = System.currentTimeMillis() - 5 * 60_000).isOverdue()
        )

    @Test fun `item due in an hour is NOT overdue`() =
        assertFalse(
            ChecklistItem(text = "X", dueDate = System.currentTimeMillis() + 3_600_000).isOverdue()
        )

    @Test fun `checked item with past deadline is NOT overdue`() =
        assertFalse(
            ChecklistItem(text = "X", dueDate = millisAt(-3, 9), isChecked = true).isOverdue()
        )

    @Test fun `item due noon today IS dueToday`() =
        assertTrue(ChecklistItem(text = "X", dueDate = millisAt(0, 12)).isDueToday())

    @Test fun `item due yesterday is NOT dueToday`() =
        assertFalse(ChecklistItem(text = "X", dueDate = millisAt(-1, 12)).isDueToday())

    @Test fun `item due tomorrow is NOT dueToday`() =
        assertFalse(ChecklistItem(text = "X", dueDate = millisAt(1, 12)).isDueToday())

    @Test fun `checked item is never dueToday`() =
        assertFalse(
            ChecklistItem(text = "X", dueDate = millisAt(0, 12), isChecked = true).isDueToday()
        )

    @Test fun `daily recurrence adds one day`() {
        val next = ChecklistItem.nextOccurrence(Recurrence.DAILY, baseDate())
        assertEquals(
            Calendar.getInstance().apply {
                set(2030, Calendar.MARCH, 16, 12, 0, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis, next
        )
    }

    @Test fun `weekly recurrence adds seven days`() {
        val next = ChecklistItem.nextOccurrence(Recurrence.WEEKLY, baseDate())
        assertEquals(
            Calendar.getInstance().apply {
                set(2030, Calendar.MARCH, 22, 12, 0, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis, next
        )
    }

    @Test fun `monthly recurrence clamps month-end - Jan 31 becomes Feb 28`() {
        val jan31 = Calendar.getInstance().apply {
            set(2031, Calendar.JANUARY, 31, 12, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val next = ChecklistItem.nextOccurrence(Recurrence.MONTHLY, jan31)
        assertEquals(
            Calendar.getInstance().apply {
                set(2031, Calendar.FEBRUARY, 28, 12, 0, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis, next
        )
    }

    @Test fun `recurrence NONE returns the future due date unchanged`() =
        assertEquals(baseDate(), ChecklistItem.nextOccurrence(Recurrence.NONE, baseDate()))

    @Test fun `formatDeadline returns a readable non-blank string`() =
        assertTrue(ChecklistItem.formatDeadline(baseDate()).isNotBlank())
}
