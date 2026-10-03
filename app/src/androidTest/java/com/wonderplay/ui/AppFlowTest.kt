package com.wonderplay.ui
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.wonderplay.MainActivity
import org.junit.Rule
import org.junit.Test
class AppFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun navigateCreatePlaylistAndOpenSettings() {
        val playlistName = "Test listening ${System.nanoTime()}"
        compose.onNodeWithText("Library",useUnmergedTree=true).performClick()
        compose.onNodeWithContentDescription("Create playlist").performClick()
        compose.onNodeWithText("Give it a name").performTextInput(playlistName)
        compose.onNodeWithText("Create",useUnmergedTree=true).performClick()
        compose.waitUntil(5000){compose.onAllNodesWithText(playlistName).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText(playlistName).assertIsDisplayed()
        compose.onNodeWithText("Home",useUnmergedTree=true).performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Reduce motion").assertIsDisplayed()
    }
    @Test fun searchFocusClearAndRotate() {
        compose.onNodeWithText("Search",useUnmergedTree=true).performClick()
        compose.onNodeWithContentDescription("Search music").performTextInput("no-such-song-test")
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithContentDescription("Search music").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.activityRule.scenario.recreate()
        compose.onNodeWithContentDescription("Search music").assertExists()
    }
}
