package com.lexapps.sblgntreader.data.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import com.lexapps.sblgntreader.data.model.ReadingLocation

class AppPrefs(private val store: PreferenceStore) {
    private val themeModeKey = intPreferencesKey("themeMode")
    private val fontSizeKey = floatPreferencesKey("fontSize")
    private val isParagraphModeKey = booleanPreferencesKey("isParagraphMode")
    private val hideVerseNumbersKey = booleanPreferencesKey("hideVerseNumbers")
    private val textJustificationKey = booleanPreferencesKey("textJustification")
    private val useGreekBookNamesKey = booleanPreferencesKey("useGreekBookNames")
    private val lastBookIdKey = intPreferencesKey("lastBookId")
    private val lastChapterKey = intPreferencesKey("lastChapter")
    private val quickstartCompletedKey = booleanPreferencesKey("quickstartCompleted")
    private val unboldProperNounsKey = booleanPreferencesKey("unboldProperNouns")
    private val lineSpacingKey = intPreferencesKey("lineSpacing")
    private val bookCoverageSortKey = intPreferencesKey("bookCoverageSort")

    val themeModeFlow: Flow<Int> = store.data.map { it[themeModeKey] ?: 0 }
    val fontSizeFlow: Flow<Float> = store.data.map { it[fontSizeKey] ?: 24f }
    val isParagraphModeFlow: Flow<Boolean> = store.data.map { it[isParagraphModeKey] ?: true }
    val hideVerseNumbersFlow: Flow<Boolean> = store.data.map { it[hideVerseNumbersKey] ?: false }
    val textJustificationFlow: Flow<Boolean> = store.data.map { it[textJustificationKey] ?: false }
    val useGreekBookNamesFlow: Flow<Boolean> = store.data.map { it[useGreekBookNamesKey] ?: false }
    val lastLocationFlow: Flow<ReadingLocation> = store.data.map { ReadingLocation(it[lastBookIdKey] ?: 1, it[lastChapterKey] ?: 1) }
    val lastBookIdFlow: Flow<Int> = store.data.map { it[lastBookIdKey] ?: 1 }
    val lastChapterFlow: Flow<Int> = store.data.map { it[lastChapterKey] ?: 1 }
    val quickstartCompletedFlow: Flow<Boolean> = store.data.map { it[quickstartCompletedKey] ?: false }
    val unboldProperNounsFlow: Flow<Boolean> = store.data.map { it[unboldProperNounsKey] ?: false }
    val lineSpacingFlow: Flow<Int> = store.data.map { it[lineSpacingKey] ?: 1 }
    val bookCoverageSortFlow: Flow<Int> = store.data.map { it[bookCoverageSortKey] ?: 0 }

    suspend fun setThemeMode(modeIndex: Int) = store.edit { it[themeModeKey] = modeIndex }
    suspend fun setFontSize(size: Float) = store.edit {
        val quantized = size.coerceIn(18f, 34f).roundToInt().toFloat()
        it[fontSizeKey] = quantized
    }
    suspend fun setParagraphMode(enabled: Boolean) = store.edit { it[isParagraphModeKey] = enabled }
    suspend fun setHideVerseNumbers(hide: Boolean) = store.edit { it[hideVerseNumbersKey] = hide }
    suspend fun setTextJustification(enabled: Boolean) = store.edit { it[textJustificationKey] = enabled }
    suspend fun setUseGreekBookNames(enabled: Boolean) = store.edit { it[useGreekBookNamesKey] = enabled }
    suspend fun setLastLocation(bookId: Int, chapter: Int) = store.edit {
        it[lastBookIdKey] = bookId
        it[lastChapterKey] = chapter
    }
    suspend fun setQuickstartCompleted(completed: Boolean) = store.edit { it[quickstartCompletedKey] = completed }
    suspend fun setUnboldProperNouns(enabled: Boolean) = store.edit { it[unboldProperNounsKey] = enabled }
    suspend fun setLineSpacing(spacing: Int) = store.edit { it[lineSpacingKey] = spacing }
    suspend fun setBookCoverageSort(sortIndex: Int) = store.edit { it[bookCoverageSortKey] = sortIndex }
}


