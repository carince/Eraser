package dev.carince.eraser.data

import java.time.Instant

data class DueItem(
    val id: String,
    val title: String,
    val courseCode: String?,
    val dueAt: Instant?,
    val dueHasTime: Boolean,
    val rawDue: String? = null,
)
