package com.lexapps.sblgntreader

import com.lexapps.sblgntreader.data.model.Book
import com.lexapps.sblgntreader.data.model.ChapterData
import com.lexapps.sblgntreader.data.model.ChapterNavigation
import com.lexapps.sblgntreader.data.model.ReadingLocation
import com.lexapps.sblgntreader.ui.reader.ReaderSession
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReaderSessionTest {
    private val books = listOf(Book(1, "Matthew", "Matt", 2), Book(2, "Mark", "Mark", 2))
    private fun chapter(location: ReadingLocation) = ChapterData(location.bookId, location.chapter, emptyList(), emptyList())

    @Test fun boundariesAndInvalidLocations() {
        val navigation = ChapterNavigation(books)
        assertEquals(ReadingLocation(2, 1), navigation.at(2))
        assertEquals(listOf(ReadingLocation(1, 1), ReadingLocation(2, 1)), navigation.adjacent(ReadingLocation(1, 2)))
        assertEquals(listOf(ReadingLocation(1, 2)), navigation.adjacent(ReadingLocation(1, 1)))
        assertEquals(listOf(ReadingLocation(2, 1)), navigation.adjacent(ReadingLocation(2, 2)))
        assertNull(navigation.at(-1))
        assertNull(navigation.at(4))
        assertEquals(ReadingLocation(1, 1), navigation.validOrFirst(ReadingLocation(99, 99)))
    }

    @Test fun restoresSavedLocationBeforePublishingBooksAndLoadsAdjacentChapters() = runTest {
        val session = ReaderSession(this, { books }, { ReadingLocation(2, 1) }, {}, ::chapter)
        session.start()
        advanceUntilIdle()
        assertEquals(ReadingLocation(2, 1), session.state.value.location)
        assertEquals(setOf("1:2", "2:1", "2:2"), session.state.value.chapters.keys)
        assertFalse(session.state.value.loading)
        assertNull(session.state.value.error)
    }

    @Test fun staleNonCancellableLoadCannotReplaceNewerLocationOrCache() = runTest {
        val gate = CompletableDeferred<Unit>()
        val saved = mutableListOf<ReadingLocation>()
        var delayOld = false
        val session = ReaderSession(this, { books }, { ReadingLocation(1, 1) }, { saved += it }, {
            if (delayOld && it == ReadingLocation(2, 1)) withContext(NonCancellable) { gate.await() }
            chapter(it)
        })
        session.start(); advanceUntilIdle()
        delayOld = true
        session.navigate(ReadingLocation(2, 1)); runCurrent()
        session.navigate(ReadingLocation(2, 2)); runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(ReadingLocation(2, 2), session.state.value.location)
        assertEquals(ReadingLocation(2, 2), saved.last())
        assertEquals(setOf("2:1", "2:2"), session.state.value.chapters.keys)
    }

    @Test fun failedLoadCanBeRetriedAndFailedSaveDoesNotHideContent() = runTest {
        var failLoad = true
        val session = ReaderSession(this, { books }, { ReadingLocation(1, 1) }, { throw IOException("disk full") }, {
            if (failLoad) throw IOException("database unavailable") else chapter(it)
        })
        session.start(); advanceUntilIdle()
        assertNotNull(session.state.value.error)
        assertFalse(session.state.value.loading)
        failLoad = false
        session.retry(); advanceUntilIdle()
        assertNull(session.state.value.error)
        assertNotNull(session.state.value.chapters["1:1"])
        assertNotNull(session.state.value.message)
    }
}
