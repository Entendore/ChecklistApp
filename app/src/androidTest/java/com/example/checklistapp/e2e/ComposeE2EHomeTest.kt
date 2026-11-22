package com.example.checklistapp.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.checklistapp.ChecklistItem
import com.example.checklistapp.MainActivity
import com.example.checklistapp.testutil.TestSetup
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeE2EHomeTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun setUp() = TestSetup.isolate(context)

    private fun waitUntilExists(text: String) =
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun emptyStateShownWithNoLists() {
        rule.onNodeWithText("No lists yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun createListShowsCard() {
        rule.onNodeWithContentDescription("Add list").performClick()
        rule.onNodeWithText("List name").performTextInput("Work Tasks")
        rule.onNodeWithText("Save").performClick()
        waitUntilExists("Work Tasks")
        rule.onNodeWithText("Work Tasks").assertIsDisplayed()
    }

    @Test
    fun tappingCardOpensChecklist() {
        TestSetup.seedList(context, 7L, "Trips", "✈️", listOf(ChecklistItem(text = "Passport")))
        waitUntilExists("Trips")
        rule.onNodeWithText("Trips").performClick()
        waitUntilExists("Passport")
        rule.onNodeWithText("Passport").assertIsDisplayed()
    }

    @Test
    fun deleteListThenUndoRestores() {
        TestSetup.seedList(context, 8L, "Ephemeral", "📋", emptyList())
        waitUntilExists("Ephemeral")

        rule.onNodeWithText("Ephemeral").performTouchInput { longClick() }
        rule.onNodeWithText("🗑 Delete list").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Ephemeral")).fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("UNDO").performClick()
        waitUntilExists("Ephemeral")
        rule.onNodeWithText("Ephemeral").assertIsDisplayed()
    }
}
