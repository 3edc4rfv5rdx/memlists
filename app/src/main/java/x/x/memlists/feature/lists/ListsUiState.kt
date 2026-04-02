package x.x.memlists.feature.lists

import x.x.memlists.core.data.ListContainerSummary

data class ListsUiState(
    val isLoading: Boolean = true,
    val currentFolderId: Long? = null,
    val currentFolderName: String? = null,
    val containers: List<ListContainerSummary> = emptyList()
)

