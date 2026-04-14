package x.x.memlists.feature.memos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import x.x.memlists.core.data.MemoFolderType

class MemosViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = (application as x.x.memlists.MemListsApplication).repository

    private val _uiState = MutableStateFlow(MemosUiState())
    val uiState: StateFlow<MemosUiState> = _uiState.asStateFlow()

    private var lastNewestFirst: Boolean? = null

    fun refresh(newestFirst: Boolean) {
        lastNewestFirst = newestFirst
        load(selectedFolder = _uiState.value.selectedFolder, newestFirst = newestFirst)
    }

    fun refreshIfNeeded(newestFirst: Boolean) {
        if (lastNewestFirst != newestFirst) {
            refresh(newestFirst)
        }
    }

    fun openFolder(folder: MemoFolderType, newestFirst: Boolean) {
        load(selectedFolder = folder, newestFirst = newestFirst)
    }

    fun leaveFolder(newestFirst: Boolean) {
        load(selectedFolder = null, newestFirst = newestFirst)
    }

    fun setTagFilter(tags: Set<String>) {
        _uiState.update { it.copy(selectedTags = tags) }
        reloadFolders()
    }

    fun clearTagFilter() {
        _uiState.update { it.copy(selectedTags = emptySet()) }
        reloadFolders()
    }

    private fun reloadFolders() {
        val folder = _uiState.value.selectedFolder
        if (folder != null) return
        val newestFirst = lastNewestFirst ?: return
        viewModelScope.launch {
            val home = repository.loadMemosHome(
                folder = null,
                newestFirst = newestFirst,
                selectedTags = _uiState.value.selectedTags
            )
            _uiState.update { it.copy(folders = home.folders) }
        }
    }

    private fun load(selectedFolder: MemoFolderType?, newestFirst: Boolean) {
        _uiState.update { it.copy(isLoading = true, selectedFolder = selectedFolder) }
        viewModelScope.launch {
            val home = repository.loadMemosHome(
                folder = selectedFolder,
                newestFirst = newestFirst,
                selectedTags = _uiState.value.selectedTags
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
