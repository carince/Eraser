package dev.carince.eraser.data

interface ItemCache {
    suspend fun read(): List<DueItem>

    suspend fun replace(items: List<DueItem>, seenAt: Long)
}

class RoomItemCache(private val dao: ItemDao) : ItemCache {
    override suspend fun read(): List<DueItem> = dao.all().map { it.toDueItem() }

    override suspend fun replace(items: List<DueItem>, seenAt: Long) {
        dao.replaceAll(items.map { it.toEntity(seenAt) })
    }
}
