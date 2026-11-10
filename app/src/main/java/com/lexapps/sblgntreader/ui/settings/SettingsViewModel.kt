package com.lexapps.sblgntreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexapps.sblgntreader.data.prefs.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.lexapps.sblgntreader.ui.UiMessages
import com.lexapps.sblgntreader.ui.launchUserAction

data class SettingsState(
    val fontSize: Float = 24f,
    val isParagraphMode: Boolean = true,
    val hideVerseNumbers: Boolean = false,
    val isTextJustified: Boolean = false,
    val useGreekBookNames: Boolean = false,
    val unboldProperNouns: Boolean = false,
    val lineSpacing: Int = 1
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPrefs,
    private val messages: UiMessages
) : ViewModel() {
    private val pairA = combine(prefs.fontSizeFlow, prefs.isParagraphModeFlow) { font, para -> Pair(font, para) }
    private val pairB = combine(prefs.hideVerseNumbersFlow, prefs.textJustificationFlow) { hideNum, justify -> Pair(hideNum, justify) }
    private val pairC = combine(prefs.useGreekBookNamesFlow, prefs.unboldProperNounsFlow) { greek, unboldProper -> Pair(greek, unboldProper) }
    private val pairD = prefs.lineSpacingFlow

    val state: StateFlow<SettingsState> = combine(pairA, pairB, pairC, pairD) { ab, cd, ef, g ->
        SettingsState(
            fontSize = ab.first,
            isParagraphMode = ab.second,
            hideVerseNumbers = cd.first,
            isTextJustified = cd.second,
            useGreekBookNames = ef.first,
            unboldProperNouns = ef.second,
            lineSpacing = g
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setFontSize(size: Float) = update { prefs.setFontSize(size) }
    fun setParagraphMode(enabled: Boolean) = update { prefs.setParagraphMode(enabled) }
    fun setHideVerseNumbers(hide: Boolean) = update { prefs.setHideVerseNumbers(hide) }
    fun setTextJustification(enabled: Boolean) = update { prefs.setTextJustification(enabled) }
    fun setUseGreekBookNames(enabled: Boolean) = update { prefs.setUseGreekBookNames(enabled) }
    fun setUnboldProperNouns(enabled: Boolean) = update { prefs.setUnboldProperNouns(enabled) }
    fun setThemeMode(index: Int) = update { prefs.setThemeMode(index) }
    fun setLineSpacing(spacing: Int) = update { prefs.setLineSpacing(spacing) }
    fun restartQuickstart(onSuccess: () -> Unit) = update {
        prefs.setQuickstartCompleted(false)
        onSuccess()
    }
    private fun update(action: suspend () -> Unit) = viewModelScope.launchUserAction(
        messages, "Could not save settings. Please try again.", action
    )
}


