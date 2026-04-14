package x.x.memlists.feature.memos

enum class HasReminder { Any, Yes, No }

data class UserFilter(
    val dateFrom: Int? = null,
    val dateTo: Int? = null,
    val tags: List<String> = emptyList(),
    val priorityMin: Int = 0,
    val hasReminder: HasReminder = HasReminder.Any
) {
    val isActive: Boolean
        get() = dateFrom != null ||
            dateTo != null ||
            tags.isNotEmpty() ||
            priorityMin > 0 ||
            hasReminder != HasReminder.Any
}
