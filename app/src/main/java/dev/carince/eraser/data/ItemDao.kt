package dev.carince.eraser.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class ItemDao {
    @Query("SELECT * FROM items ORDER BY dueAt ASC")
    abstract suspend fun all(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(items: List<ItemEntity>)

    @Query("DELETE FROM items")
    abstract suspend fun clear()

    @Query("DELETE FROM items WHERE id NOT IN (:ids)")
    abstract suspend fun deleteMissing(ids: List<String>)

    @Transaction
    open suspend fun replaceAll(items: List<ItemEntity>) {
        if (items.isEmpty()) {
            clear()
            return
        }
        upsert(items)
        deleteMissing(items.map { it.id })
    }
}
