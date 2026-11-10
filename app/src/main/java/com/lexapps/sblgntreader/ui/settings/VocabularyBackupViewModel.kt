package com.lexapps.sblgntreader.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexapps.sblgntreader.data.backup.VocabularyBackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VocabularyBackupState(val busy: Boolean = false, val message: String? = null)

@HiltViewModel
class VocabularyBackupViewModel @Inject constructor(private val repository: VocabularyBackupRepository) : ViewModel() {
    private val _state = MutableStateFlow(VocabularyBackupState())
    val state = _state.asStateFlow()

    fun export(uri: Uri) = runOperation {
        repository.exportTo(uri)
        "Vocabulary exported."
    }

    fun importFrom(uri: Uri) = runOperation {
        val count = repository.importFrom(uri)
        "Imported $count new vocabulary entries. Existing entries were kept."
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    private fun runOperation(operation: suspend () -> String) {
        if (_state.value.busy) return
        _state.value = VocabularyBackupState(busy = true)
        viewModelScope.launch {
            try {
                _state.value = VocabularyBackupState(message = operation())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = VocabularyBackupState(message = "Vocabulary transfer failed. ${error.message.orEmpty()}")
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }
}
