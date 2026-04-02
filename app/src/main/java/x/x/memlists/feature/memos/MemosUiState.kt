package x.x.memlists.feature.memos

import x.x.memlists.core.data.MemoFolderType
import x.x.memlists.core.data.MemoFolderSummary
import x.x.memlists.core.data.MemoItemSummary

data class MemosUiState(
    val isLoading: Boolean = true,
    val selectedFolder: MemoFolderType? = null,
    val items: List<MemoItemSummary> = emptyList(),
    val folders: List<MemoFolderSummary> = emptyList()
)

