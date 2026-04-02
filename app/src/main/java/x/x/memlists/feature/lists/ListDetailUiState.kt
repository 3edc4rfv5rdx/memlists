package x.x.memlists.feature.lists

import x.x.memlists.core.data.ListEntrySummary

data class ListDetailUiState(
    val isLoading: Boolean = true,
    val listId: Long = -1L,
    val title: String = "",
    val comment: String? = null,
    val uncheckedEntries: List<ListEntrySummary> = emptyList(),
    val checkedEntries: List<ListEntrySummary> = emptyList()
)

