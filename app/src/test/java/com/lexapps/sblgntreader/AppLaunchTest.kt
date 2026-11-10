package com.lexapps.sblgntreader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.lifecycle.Lifecycle

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = SblgntApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppLaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun readerLoadsBundledGreekTextAndSettingsSurviveActivityRecreation() {
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasText("Βίβλος", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Matthew 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Font Size").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Font Size").assertIsDisplayed()
    }

    @Test fun sameChapterSelectionBookBoundaryAndBackgroundRestoreKeepLocation() {
        waitForTitle("Matthew 1")
        repeat(6) { compose.onNodeWithText("Next").performClick() }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Next").fetchSemanticsNodes().isEmpty() }
        selectMatthewChapter(1)
        compose.onNodeWithTag("reader-pager").performTouchInput { swipeLeft() }
        waitForTitle("Matthew 2")
        selectMatthewChapter(28)
        waitForTitle("Matthew 28")
        compose.onNodeWithTag("reader-pager").performTouchInput { swipeLeft() }
        waitForTitle("Mark 1")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.mainClock.advanceTimeBy(6_000)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitForTitle("Mark 1")
        compose.activityRule.scenario.recreate()
        waitForTitle("Mark 1")
    }

    private fun selectMatthewChapter(chapter: Int) {
        compose.onNodeWithContentDescription("Menu").performClick()
        val chip = compose.onAllNodesWithTag("chapter-1-$chapter")
        if (chip.fetchSemanticsNodes().isEmpty()) compose.onNodeWithText("Matthew").performClick()
        compose.onNodeWithTag("chapter-1-$chapter").performScrollTo().performClick()
    }

    private fun waitForTitle(title: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(title).assertIsDisplayed()
    }
}
