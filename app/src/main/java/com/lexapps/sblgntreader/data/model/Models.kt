package com.lexapps.sblgntreader.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Book(
    val id: Int,
    val name: String,
    val osisName: String,
    val chapterCount: Int
)

@Immutable
data class Verse(
    val id: Int,
    val chapterId: Int,
    val bookId: Int,
    val chapterNumber: Int,
    val number: String,
    val words: List<Word>
)

@Immutable
data class Word(
    val id: Int,
    val verseId: Int,
    val wordIndex: Int,
    val text: String,
    val normalizedText: String?,
    val posTag: String?,
    val gloss: String?,
    val lemma: String?,
    val literal: String?,
    val louw: String?,
    val strong: String?,
    val type: String?,
    val vocabLevels: Map<String, Int>,
    val courses: Map<String, VocabularyCourse> = emptyMap()
)

@Immutable
data class VocabularyCourse(val name: String, val levelLabel: String)

@Immutable
data class Paragraph(
    val id: Int,
    val chapterId: Int,
    val paragraphIndex: Int,
    val verseSpans: List<String>
)

@Immutable
data class VocabConfig(
    val name: String,
    val minLevel: Int,
    val maxLevel: Int,
    val selectedLevel: Int,
    val isActive: Boolean,
    val isUser: Boolean = false,
    val displayName: String = name,
    val levelLabel: String = "Chapter"
)

@Immutable
data class ChapterData(
    val bookId: Int,
    val chapter: Int,
    val verses: List<Verse>,
    val paragraphs: List<Paragraph>,
    val error: String? = null,
    val isLoading: Boolean = false
)


