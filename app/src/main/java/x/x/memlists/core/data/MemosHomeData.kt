package x.x.memlists.core.data

enum class MemoFolderType(val titleKey: String) {
    Notes("Notes"),
    Daily("Daily Reminders"),
    Periods("Periods"),
    Monthly("Monthly Events"),
    Yearly("Yearly Events")
}

data class MemoItemSummary(
    val id: Long,
    val title: String,
    val content: String?,
    val tags: String?,
    val priority: Int,
    val created: Int,
    val hidden: Boolean,
    val reminderType: Int,
    val active: Boolean,
    val date: Int?,
    val time: Int?,
    val timesJson: String?,
    val dateTo: Int?,
    val daysMask: Int?,
    val yearly: Boolean,
    val monthly: Boolean,
    val photoCount: Int
)

data class MemoFolderSummary(
    val type: MemoFolderType,
    val count: Int
)

data class MemosHomeData(
    val items: List<MemoItemSummary> = emptyList(),
    val folders: List<MemoFolderSummary> = emptyList()
)

