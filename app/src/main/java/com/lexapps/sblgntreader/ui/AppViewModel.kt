package com.lexapps.sblgntreader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexapps.sblgntreader.data.prefs.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    prefs: AppPrefs,
    messages: UiMessages
) : ViewModel() {
    val messages = messages.messages
    val themeModeIndex: StateFlow<Int> = prefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}


