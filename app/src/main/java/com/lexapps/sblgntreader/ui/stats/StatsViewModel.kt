package com.lexapps.sblgntreader.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexapps.sblgntreader.data.model.LearnedStats
import com.lexapps.sblgntreader.data.repo.StatsRepository
import com.lexapps.sblgntreader.data.repo.VocabRepository
import com.lexapps.sblgntreader.data.prefs.VocabPrefs
import com.lexapps.sblgntreader.ui.UiMessages
import com.lexapps.sblgntreader.ui.launchUserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepository: StatsRepository,
    vocabRepository: VocabRepository,
    vocabPrefs: VocabPrefs,
    private val preferences: StatsPreferenceSource,
    private val messages: UiMessages
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val stats = refresh.flatMapLatest {
        combine(
            vocabRepository.observeActiveLevels(vocabPrefs).distinctUntilChanged(),
            vocabRepository.observeUserVocabLemmas().distinctUntilChanged(),
            preferences.unboldProperNouns.distinctUntilChanged()
        ) { levels, lemmas, proper -> Triple(levels, lemmas, proper) }
            .transformLatest { (levels, lemmas, proper) ->
                val sources = levels.keys.map { it.lowercase() }.toMutableSet()
                if (proper) sources += "proper"
                emit(StatsUiState(activeSources = sources, includeProperNouns = proper))
                val result = statsRepository.calculateStats(levels, lemmas, proper)
                emit(StatsUiState(isLoading = false, stats = result, activeSources = sources, includeProperNouns = proper))
            }.catch { error ->
                if (error is CancellationException || error !is Exception) throw error
                messages.report("Unable to load learning progress. Please try again.", error)
                emit(StatsUiState(isLoading = false, error = "Unable to load learning progress."))
            }
    }
    val state: StateFlow<StatsUiState> = combine(stats, preferences.bookCoverageSort) { value, sort ->
        value.copy(bookCoverageSort = sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun retry() { refresh.value += 1 }

    fun setBookCoverageSort(sort: BookCoverageSort) = viewModelScope.launchUserAction(
        messages, "Could not save the progress sort order. Please try again."
    ) { preferences.setBookCoverageSort(sort) }
}

data class StatsUiState(
    val isLoading: Boolean = true,
    val stats: LearnedStats? = null,
    val activeSources: Set<String> = emptySet(),
    val includeProperNouns: Boolean = false,
    val bookCoverageSort: BookCoverageSort = BookCoverageSort.CANONICAL,
    val error: String? = null
)
