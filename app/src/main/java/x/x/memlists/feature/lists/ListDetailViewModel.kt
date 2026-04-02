package x.x.memlists.feature.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListDetailViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = (application as x.x.memlists.MemListsApplication).repository

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    fun load(listId: Long) {
        _uiState.update { it.copy(isLoading = true, listId = listId) }
        viewModelScope.launch {
            val detail = repository.loadListDetail(listId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    listId = detail.listId,
                    title = detail.name,
                    comment = detail.comment,
                    uncheckedEntries = detail.uncheckedEntries,
                    checkedEntries = detail.checkedEntries
                )
            }
        }
    }

    fun toggleChecked(entryId: Long, checked: Boolean) {
        viewModelScope.launch {
            repository.updateEntryChecked(entryId, checked)
            load(_uiState.value.listId)
        }
    }
}
