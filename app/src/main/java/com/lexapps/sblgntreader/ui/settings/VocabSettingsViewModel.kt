package com.lexapps.sblgntreader.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexapps.sblgntreader.data.model.VocabConfig
import com.lexapps.sblgntreader.data.prefs.VocabPrefs
import com.lexapps.sblgntreader.data.repo.VocabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.lexapps.sblgntreader.ui.UiMessages
import com.lexapps.sblgntreader.ui.launchUserAction

@HiltViewModel
class VocabSettingsViewModel @Inject constructor(
    private val repo: VocabRepository,
    private val prefs: VocabPrefs,
    private val messages: UiMessages
) : ViewModel() {
    private val _configs = MutableStateFlow<List<VocabConfig>>(emptyList())
    val configs: StateFlow<List<VocabConfig>> = _configs.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var updateGeneration = 0L
    private var successfulGeneration = 0L
    private var reloadGeneration = 0L

    init {
        retry()
    }

    private suspend fun reload(generation: Long) {
        if (generation != successfulGeneration) return
        val request = ++reloadGeneration
        _isLoading.value = true
        _error.value = null
        try {
            val configs = loadConfigs()
            if (isCurrent(request, generation)) _configs.value = configs
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("VocabSettings", "Could not load vocabulary settings", error)
            if (isCurrent(request, generation)) {
                _error.value = "Could not load vocabulary settings. Please try again."
            }
        } finally {
            if (isCurrent(request, generation)) _isLoading.value = false
        }
    }

    private fun isCurrent(request: Long, generation: Long): Boolean =
        request == reloadGeneration && generation == successfulGeneration

    private suspend fun loadConfigs(): List<VocabConfig> {
        val existingConfigs = repo.getAvailableVocabConfigs(prefs)

        val prioritized = existingConfigs.sortedBy { it.displayName.lowercase() }

        val userConfig = VocabConfig(
            name = "User",
            minLevel = 1,
            maxLevel = 1,
            selectedLevel = 1,
            isActive = prefs.isActive("User"),
            isUser = true
        )
        // Show prioritized built-ins first, then the User list section
        return prioritized + listOf(userConfig)
    }

    fun retry() {
        viewModelScope.launch { reload(successfulGeneration) }
    }

    fun setActive(name: String, active: Boolean) = update {
        prefs.setActive(name, active)
    }

    fun setLevel(name: String, level: Int) = update {
        prefs.setLevel(name, level)
    }

    private fun update(operation: suspend () -> Unit) {
        val generation = ++updateGeneration
        viewModelScope.launchUserAction(messages, "Could not update vocabulary. Please try again.") {
            operation()
            successfulGeneration = maxOf(successfulGeneration, generation)
            if (generation == successfulGeneration) reload(generation)
        }
    }
}

