package com.lexapps.sblgntreader.ui.reader

import com.lexapps.sblgntreader.data.model.Book
import com.lexapps.sblgntreader.data.model.ChapterData
import com.lexapps.sblgntreader.data.model.ChapterNavigation
import com.lexapps.sblgntreader.data.model.ReadingLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ReaderUiState(
    val books: List<Book> = emptyList(),
    val location: ReadingLocation? = null,
    val chapters: Map<String, ChapterData> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val messageId: Long = 0L,
    val retryLocationSave: Boolean = false
)

/** Owns one ordered stream of navigation requests and one atomic UI state. */
class ReaderSession(
    private val scope: CoroutineScope,
    private val loadBooks: suspend () -> List<Book>,
    private val loadLocation: suspend () -> ReadingLocation,
    private val saveLocation: suspend (ReadingLocation) -> Unit,
    private val loadChapter: suspend (ReadingLocation) -> ChapterData,
    private val reportError: (Exception) -> Unit = {}
) {
    private val _state = MutableStateFlow(ReaderUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    private var generation = 0L
    private val saveMutex = Mutex()

    fun start() {
        val request = ++generation
        job?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        job = scope.launch {
            try {
                val books = loadBooks()
                val location = ChapterNavigation(books).validOrFirst(loadLocation())
                    ?: error("The bundled text contains no chapters.")
                currentCoroutineContext().ensureActive()
                if (request != generation) return@launch
                _state.value = ReaderUiState(books = books, location = location)
                load(request, location, persist = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                fail(request, error)
            }
        }
    }

    fun navigate(location: ReadingLocation, force: Boolean = false) {
        val previous = _state.value
        if (ChapterNavigation(previous.books).indexOf(location) < 0) return
        if (!force && previous.location == location) return
        val request = ++generation
        job?.cancel()
        _state.update { it.copy(location = location, loading = true, error = null, message = null, retryLocationSave = false) }
        job = scope.launch { load(request, location, persist = true) }
    }

    fun retry() {
        val location = _state.value.location
        if (location == null || _state.value.books.isEmpty()) start() else navigate(location, force = true)
    }

    fun showMessage(message: String?, retryLocationSave: Boolean = false) {
        _state.update {
            it.copy(message = message, messageId = it.messageId + 1, retryLocationSave = message != null && retryLocationSave)
        }
    }

    private suspend fun load(request: Long, location: ReadingLocation, persist: Boolean) {
        try {
            if (persist) {
                try {
                    saveMutex.withLock {
                        currentCoroutineContext().ensureActive()
                        if (request == generation) saveLocation(location)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    reportError(error)
                    if (request == generation) showMessage("Could not save your reading location. Try again.", retryLocationSave = true)
                }
            }
            val current = _state.value.chapters[location.key] ?: loadChapter(location)
            currentCoroutineContext().ensureActive()
            if (request != generation) return
            val adjacentLocations = ChapterNavigation(_state.value.books).adjacent(location)
            val cacheKeys = (adjacentLocations + location).map { it.key }.toSet()
            _state.update {
                it.copy(
                    chapters = it.chapters.filterKeys { key -> key in cacheKeys } + (location.key to current),
                    loading = false,
                    error = null
                )
            }
            for (adjacent in adjacentLocations) {
                if (adjacent.key in _state.value.chapters) continue
                val chapter = loadChapter(adjacent)
                currentCoroutineContext().ensureActive()
                if (request != generation) return
                _state.update { it.copy(chapters = it.chapters + (adjacent.key to chapter)) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            fail(request, error)
        }
    }

    private fun fail(request: Long, error: Exception) {
        reportError(error)
        if (request != generation) return
        _state.update {
            if (it.location?.key in it.chapters) it.copy(loading = false)
            else it.copy(loading = false, error = "Unable to load this chapter. Please try again.")
        }
    }
}
