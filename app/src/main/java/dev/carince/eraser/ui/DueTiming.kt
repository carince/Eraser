package dev.carince.eraser.ui

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

enum class DueBucket {
    OVERDUE,
    TODAY,
    THIS_WEEK,
    LATER,
}

enum class DueUrgency {
    OVERDUE,
    SOON,
    NORMAL,
}

data class RelativeDue(
    val label: String,
    val urgency: DueUrgency,
)

fun relativeDue(
    due: Instant,
    hasTime: Boolean,
    now: Instant,
    zone: ZoneId,
    locale: Locale,
): RelativeDue {
    val today = now.atZone(zone).toLocalDate()
    val dueDate = due.atZone(zone).toLocalDate()

    if (!hasTime) {
        val days = ChronoUnit.DAYS.between(today, dueDate)
        return when {
            days < 0 -> RelativeDue("${-days}d late", DueUrgency.OVERDUE)
            days == 0L -> RelativeDue("Today", DueUrgency.SOON)
            days == 1L -> RelativeDue("Tomorr.", DueUrgency.NORMAL)
            days < 7 -> RelativeDue(dayLabel(dueDate, locale), DueUrgency.NORMAL)
            else -> RelativeDue(dateLabel(dueDate, locale), DueUrgency.NORMAL)
        }
    }

    if (due.isBefore(now)) {
        val minutes = Duration.between(due, now).toMinutes()
        return when {
            minutes < 60 -> RelativeDue("${max(1, minutes)}m late", DueUrgency.OVERDUE)
            minutes < 24 * 60 -> RelativeDue("${minutes / 60}h late", DueUrgency.OVERDUE)
            else -> RelativeDue("${minutes / (24 * 60)}d late", DueUrgency.OVERDUE)
        }
    }

    val minutes = Duration.between(now, due).toMinutes()
    if (minutes < 60) return RelativeDue("${max(1, minutes)}m", DueUrgency.SOON)
    if (minutes < 24 * 60) return RelativeDue("${minutes / 60}h", DueUrgency.SOON)

    val days = ChronoUnit.DAYS.between(today, dueDate)
    return when {
        days == 1L -> RelativeDue("Tomorr.", DueUrgency.NORMAL)
        days < 7 -> RelativeDue(dayLabel(dueDate, locale), DueUrgency.NORMAL)
        else -> RelativeDue(dateLabel(dueDate, locale), DueUrgency.NORMAL)
    }
}

const val OVERDUE_WINDOW_DAYS = 14L

fun isActionable(due: Instant, now: Instant, zone: ZoneId): Boolean {
    val today = now.atZone(zone).toLocalDate()
    val dueDate = due.atZone(zone).toLocalDate()
    return ChronoUnit.DAYS.between(dueDate, today) <= OVERDUE_WINDOW_DAYS
}

fun bucketFor(
    due: Instant,
    hasTime: Boolean,
    now: Instant,
    zone: ZoneId,
): DueBucket {
    val today = now.atZone(zone).toLocalDate()
    val dueDate = due.atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, dueDate)

    if (!hasTime) {
        return when {
            days < 0 -> DueBucket.OVERDUE
            days == 0L -> DueBucket.TODAY
            days < 7 -> DueBucket.THIS_WEEK
            else -> DueBucket.LATER
        }
    }

    if (due.isBefore(now)) return DueBucket.OVERDUE
    return when {
        days == 0L -> DueBucket.TODAY
        days < 7 -> DueBucket.THIS_WEEK
        else -> DueBucket.LATER
    }
}

private fun dayLabel(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern("EEE", locale))

private fun dateLabel(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern("MMM d", locale))
