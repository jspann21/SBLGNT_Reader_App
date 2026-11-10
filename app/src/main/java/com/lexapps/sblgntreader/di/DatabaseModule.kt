package com.lexapps.sblgntreader.di

import android.content.Context
import com.lexapps.sblgntreader.data.db.BookDao
import com.lexapps.sblgntreader.data.db.ChapterDao
import com.lexapps.sblgntreader.data.db.ParagraphDao
import com.lexapps.sblgntreader.data.db.SblgntDatabase
import com.lexapps.sblgntreader.data.db.StatsDao
import com.lexapps.sblgntreader.data.db.UserVocabDao
import com.lexapps.sblgntreader.data.db.UserDatabase
import com.lexapps.sblgntreader.data.db.WordDao
import com.lexapps.sblgntreader.data.db.VerseDao
import com.lexapps.sblgntreader.data.db.VocabMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SblgntDatabase =
        SblgntDatabase.build(context)

    @Provides
    fun provideBookDao(db: SblgntDatabase): BookDao = db.bookDao()

    @Provides
    fun provideChapterDao(db: SblgntDatabase): ChapterDao = db.chapterDao()

    @Provides
    fun provideVerseDao(db: SblgntDatabase): VerseDao = db.verseDao()

    @Provides
    fun provideParagraphDao(db: SblgntDatabase): ParagraphDao = db.paragraphDao()

    @Provides
    fun provideVocabMetadataDao(db: SblgntDatabase): VocabMetadataDao = db.vocabMetadataDao()

    @Provides
    fun provideUserVocabDao(db: UserDatabase): UserVocabDao = db.userVocabDao()

    @Provides
    @Singleton
    fun provideUserDatabase(@ApplicationContext context: Context): UserDatabase = UserDatabase.build(context)

    @Provides
    fun provideStatsDao(db: SblgntDatabase): StatsDao = db.statsDao()

    @Provides
    fun provideWordDao(db: SblgntDatabase): WordDao = db.wordDao()
}
