package com.lexapps.sblgntreader.ui.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lexapps.sblgntreader.data.model.Paragraph
import com.lexapps.sblgntreader.data.model.Verse
import com.lexapps.sblgntreader.data.model.Word

data class WordTextContent(val text: AnnotatedString, val words: Map<String, Word>) {
    fun wordAt(offset: Int): Word? = if (offset !in 0 until text.length) null else
        text.getStringAnnotations("word", offset, offset).firstOrNull()?.let { words[it.item] }
}

fun buildVerseText(verse: Verse, prefs: ReaderPrefs, levels: Map<String, Int>, userLemmas: Set<String>): WordTextContent =
    buildWordText(listOf(verse), prefs.hideVerseNumbers, prefs.unboldProperNouns, levels, userLemmas)

fun buildParagraphText(
    paragraph: Paragraph,
    verses: Map<String, Verse>,
    fontSize: Float,
    showVerseNumbers: Boolean,
    unboldProperNouns: Boolean,
    levels: Map<String, Int>,
    userLemmas: Set<String>
): WordTextContent {
    val ordered = paragraph.verseSpans.flatMap(::expandVerseToken).mapNotNull(verses::get)
    return buildWordText(ordered, !showVerseNumbers, unboldProperNouns, levels, userLemmas, fontSize)
}

private fun buildWordText(
    verses: List<Verse>, hideNumbers: Boolean, unboldProper: Boolean,
    levels: Map<String, Int>, userLemmas: Set<String>, indent: Float? = null
): WordTextContent {
    val builder = AnnotatedString.Builder()
    val words = linkedMapOf<String, Word>()
    if (indent != null) builder.pushStyle(ParagraphStyle(textIndent = androidx.compose.ui.text.style.TextIndent(firstLine = (indent * 1.5f).sp)))
    for (verse in verses) {
        if (!hideNumbers && verse.words.isNotEmpty()) {
            displayVerseNumber(verse.number)?.let { number ->
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
                builder.append(number.map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it.digitToInt()] }.joinToString(""))
                builder.append('\u00a0')
                builder.pop()
            }
        }
        verse.words.forEachIndexed { index, word ->
            val learned = (levels.containsKey("User") && (word.lemma ?: word.text) in userLemmas) ||
                levels.any { (name, level) -> name != "User" && word.vocabLevels[name]?.let { it <= level } == true }
            val normal = levels.isEmpty() || learned || (unboldProper && word.type == "proper")
            builder.pushStyle(SpanStyle(fontWeight = if (normal) FontWeight.Normal else FontWeight.Bold))
            val key = "${verse.id}:$index"
            words[key] = word
            builder.pushStringAnnotation("word", key)
            builder.append(word.text)
            builder.pop()
            builder.append(' ')
            builder.pop()
        }
    }
    if (indent != null) builder.pop()
    return WordTextContent(builder.toAnnotatedString(), words)
}

internal fun expandVerseToken(token: String): List<String> {
    if ('-' !in token) return listOf(token)
    val parts = token.split('-')
    val start = parts.getOrNull(0)?.takeWhile(Char::isDigit)?.toIntOrNull()
    val end = parts.getOrNull(1)?.takeWhile(Char::isDigit)?.toIntOrNull()
    return if (start != null && end != null && end in start..200) (start..end).map(Int::toString) else listOf(token)
}

private fun displayVerseNumber(number: String): String? {
    val digits = number.takeWhile(Char::isDigit)
    val suffix = number.drop(digits.length)
    return digits.takeIf { it.isNotEmpty() && (suffix.isEmpty() || suffix.equals("a", ignoreCase = true)) }
}
