package com.lexapps.sblgntreader.data.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE verse_id IN (:verseIds) ORDER BY verse_id, word_index")
    suspend fun forVerses(verseIds: List<Int>): List<WordEntity>

    @Query("SELECT * FROM words WHERE word_id IN (:ids) ORDER BY word_id")
    suspend fun forIds(ids: List<Int>): List<WordEntity>

    @Query("SELECT * FROM words WHERE word_id IN (SELECT MIN(word_id) FROM words WHERE lemma IN (:lemmas) GROUP BY lemma) ORDER BY lemma")
    suspend fun forLemmas(lemmas: List<String>): List<WordEntity>

    @Query("SELECT DISTINCT lemma FROM words WHERE lemma IS NOT NULL AND lemma <> ''")
    suspend fun allLemmas(): List<String>
}
