package com.lexapps.sblgntreader.ui.reader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexapps.sblgntreader.data.model.Book
import com.lexapps.sblgntreader.data.model.ChapterData
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.data.prefs.AppPrefs
import com.lexapps.sblgntreader.data.prefs.VocabPrefs
import com.lexapps.sblgntreader.data.repo.BookRepository
import com.lexapps.sblgntreader.data.repo.TextRepository
import com.lexapps.sblgntreader.data.repo.VocabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.lexapps.sblgntreader.data.model.ReadingLocation
import kotlinx.coroutines.CancellationException
import java.io.IOException

data class VocabularyLoadError(val message: String, val attempt: Int)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val textRepository: TextRepository,
    private val vocabRepository: VocabRepository,
    prefs: VocabPrefs,
    private val appPrefs: AppPrefs
) : ViewModel() {
    private val session = ReaderSession(
        scope = viewModelScope,
        loadBooks = bookRepository::getBooksWithChapterCounts,
        loadLocation = { appPrefs.lastLocationFlow.first() },
        saveLocation = { appPrefs.setLastLocation(it.bookId, it.chapter) },
        loadChapter = { location ->
            val (verses, paragraphs) = textRepository.getChapterData(location.bookId, location.chapter)
            if (verses.isEmpty()) throw IOException("Chapter ${location.key} has no content")
            ChapterData(location.bookId, location.chapter, verses, paragraphs)
        },
        reportError = { Log.e("Reader", "Reader operation failed", it) }
    )
    val state = session.state

    private val pairA = combine(appPrefs.fontSizeFlow, appPrefs.isParagraphModeFlow) { f, p -> f to p }
    private val pairB = combine(appPrefs.hideVerseNumbersFlow, appPrefs.textJustificationFlow) { h, j -> h to j }
    private val pairC = combine(appPrefs.useGreekBookNamesFlow, appPrefs.unboldProperNounsFlow) { g, u -> g to u }
    private val pairD = appPrefs.lineSpacingFlow

    val readerPrefs: StateFlow<ReaderPrefs> = combine(pairA, pairB, pairC, pairD) { ab, cd, ef, g ->
        ReaderPrefs(
            fontSize = ab.first,
            isParagraphMode = ab.second,
            hideVerseNumbers = cd.first,
            isTextJustified = cd.second,
            useGreekBookNames = ef.first,
            unboldProperNouns = ef.second,
            lineSpacing = g
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderPrefs(24f,
        isParagraphMode = true,
        hideVerseNumbers = false,
        isTextJustified = false,
        useGreekBookNames = false,
        unboldProperNouns = false,
        lineSpacing = 1
    ))

    private data class VocabularyState(
        val activeLevels: Map<String, Int> = emptyMap(),
        val userLemmas: Set<String> = emptySet(),
        val error: VocabularyLoadError? = null
    )

    private val vocabularyRefresh = MutableStateFlow(0)
    private var lastVocabulary = VocabularyState()
    private val vocabulary = vocabularyRefresh.flatMapLatest { attempt ->
        combine(
            vocabRepository.observeActiveLevels(prefs),
            vocabRepository.observeUserVocabLemmas()
        ) { levels, lemmas -> VocabularyState(activeLevels = levels, userLemmas = lemmas) }
            .onEach { lastVocabulary = it }
            .onStart { emit(lastVocabulary) }
            .catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                Log.e("Reader", "Could not load vocabulary", error)
                emit(lastVocabulary.copy(error = VocabularyLoadError("Could not load vocabulary. Please try again.", attempt)))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), VocabularyState())

    val activeVocabLevels: StateFlow<Map<String, Int>> = vocabulary.map { it.activeLevels }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val userVocabLemmas: StateFlow<Set<String>> = vocabulary.map { it.userLemmas }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val vocabularyError: StateFlow<VocabularyLoadError?> = vocabulary.map { it.error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init { session.start() }

    fun setCurrent(bookId: Int, chapter: Int) = session.navigate(ReadingLocation(bookId, chapter))
    fun retry() { session.retry(); retryVocabulary() }
    fun retryVocabulary() { vocabularyRefresh.update { it + 1 } }
    fun clearMessage() = session.showMessage(null)

    fun setUserVocab(word: Word, added: Boolean) = save("Could not update your vocabulary. Please try again.") {
        val lemma = word.lemma ?: word.text
        if (added) vocabRepository.addUserVocabLemma(lemma)
        else vocabRepository.removeUserVocabLemma(lemma)
    }

    val quickstartActive: StateFlow<Boolean> = appPrefs.quickstartCompletedFlow
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun markQuickstartCompleted() = save("Could not save tutorial progress. Please try again.") {
        appPrefs.setQuickstartCompleted(true)
    }

    private fun save(message: String, operation: suspend () -> Unit) {
        viewModelScope.launch {
            try { operation() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                Log.e("Reader", message, error)
                session.showMessage(message)
            }
        }
    }
}
