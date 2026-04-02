package x.x.memlists.feature.lists

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import x.x.memlists.MemListsApplication

class ListsViewModel(
    application: MemListsApplication
) : AndroidViewModel(application) {
    private val repository = application.repository

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    fun refresh() {
        load(parentId = _uiState.value.currentFolderId)
    }

    fun openFolder(folderId: Long) {
        load(parentId = folderId)
    }

    fun leaveFolder() {
        load(parentId = null)
    }

    private fun load(parentId: Long?) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val home = repository.loadListsHome(parentId = parentId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentFolderId = home.currentFolderId,
                    currentFolderName = home.currentFolderName,
                    containers = home.containers
                )
            }
        }
    }
}

