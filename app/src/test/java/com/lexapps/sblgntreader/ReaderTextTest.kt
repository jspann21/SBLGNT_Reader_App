package com.lexapps.sblgntreader

import com.lexapps.sblgntreader.data.model.Paragraph
import com.lexapps.sblgntreader.data.model.Verse
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.ui.reader.*
import org.junit.Assert.*
import org.junit.Test

class ReaderTextTest {
    private fun word(id: Int, text: String) = Word(id, 1, id, text, text, null, null, text, null, null, null, null, mapOf("mounce" to 4))
    private val verse = Verse(1, 1, 1, 1, "12", listOf(word(1, "λόγος"), word(2, "θεός")))
    private val prefs = ReaderPrefs(24f, true, false, false, false, false, 1)

    @Test fun wordSelectionFollowsCurrentOffsetsWithAndWithoutNumbers() {
        for (hide in listOf(false, true)) {
            val content = buildVerseText(verse, prefs.copy(hideVerseNumbers = hide), emptyMap(), emptySet())
            val second = content.text.text.indexOf("θεός")
            assertEquals(2, content.wordAt(second)?.id)
            assertNull(content.wordAt(second - 1))
            assertNull(content.wordAt(content.text.length))
            assertNull(content.wordAt(-1))
        }
    }

    @Test fun paragraphSelectionAndVocabularyStylesRemainConsistentAcrossPreferences() {
        val paragraph = Paragraph(1, 1, 0, listOf("12"))
        val visible = buildParagraphText(paragraph, mapOf("12" to verse), 24f, true, false, mapOf("mounce" to 4), emptySet())
        val hidden = buildParagraphText(paragraph, mapOf("12" to verse), 34f, false, false, mapOf("User" to 1), setOf("θεός"))
        assertEquals(1, visible.wordAt(visible.text.text.indexOf("λόγος"))?.id)
        assertEquals(2, hidden.wordAt(hidden.text.text.indexOf("θεός"))?.id)
        assertFalse(hidden.text.text.startsWith("¹²"))
        assertEquals(listOf("1", "2", "3"), expandVerseToken("1-3"))
        assertEquals(listOf("4a"), expandVerseToken("4a"))
    }
}
