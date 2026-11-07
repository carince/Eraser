package dev.carince.eraser.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val courseCode: String?,
    val dueAt: Long?,
    val dueHasTime: Boolean,
    val rawDue: String?,
    @ColumnInfo(name = "last_seen") val lastSeen: Long,
)

fun DueItem.toEntity(seenAt: Long): ItemEntity = ItemEntity(
    id = id,
    title = title,
    courseCode = courseCode,
    dueAt = dueAt?.toEpochMilli(),
    dueHasTime = dueHasTime,
    rawDue = rawDue,
    lastSeen = seenAt,
)

fun ItemEntity.toDueItem(): DueItem = DueItem(
    id = id,
    title = title,
    courseCode = courseCode,
    dueAt = dueAt?.let(Instant::ofEpochMilli),
    dueHasTime = dueHasTime,
    rawDue = rawDue,
)
