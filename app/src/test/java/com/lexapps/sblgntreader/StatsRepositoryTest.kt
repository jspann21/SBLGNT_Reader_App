package com.lexapps.sblgntreader

import com.lexapps.sblgntreader.data.db.*
import com.lexapps.sblgntreader.data.repo.StatsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class StatsRepositoryTest {
    @Test fun overlappingVocabularySourcesAreCountedOnceInOverallAndBookTotals() = runTest {
        val repository = StatsRepository(object : StatsDao {
            override suspend fun getWordTableCount() = 3L
            override suspend fun getTokenStats(trimCharacters: String) = listOf(
                WordTokenStat(1, "Matthew", "a", "a", "a", "N", mapOf("mounce" to 4, "custom-course" to 1), "proper", 2),
                WordTokenStat(1, "Matthew", "b", "b", "b", "V", mapOf("custom-course" to 1), null, 1)
            )
        }, object : VocabMetadataDao {
            override suspend fun getAll() = listOf(
                VocabMetadataEntity("mounce", 4, 36, "Mounce", "Chapter"),
                VocabMetadataEntity("custom-course", 1, 2, "Custom Course", "Lesson")
            )
            override suspend fun getByName(name: String) = getAll().find { it.vocabName == name }
        })
        val stats = repository.calculateStats(mapOf("mounce" to 4, "custom-course" to 1, "User" to 1), setOf("a"), true)
        assertEquals(3L, stats.totalLearnedWordCount)
        assertEquals(2, stats.totalLearnedLemmaCount)
        assertEquals(3L, stats.bookCoverage.single().learnedWords)
        assertEquals(1f, stats.learnedWordRatio)
        assertTrue(stats.sources.sumOf { it.wordCount } > stats.totalLearnedWordCount)
        assertTrue(stats.suggestions.isEmpty())
    }
}
