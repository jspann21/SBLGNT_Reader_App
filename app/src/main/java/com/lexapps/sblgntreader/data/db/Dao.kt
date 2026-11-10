package com.lexapps.sblgntreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY book_id")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT book_id, MAX(chapter_number) AS count FROM chapters GROUP BY book_id")
    suspend fun getChapterCounts(): List<BookChapterCount>
}

@Dao
interface ChapterDao {
    @Query("SELECT chapter_id FROM chapters WHERE book_id = :bookId AND chapter_number = :chapter LIMIT 1")
    suspend fun getChapterId(bookId: Int, chapter: Int): Int?
}

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses WHERE chapter_id = :chapterId ORDER BY verse_id")
    suspend fun getVersesByChapter(chapterId: Int): List<VerseEntity>
}

@Dao
interface ParagraphDao {
    @Query("SELECT * FROM paragraphs WHERE chapter_id = :chapterId ORDER BY paragraph_index")
    suspend fun getParagraphsByChapter(chapterId: Int): List<ParagraphEntity>
}

@Dao
interface VocabMetadataDao {
    @Query("SELECT * FROM vocab_metadata")
    suspend fun getAll(): List<VocabMetadataEntity>

    @Query("SELECT * FROM vocab_metadata WHERE vocab_name = :name LIMIT 1")
    suspend fun getByName(name: String): VocabMetadataEntity?
}

@Dao
interface UserVocabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<UserVocabEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(userVocabEntity: UserVocabEntity)

    @Query("DELETE FROM user_vocab WHERE lemma = :lemma")
    suspend fun deleteByLemma(lemma: String)

    @Query("SELECT * FROM user_vocab ORDER BY created_at DESC")
    suspend fun getAll(): List<UserVocabEntity>

    @Query("SELECT * FROM user_vocab WHERE lemma = :lemma")
    suspend fun getByLemma(lemma: String): UserVocabEntity?

    @Query("SELECT lemma FROM user_vocab ORDER BY created_at DESC")
    suspend fun getAllLemmas(): List<String>

    @Query("SELECT * FROM user_vocab ORDER BY created_at DESC")
    fun observeAll(): Flow<List<UserVocabEntity>>
}

@Dao
interface StatsDao {
    @Query(
        """
        SELECT c.book_id AS bookId, b.name AS bookName,
               w.lemma,
               CASE WHEN TRIM(COALESCE(w.lemma, ''), :trimCharacters) <> ''
                    THEN '' ELSE w.text END AS text,
               CASE WHEN TRIM(COALESCE(w.lemma, ''), :trimCharacters) <> ''
                    THEN NULL ELSE w.normalized_text END AS normalizedText,
               SUBSTR(COALESCE(w.pos_tag, ''), 1, 1) AS pos,
               w.vocab_levels AS vocabLevels,
               w.type, COUNT(*) AS count
        FROM words w
        JOIN verses v ON v.verse_id = w.verse_id
        JOIN chapters c ON c.chapter_id = v.chapter_id
        JOIN books b ON b.book_id = c.book_id
        GROUP BY c.book_id, b.name, w.lemma,
                 CASE WHEN TRIM(COALESCE(w.lemma, ''), :trimCharacters) <> ''
                      THEN '' ELSE w.text END,
                 CASE WHEN TRIM(COALESCE(w.lemma, ''), :trimCharacters) <> ''
                      THEN NULL ELSE w.normalized_text END,
                 SUBSTR(COALESCE(w.pos_tag, ''), 1, 1),
                 w.vocab_levels, w.type
        ORDER BY c.book_id
        """
    )
    suspend fun getTokenStats(trimCharacters: String): List<WordTokenStat>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordTableCount(): Long
}

data class WordTokenStat(
    val bookId: Int,
    val bookName: String,
    val lemma: String?,
    val text: String,
    val normalizedText: String?,
    val pos: String,
    val vocabLevels: Map<String, Int>,
    val type: String?,
    val count: Long
)
