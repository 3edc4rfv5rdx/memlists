package x.x.memlists.feature.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import x.x.memlists.core.data.DictionaryItem

data class DictionaryUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val items: List<DictionaryItem> = emptyList()
)

class DictionaryViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = (application as x.x.memlists.MemListsApplication).repository

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val items = repository.loadDictionaryAll(_uiState.value.query)
            _uiState.update { it.copy(isLoading = false, items = items) }
        }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
        load()
    }

    fun add(
        name: String, unit: String?,
        onDuplicate: () -> Unit,
        onDone: () -> Unit,
        onAdded: () -> Unit,
        onError: () -> Unit
    ) {
        val normalized = name.trim().replaceFirstChar { it.titlecase() }
        if (normalized.isEmpty()) return
        viewModelScope.launch {
            try {
                if (repository.findDictionaryByName(normalized) != null) {
                    onDuplicate()
                    return@launch
                }
                repository.insertDictionary(normalized, unit?.trim()?.ifBlank { null })
                load()
                onDone()
                onAdded()
            } catch (_: Exception) {
                onError()
            }
        }
    }

    fun update(
        id: Long, name: String, unit: String?,
        onDuplicate: () -> Unit,
        onDone: () -> Unit,
        onError: () -> Unit
    ) {
        val normalized = name.trim().replaceFirstChar { it.titlecase() }
        if (normalized.isEmpty()) return
        viewModelScope.launch {
            try {
                val existing = repository.findDictionaryByName(normalized)
                if (existing != null && existing.id != id) {
                    onDuplicate()
                    return@launch
                }
                repository.updateDictionary(id, normalized, unit?.trim()?.ifBlank { null })
                load()
                onDone()
            } catch (_: Exception) {
                onError()
            }
        }
    }

    fun delete(id: Long, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteDictionary(id)
                load()
            } catch (_: Exception) {
                onError()
            }
        }
    }
}
