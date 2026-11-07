package dev.carince.eraser.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ItemEntity::class], version = 1, exportSchema = false)
abstract class EraserDatabase : RoomDatabase() {
    abstract fun items(): ItemDao

    companion object {
        @Volatile
        private var instance: EraserDatabase? = null

        fun get(context: Context): EraserDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EraserDatabase::class.java,
                    "eraser.db",
                ).build().also { instance = it }
            }
    }
}
