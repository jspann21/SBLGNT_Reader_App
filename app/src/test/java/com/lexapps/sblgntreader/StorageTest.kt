package com.lexapps.sblgntreader

import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.lexapps.sblgntreader.data.backup.VocabularyBackup
import com.lexapps.sblgntreader.data.backup.VocabularyBackupRepository
import com.lexapps.sblgntreader.data.db.SblgntDatabase
import com.lexapps.sblgntreader.data.db.UserDatabase
import com.lexapps.sblgntreader.data.db.UserVocabEntity
import com.lexapps.sblgntreader.data.model.ReadingLocation
import com.lexapps.sblgntreader.data.model.toWord
import com.lexapps.sblgntreader.data.repo.VocabRepository
import com.lexapps.sblgntreader.data.prefs.AppPrefs
import com.lexapps.sblgntreader.data.prefs.PreferenceStore
import com.lexapps.sblgntreader.ui.UiMessages
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class StorageTest {
    @Test fun typedWordQueriesKeepOrderingNullableFieldsAndLargeVocabularyLists() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val corpus = SblgntDatabase.build(context)
        val users = UserDatabase.build(context)
        try {
            val chapter = requireNotNull(corpus.chapterDao().getChapterId(1, 1))
            val verses = corpus.verseDao().getVersesByChapter(chapter)
            val words = corpus.wordDao().forVerses(verses.mapNotNull { it.verseId })
            assertTrue(words.isNotEmpty())
            words.groupBy { it.verseId }.values.forEach { rows ->
                assertEquals(rows.map { it.wordIndex }.sorted(), rows.map { it.wordIndex })
            }
            val repository = VocabRepository(corpus.wordDao(), corpus.vocabMetadataDao(), users.userVocabDao())
            val requested = corpus.wordDao().allLemmas().take(1_001)
            val representatives = repository.getWordsByLemmas(requested)
            assertEquals(requested.toSet(), representatives.map { it.lemma }.toSet())
            assertEquals(requested.size, representatives.size)
            val nullable = com.lexapps.sblgntreader.data.db.WordEntity(1, 1, 0, "λόγος", null, null, null, null, null, null, null, null).toWord()
            assertNull(nullable.gloss)
            assertNull(nullable.lemma)
            assertTrue(nullable.vocabLevels.isEmpty())
        } finally { corpus.close(); users.close() }
    }

    @Test fun preferenceReadFailuresReportDefaultsButDoNotSwallowBugsOrCancellation() = runBlocking<Unit> {
        fun store(error: Exception, messages: UiMessages) = PreferenceStore(object : DataStore<Preferences> {
            override val data = flow<Preferences> { throw error }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw error
        }, messages)
        val messages = UiMessages()
        val prefs = AppPrefs(store(IOException("disk unavailable"), messages))
        assertEquals(ReadingLocation(1, 1), prefs.lastLocationFlow.first())
        assertTrue(messages.messages.first().contains("Temporary defaults"))
        for (failure in listOf(IllegalStateException("unexpected bug"), CancellationException("cancelled"))) {
            try { store(failure, UiMessages()).data.first(); fail("Failure was swallowed") }
            catch (actual: Exception) { assertSame(failure, actual) }
        }
    }

    @Test fun freshCorpusAndPersonalDatabaseOpenAndVocabularyPersists() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val corpus = SblgntDatabase.build(context)
        val users = UserDatabase.build(context)
        try {
            assertEquals(27, corpus.bookDao().getAllBooks().size)
            assertEquals(137741L, corpus.statsDao().getWordTableCount())
            assertEquals(260, corpus.bookDao().getChapterCounts().sumOf { it.count })
            users.userVocabDao().insert(UserVocabEntity("λόγος", 1234))
        } finally { corpus.close(); users.close() }
        val reopened = UserDatabase.build(context)
        try {
            assertEquals(1234L, reopened.userVocabDao().getByLemma("λόγος")?.createdAt)
            reopened.userVocabDao().deleteByLemma("λόγος")
            assertTrue(reopened.userVocabDao().getAll().isEmpty())
        } finally { reopened.close() }
    }

    @Test fun vocabularyRoundTripMergesWithoutLosingTimestampsAndRejectsUnknownWords() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val corpus = SblgntDatabase.build(context)
        val users = UserDatabase.build(context)
        val file = File.createTempFile("vocabulary", ".json", context.cacheDir)
        try {
            val repo = VocabularyBackupRepository(context, users, corpus)
            users.userVocabDao().insert(UserVocabEntity("λόγος", 100))
            repo.exportTo(Uri.fromFile(file))
            users.userVocabDao().deleteByLemma("λόγος")
            assertEquals(1, repo.importFrom(Uri.fromFile(file)))
            assertEquals(0, repo.importFrom(Uri.fromFile(file)))
            assertEquals(100L, users.userVocabDao().getByLemma("λόγος")?.createdAt)
            file.writeText(VocabularyBackup.encode(listOf(UserVocabEntity("not-a-corpus-lemma", 1))))
            try { repo.importFrom(Uri.fromFile(file)); fail("Unknown vocabulary must be rejected") }
            catch (_: IllegalArgumentException) { }
            assertEquals(listOf("λόγος"), users.userVocabDao().getAll().map { it.lemma })
        } finally { corpus.close(); users.close(); file.delete() }
    }

    @Test fun portableVocabularyValidatesTypesAndKeepsEarliestDuplicate() {
        val entries = listOf(UserVocabEntity("λόγος", 20), UserVocabEntity("λόγος", 10))
        assertEquals(listOf(UserVocabEntity("λόγος", 10)), VocabularyBackup.decode(VocabularyBackup.encode(entries)))
        for (text in listOf(
            "{}",
            """{"format":"sblgnt-vocabulary","version":1,"entries":[{"lemma":"","createdAt":1}]}""",
            """{"format":"sblgnt-vocabulary","version":1,"entries":[{"lemma":"λόγος","createdAt":-1}]}""",
            """{"format":"sblgnt-vocabulary","version":1,"entries":[{"lemma":123,"createdAt":1}]}"""
        )) {
            try { VocabularyBackup.decode(text); fail("Invalid backup was accepted") }
            catch (_: org.json.JSONException) { }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun readingLocationAndPreferencesSurviveStoreRecreation() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = File(context.cacheDir, "roundtrip.preferences_pb")
        file.delete()
        suspend fun openAndUse(action: suspend (AppPrefs) -> Unit) {
            val job = SupervisorJob()
            val scope = CoroutineScope(job + Dispatchers.IO)
            val datastore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            try { action(AppPrefs(PreferenceStore(datastore, UiMessages()))) }
            finally { job.cancel(); job.join() }
        }
        openAndUse { it.setLastLocation(2, 3); it.setFontSize(29.4f); it.setHideVerseNumbers(true) }
        openAndUse {
            assertEquals(ReadingLocation(2, 3), it.lastLocationFlow.first())
            assertEquals(29f, it.fontSizeFlow.first())
            assertTrue(it.hideVerseNumbersFlow.first())
        }
        file.delete()
    }
}
