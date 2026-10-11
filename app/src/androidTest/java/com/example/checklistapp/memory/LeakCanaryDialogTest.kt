package com.example.checklistapp.memory

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.checklistapp.MainActivity
import com.example.checklistapp.testutil.TestSetup
import leakcanary.FailTestOnLeak
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LeakCanaryDialogTest {

    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
    @get:Rule val failTestOnLeak = FailTestOnLeak()

    @Before fun setUp() =
        TestSetup.isolate(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun addListDialogIsLeakFreeAfterDismiss() {
        composeRule.onNodeWithContentDescription("Add list").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
    }
}
