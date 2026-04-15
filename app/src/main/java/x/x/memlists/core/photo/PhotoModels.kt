package x.x.memlists.core.photo

enum class PhotoOwnerType(val dbValue: String) {
    Memo("memo"),
    Entry("entry");

    companion object {
        fun fromDb(value: String): PhotoOwnerType =
            values().firstOrNull { it.dbValue == value } ?: Memo
    }
}

data class PhotoRef(
    val id: Long,
    val ownerType: PhotoOwnerType,
    val ownerId: Long,
    val path: String,
    val sortOrder: Int
)

const val MAX_PHOTOS_PER_OWNER = 10
