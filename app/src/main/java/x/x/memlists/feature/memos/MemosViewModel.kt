package x.x.memlists.feature.memos

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import x.x.memlists.MemListsApplication
import x.x.memlists.core.data.MemoFolderType

class MemosViewModel(
    application: MemListsApplication
) : AndroidViewModel(application) {
    private val repository = application.repository

    private val _uiState = MutableStateFlow(MemosUiState())
    val uiState: StateFlow<MemosUiState> = _uiState.asStateFlow()

    fun refresh(newestFirst: Boolean) {
        load(selectedFolder = _uiState.value.selectedFolder, newestFirst = newestFirst)
    }

    fun openFolder(folder: MemoFolderType, newestFirst: Boolean) {
        load(selectedFolder = folder, newestFirst = newestFirst)
    }

    fun leaveFolder(newestFirst: Boolean) {
        load(selectedFolder = null, newestFirst = newestFirst)
    }

    private fun load(selectedFolder: MemoFolderType?, newestFirst: Boolean) {
        _uiState.update { it.copy(isLoading = true, selectedFolder = selectedFolder) }
        viewModelScope.launch {
            val home = repository.loadMemosHome(
                folder = selectedFolder,
                newestFirst = newestFirst
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedFolder = selectedFolder,
                    items = home.items,
                    folders = home.folders
                )
            }
        }
    }
}

