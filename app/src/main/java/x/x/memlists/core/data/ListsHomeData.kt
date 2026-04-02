package x.x.memlists.core.data

data class ListContainerSummary(
    val id: Long,
    val name: String,
    val comment: String?,
    val parentId: Long?,
    val isFolder: Boolean,
    val isLocked: Boolean,
    val uncheckedCount: Int,
    val totalCount: Int
)

data class ListsHomeData(
    val currentFolderId: Long? = null,
    val currentFolderName: String? = null,
    val containers: List<ListContainerSummary> = emptyList()
)

