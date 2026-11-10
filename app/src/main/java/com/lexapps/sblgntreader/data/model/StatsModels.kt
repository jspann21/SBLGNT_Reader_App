package com.lexapps.sblgntreader.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class SourceStats(
    val key: String,
    val level: Int?,
    val lemmaCount: Int,
    val wordCount: Long,
    val displayName: String = key,
    val levelLabel: String = "Chapter"
) {
    val hasProgress: Boolean get() = wordCount > 0 && lemmaCount > 0
}

@Immutable
data class LearnedStats(
    val totalWordCount: Long,
    val totalLearnedWordCount: Long,
    val totalLearnedLemmaCount: Int,
    val sources: List<SourceStats>,
    val bookCoverage: List<BookCoverage>,
    val posCoverage: List<PosCoverage>,
    val suggestions: List<SuggestedLemma>
) {
    val learnedWordRatio: Float
        get() = if (totalWordCount == 0L) 0f else totalLearnedWordCount.toFloat() / totalWordCount.toFloat()

    fun sourceRatioOfTotal(source: SourceStats): Float =
        if (totalWordCount == 0L) 0f else source.wordCount.toFloat() / totalWordCount.toFloat()
}

@Immutable
data class BookCoverage(
    val bookId: Int,
    val bookName: String,
    val totalWords: Long,
    val learnedWords: Long
) {
    val readability: Float
        get() = if (totalWords == 0L) 0f else learnedWords.toFloat() / totalWords.toFloat()
}

@Immutable
data class PosCoverage(
    val key: String,
    val totalWords: Long,
    val learnedWords: Long
) {
    val coverage: Float
        get() = if (totalWords == 0L) 0f else learnedWords.toFloat() / totalWords.toFloat()
}

@Immutable
data class SuggestedLemma(
    val lemma: String,
    val wordGain: Long,
    val primaryPos: String?
)

