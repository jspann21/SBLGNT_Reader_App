package com.lexapps.sblgntreader.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Int?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "osis_name") val osisName: String
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["book_id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(name = "idx_chapters_book_id", value = ["book_id"])]
)
data class ChapterEntity(
    @PrimaryKey @ColumnInfo(name = "chapter_id") val chapterId: Int?,
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int
)

@Entity(
    tableName = "verses",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["chapter_id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(name = "idx_verses_chapter_id", value = ["chapter_id"])]
)
data class VerseEntity(
    @PrimaryKey @ColumnInfo(name = "verse_id") val verseId: Int?,
    @ColumnInfo(name = "chapter_id") val chapterId: Int,
    @ColumnInfo(name = "verse_number") val verseNumber: String
)

@Entity(
    tableName = "paragraphs",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["chapter_id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(name = "idx_paragraphs_chapter_id", value = ["chapter_id"])]
)
data class ParagraphEntity(
    @PrimaryKey @ColumnInfo(name = "paragraph_id") val paragraphId: Int?,
    @ColumnInfo(name = "chapter_id") val chapterId: Int,
    @ColumnInfo(name = "paragraph_index") val paragraphIndex: Int,
    @ColumnInfo(name = "verse_spans") val verseSpans: String
)

@Entity(tableName = "vocab_metadata")
data class VocabMetadataEntity(
    @PrimaryKey @ColumnInfo(name = "vocab_name") val vocabName: String,
    @ColumnInfo(name = "min_level") val minLevel: Int,
    @ColumnInfo(name = "max_level") val maxLevel: Int,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "level_label") val levelLabel: String
)

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["verse_id"],
            childColumns = ["verse_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(name = "idx_words_verse_id", value = ["verse_id"]),
        Index(name = "idx_words_lemma", value = ["lemma"])
    ]
)
data class WordEntity(
    @PrimaryKey @ColumnInfo(name = "word_id") val wordId: Int?,
    @ColumnInfo(name = "verse_id") val verseId: Int,
    @ColumnInfo(name = "word_index") val wordIndex: Int,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "normalized_text") val normalizedText: String?,
    @ColumnInfo(name = "pos_tag") val posTag: String?,
    @ColumnInfo(name = "gloss") val gloss: String?,
    @ColumnInfo(name = "lemma") val lemma: String?,
    @ColumnInfo(name = "literal") val literal: String?,
    @ColumnInfo(name = "louw") val louw: String?,
    @ColumnInfo(name = "strong") val strong: String?,
    @ColumnInfo(name = "type") val type: String?,
    @ColumnInfo(name = "vocab_levels", defaultValue = "'{}'") val vocabLevels: Map<String, Int> = emptyMap()
)

data class BookChapterCount(
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "count") val count: Int
)

@Entity(tableName = "user_vocab")
data class UserVocabEntity(
    @PrimaryKey @ColumnInfo(name = "lemma") val lemma: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "word_counts",
    indices = [
        Index(name = "idx_word_counts_lemma", value = ["lemma"]),
        Index(name = "idx_word_counts_normalized", value = ["normalized_text"])
    ]
)
data class WordCountEntity(
    @PrimaryKey @ColumnInfo(name = "word_key") val wordKey: String,
    @ColumnInfo(name = "lemma") val lemma: String?,
    @ColumnInfo(name = "normalized_text") val normalizedText: String?,
    @ColumnInfo(name = "count") val count: Long
)

@Entity(tableName = "global_stats")
data class GlobalStatEntity(
    @PrimaryKey @ColumnInfo(name = "stat_key") val statKey: String,
    @ColumnInfo(name = "stat_value") val statValue: Long
)

@Entity(
    tableName = "proper_noun_forms",
    indices = [
        Index(name = "idx_proper_noun_forms_lemma", value = ["lemma"]),
        Index(name = "idx_proper_noun_forms_normalized", value = ["normalized_text"])
    ]
)
data class ProperNounForm(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "form_id") val formId: Long,
    @ColumnInfo(name = "lemma") val lemma: String?,
    @ColumnInfo(name = "normalized_text") val normalizedText: String?
)

@Entity(tableName = "book_word_totals")
data class BookWordTotal(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "book_name") val bookName: String,
    @ColumnInfo(name = "total_words") val totalWords: Long
)

@Entity(
    tableName = "book_lemma_stats",
    indices = [
        Index(name = "idx_book_lemma_stats_book_id", value = ["book_id"]),
        Index(name = "idx_book_lemma_stats_lemma", value = ["lemma"]),
        Index(name = "idx_book_lemma_stats_normalized", value = ["normalized_text"])
    ]
)
data class BookLemmaStat(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "stat_id") val statId: Long,
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "lemma") val lemma: String?,
    @ColumnInfo(name = "normalized_text") val normalizedText: String?,
    @ColumnInfo(name = "count") val count: Long
)

@Entity(
    tableName = "pos_lemma_stats",
    indices = [
        Index(name = "idx_pos_lemma_stats_pos", value = ["pos"]),
        Index(name = "idx_pos_lemma_stats_lemma", value = ["lemma"]),
        Index(name = "idx_pos_lemma_stats_normalized", value = ["normalized_text"])
    ]
)
data class PosLemmaStat(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "stat_id") val statId: Long,
    @ColumnInfo(name = "pos") val pos: String,
    @ColumnInfo(name = "lemma") val lemma: String?,
    @ColumnInfo(name = "normalized_text") val normalizedText: String?,
    @ColumnInfo(name = "count") val count: Long
)
