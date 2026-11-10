package com.lexapps.sblgntreader.ui.stats

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StatsPreferenceSource {
    val unboldProperNouns: Flow<Boolean>
    val bookCoverageSort: Flow<BookCoverageSort>
    suspend fun setBookCoverageSort(sort: BookCoverageSort)
}

@Singleton
class DefaultStatsPreferenceSource @Inject constructor(
    private val appPrefs: com.lexapps.sblgntreader.data.prefs.AppPrefs
) : StatsPreferenceSource {
    override val unboldProperNouns: Flow<Boolean>
        get() = appPrefs.unboldProperNounsFlow

    override val bookCoverageSort: Flow<BookCoverageSort>
        get() = appPrefs.bookCoverageSortFlow.map { BookCoverageSort.fromStorage(it) }

    override suspend fun setBookCoverageSort(sort: BookCoverageSort) {
        appPrefs.setBookCoverageSort(sort.storageValue)
    }
}
