package com.lexapps.sblgntreader.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexapps.sblgntreader.data.db.UserVocabEntity
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.data.repo.VocabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import com.lexapps.sblgntreader.ui.UiMessages
import com.lexapps.sblgntreader.ui.launchUserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UserVocabSortOption { RECENT, ALPHABETICAL }

data class UserVocabItem(
    val word: Word,
    val createdAt: Long
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserVocabViewModel @Inject constructor(
    private val vocabRepository: VocabRepository,
    private val messages: UiMessages
) : ViewModel() {

    private val refresh = MutableStateFlow(0)
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()
    private var lastItems: List<UserVocabItem> = emptyList()
    private val rawItems = refresh.flatMapLatest {
        vocabRepository.observeUserVocabEntries()
            .mapLatest { buildItems(it) }
            .onEach { items -> lastItems = items; _loadError.value = null }
            .onStart { _loadError.value = null; emit(lastItems) }
            .catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                Log.e("UserVocab", "Could not load vocabulary", error)
                _loadError.value = "Could not load your vocabulary. Please try again."
                emit(lastItems)
            }
    }
    private val _sortOption = MutableStateFlow(UserVocabSortOption.RECENT)

    val sortOption: StateFlow<UserVocabSortOption> = _sortOption.asStateFlow()

    val words: StateFlow<List<UserVocabItem>> = combine(rawItems, _sortOption) { items, sort ->
        when (sort) {
            UserVocabSortOption.RECENT -> items.sortedByDescending { it.createdAt }
            UserVocabSortOption.ALPHABETICAL -> items.map { item ->
                AlphabeticalItem(item, normalizeGreek(item.word.lemma ?: item.word.text), normalizeGreek(item.word.text))
            }.sortedWith(compareBy({ it.lemmaKey }, { it.textKey })).map { it.item }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSortOption(option: UserVocabSortOption) {
        _sortOption.value = option
    }

    fun retry() { refresh.update { it + 1 } }

    fun deleteWord(word: Word) {
        viewModelScope.launchUserAction(messages, "Could not update vocabulary. Please try again.") {
            val lemma = word.lemma ?: word.text
            vocabRepository.removeUserVocabLemma(lemma)
        }
    }

    private suspend fun buildItems(entries: List<UserVocabEntity>): List<UserVocabItem> {
        if (entries.isEmpty()) return emptyList()
        val lemmas = entries.map { it.lemma }
        val words = vocabRepository.getWordsByLemmas(lemmas)
        if (words.isEmpty()) return emptyList()
        val wordsByLemma = words.associateBy { it.lemma ?: it.text }
        return entries.mapNotNull { entry ->
            val word = wordsByLemma[entry.lemma] ?: return@mapNotNull null
            UserVocabItem(word = word, createdAt = entry.createdAt)
        }
    }

    private data class AlphabeticalItem(val item: UserVocabItem, val lemmaKey: String, val textKey: String)

    private fun normalizeGreek(input: String): String {
        if (input.isEmpty()) return input
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .lowercase(Locale.ROOT)
        val sb = StringBuilder(normalized.length)
        for (ch in normalized) {
            val type = Character.getType(ch)
            if (type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.MODIFIER_SYMBOL.toInt() ||
                type == Character.MODIFIER_LETTER.toInt()
            ) {
                continue
            }
            when (ch) {
                'ς', 'ϲ' -> sb.append('σ')
                'ͺ' -> continue
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
