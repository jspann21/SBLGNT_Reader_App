package com.lexapps.sblgntreader

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.lexapps.sblgntreader.data.model.Paragraph
import com.lexapps.sblgntreader.data.model.Verse
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.ui.reader.*
import com.lexapps.sblgntreader.ui.screens.components.WordText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WordInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val verse = Verse(1, 1, 1, 1, "12", listOf(
        Word(1, 1, 0, "λόγος", null, null, null, "λόγος", null, null, null, null, emptyMap()),
        Word(2, 1, 1, "θεός", null, null, null, "θεός", null, null, null, null, emptyMap())
    ))

    @Test fun tapAndLongPressUseCurrentTextAfterFormattingChanges() {
        var selected = 0
        compose.setContent {
            var hidden by remember { mutableStateOf(false) }
            var paragraph by remember { mutableStateOf(false) }
            val prefs = ReaderPrefs(if (hidden) 30f else 24f, paragraph, hidden, false, false, false, 1)
            val content = if (paragraph) buildParagraphText(Paragraph(1, 1, 1, listOf("12")), mapOf("12" to verse), prefs.fontSize, !hidden, false, emptyMap(), emptySet())
                else buildVerseText(verse, prefs, emptyMap(), emptySet())
            Column {
                Button(onClick = { hidden = !hidden; paragraph = !paragraph }) { Text("Change formatting") }
                WordText(content, TextStyle(fontSize = prefs.fontSize.sp), { selected = it.id }, { selected = -it.id }, Modifier.testTag("reader-text"))
            }
        }
        selectSecondWord(longPress = false)
        compose.runOnIdle { assertEquals(2, selected) }
        compose.onNodeWithText("Change formatting").performClick()
        selectSecondWord(longPress = true)
        compose.runOnIdle { assertEquals(-2, selected) }
        compose.onNodeWithText("Change formatting").performClick()
        selectSecondWord(longPress = false)
        compose.runOnIdle { assertEquals(2, selected) }
    }

    private fun selectSecondWord(longPress: Boolean) {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithTag("reader-text")
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        val point = layout.getBoundingBox(layout.layoutInput.text.text.indexOf("θεός") + 1).center
        node.performTouchInput { if (longPress) longClick(point) else click(point) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
    }
}
