package com.example.checklistapp.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.data.ChecklistRepository
import com.example.checklistapp.MainActivity
import com.example.checklistapp.testutil.TestSetup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeE2EChecklistTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun setUp() {
        TestSetup.isolate(context)
        TestSetup.seedList(
            context, 42L, "Test List", "🧪",
            listOf(ChecklistItem(text = "Read book"), ChecklistItem(text = "Buy milk"))
        )
    }

    private fun openList() {
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Test List").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Test List").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Read book").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun addItemViaDialog() {
        openList()
        rule.onNodeWithContentDescription("Add item").performClick()
        rule.onNodeWithText("Item title").performTextInput("New Task")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("New Task").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("New Task").assertIsDisplayed()
    }

    @Test
    fun saveButtonDisabledWithoutTitle() {
        openList()
        rule.onNodeWithContentDescription("Add item").performClick()
        rule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun checkingItemUpdatesProgress() {
        openList()
        rule.onNodeWithTag("checkbox_Buy milk").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("1 of 2 completed", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun swipeDeleteThenUndoRestores() {
        openList()
        rule.onNodeWithTag("row_Read book").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Read book").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("UNDO").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Read book").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun filterChipsShowTheRightItems() {
        openList()
        rule.onNodeWithTag("checkbox_Buy milk").performClick()
        rule.onNodeWithText("Active").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Buy milk").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("Done").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Buy milk").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun searchFiltersItems() {
        openList()
        rule.onNodeWithTag("searchField").performTextInput("milk")
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Read book").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("Buy milk").assertIsDisplayed()
    }

    @Test
    fun dailyRecurringTaskReschedulesWhenCompleted() {
        openList()
        rule.onNodeWithContentDescription("Add item").performClick()
        rule.onNodeWithText("Item title").performTextInput("Take vitamins")
        rule.onNodeWithText("🔁 Daily").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Take vitamins").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("checkbox_Take vitamins").performClick()

        rule.onNodeWithText("repeats daily", substring = true).assertIsDisplayed()

        val items = runBlocking { ChecklistRepository(context).getItemsOnce(42L) }
        val item = items.first { it.text == "Take vitamins" }
        assertTrue(!item.isChecked && item.dueDate!! > System.currentTimeMillis())
    }

    @Test
    fun duplicateItemFromQuickActions() {
        openList()
        rule.onNodeWithTag("row_Buy milk").performTouchInput { longClick() }
        rule.onNodeWithText("📄 Duplicate").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Buy milk (copy)").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
