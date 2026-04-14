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
        _uiState.update {
            it.copy(
                savedFolderUserFilter = it.userFilter,
                userFilter = UserFilter()
            )
        }
        load(selectedFolder = folder, newestFirst = newestFirst)
    }

    fun leaveFolder(newestFirst: Boolean) {
        _uiState.update {
            val restored = it.savedFolderUserFilter ?: UserFilter()
            it.copy(userFilter = restored, savedFolderUserFilter = null)
        }
        load(selectedFolder = null, newestFirst = newestFirst)
    }

    fun setTagFilter(tags: Set<String>) {
        _uiState.update { it.copy(selectedTags = tags) }
        reloadAll()
    }

    fun clearTagFilter() {
        _uiState.update { it.copy(selectedTags = emptySet()) }
        reloadAll()
    }

    fun setUserFilter(filter: UserFilter) {
        _uiState.update { it.copy(userFilter = filter) }
        reloadAll()
    }

    fun clearUserFilter() {
        _uiState.update { it.copy(userFilter = UserFilter()) }
        reloadAll()
    }

    fun clearAllFilters() {
        _uiState.update {
            it.copy(selectedTags = emptySet(), userFilter = UserFilter())
        }
        reloadAll()
    }

    private fun reloadAll() {
        val newestFirst = lastNewestFirst ?: return
        load(selectedFolder = _uiState.value.selectedFolder, newestFirst = newestFirst)
    }

    private fun load(selectedFolder: MemoFolderType?, newestFirst: Boolean) {
        _uiState.update { it.copy(isLoading = true, selectedFolder = selectedFolder) }
        viewModelScope.launch {
            val home = repository.loadMemosHome(
                folder = selectedFolder,
                newestFirst = newestFirst,
                selectedTags = _uiState.value.selectedTags,
                userFilter = _uiState.value.userFilter
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
