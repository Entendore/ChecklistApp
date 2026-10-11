package com.example.checklistapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.checklistapp.data.ChecklistRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChecklistRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repo: ChecklistRepository

    @Before
    fun setUp() = runBlocking {
        context.getSharedPreferences("checklist_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        repo = ChecklistRepository(context)
        repo.nukeAll()
    }

    @Test
    fun `addItem assigns sequential positions`() = runBlocking {
        val listId = repo.addList("L", "📋")
        repo.addItem(ChecklistItem(listId = listId, text = "a"))
        repo.addItem(ChecklistItem(listId = listId, text = "b"))
        val items = repo.getItemsOnce(listId)
        assertEquals(listOf("a", "b"), items.map { it.text })
        assertEquals(listOf(0, 1), items.map { it.position })
    }

    @Test
    fun `checking a recurring task reschedules instead of completing`() = runBlocking {
        val listId = repo.addList("L", "📋")
        val item = ChecklistItem(listId = listId, text = "chores", recurrence = Recurrence.WEEKLY)
        repo.addItem(item)

        val next = repo.setChecked(item, true)
        assertNotNull(next)
        assertTrue(next!! > System.currentTimeMillis())

        val after = repo.getItemOnce(item.id)!!
        assertFalse(after.isChecked)
        assertEquals(next, after.dueDate)
        assertEquals(1, repo.completionHistory().size)
    }

    @Test
    fun `checking a normal task completes and logs history`() = runBlocking {
        val listId = repo.addList("L", "📋")
        val item = ChecklistItem(listId = listId, text = "one-off")
        repo.addItem(item)

        assertNull(repo.setChecked(item, true))
        assertTrue(repo.getItemOnce(item.id)!!.isChecked)
        assertEquals(1, repo.completionHistory().size)
    }

    @Test
    fun `delete then undo restores item at its position`() = runBlocking {
        val listId = repo.addList("L", "📋")
        listOf("a", "b", "c").forEach { repo.addItem(ChecklistItem(listId = listId, text = it)) }
        val b = repo.getItemsOnce(listId).first { it.text == "b" }

        val (deleted, index) = repo.deleteItem(b)
        assertEquals(1, index)
        assertEquals(listOf("a", "c"), repo.getItemsOnce(listId).map { it.text })

        repo.undoDelete(deleted, index)
        assertEquals(listOf("a", "b", "c"), repo.getItemsOnce(listId).map { it.text })
    }

    @Test
    fun `setItemDetails syncs subtasks`() = runBlocking {
        val listId = repo.addList("L", "📋")
        val item = ChecklistItem(listId = listId, text = "parent")
        repo.addItem(item)

        val subs = listOf(Subtask(text = "child 1"), Subtask(text = "child 2", isChecked = true))
        repo.setItemDetails(item.copy(notes = "updated"), subs, emptyList())

        val reloaded = repo.getItemOnce(item.id)!!
        assertEquals("updated", reloaded.notes)
        assertEquals(listOf("child 1", "child 2"), reloaded.subtasks.map { it.text })
        assertTrue(reloaded.subtasks[1].isChecked)
    }

    @Test
    fun `moveItem transfers ownership between lists`() = runBlocking {
        val l1 = repo.addList("One", "1️⃣")
        val l2 = repo.addList("Two", "2️⃣")
        val item = ChecklistItem(listId = l1, text = "wanderer")
        repo.addItem(item)

        repo.moveItem(item, l2)
        assertTrue(repo.getItemsOnce(l1).isEmpty())
        assertEquals(listOf("wanderer"), repo.getItemsOnce(l2).map { it.text })
    }

    @Test
    fun `backup restore round-trips everything`() = runBlocking {
        val listId = repo.addList("Home", "🏠")
        val item = ChecklistItem(
            listId = listId, text = "clean",
            priority = Priority.HIGH, recurrence = Recurrence.DAILY
        ).apply { subtasks.add(Subtask(text = "sweep")) }
        repo.addItem(item)
        repo.logCompletionForTests(1_700_000_000_000)

        val json = repo.exportJson()
        repo.nukeAll()
        assertTrue(repo.importJson(json))

        val restored = repo.getItemsOnce(listId)
        assertEquals(1, restored.size)
        assertEquals(Priority.HIGH, restored[0].priority)
        assertEquals(Recurrence.DAILY, restored[0].recurrence)
        assertEquals(listOf("sweep"), restored[0].subtasks.map { it.text })
        assertEquals(listOf(1_700_000_000_000L), repo.completionHistory())
    }

    @Test
    fun `importJson rejects malformed input`() = runBlocking {
        assertFalse(repo.importJson("definitely-not-json"))
        assertFalse(repo.importJson("null"))
    }
}
