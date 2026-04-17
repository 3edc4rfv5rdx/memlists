package x.x.memlists.core.data

data class ListEntrySummary(
    val id: Long,
    val listId: Long,
    val name: String,
    val quantity: String?,
    val unit: String?,
    val isChecked: Boolean,
    val sortOrder: Int,
    val photoCount: Int,
    val isInDictionary: Boolean = false
)

data class ListDetailData(
    val listId: Long,
    val name: String,
    val comment: String?,
    val uncheckedEntries: List<ListEntrySummary>,
    val checkedEntries: List<ListEntrySummary>
)

data class DictionaryItem(
    val id: Long,
    val name: String,
    val unit: String?
)

