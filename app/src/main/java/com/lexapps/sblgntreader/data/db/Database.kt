package com.lexapps.sblgntreader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        VerseEntity::class,
        ParagraphEntity::class,
        VocabMetadataEntity::class,
        WordEntity::class,
        WordCountEntity::class,
        GlobalStatEntity::class,
        ProperNounForm::class,
        BookWordTotal::class,
        BookLemmaStat::class,
        PosLemmaStat::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(VocabularyLevelsConverter::class)
abstract class SblgntDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun verseDao(): VerseDao
    abstract fun paragraphDao(): ParagraphDao
    abstract fun vocabMetadataDao(): VocabMetadataDao
    abstract fun statsDao(): StatsDao
    abstract fun wordDao(): WordDao

    companion object {
        const val DB_NAME = "corpus.db"

        fun build(context: Context): SblgntDatabase = Room.databaseBuilder(
            context.applicationContext,
            SblgntDatabase::class.java,
            DB_NAME
        )
            .createFromAsset("database/sblgnt_data.db")
            .build()
    }
}
