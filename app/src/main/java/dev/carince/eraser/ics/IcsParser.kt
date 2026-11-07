package dev.carince.eraser.ics

import biweekly.Biweekly
import biweekly.util.ICalDate
import dev.carince.eraser.data.DueItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val courseCodePattern = Regex("""\b[A-Z]{2,6}\s?-?\s?\d{3,4}\b""")

fun courseCodeFromSummary(summary: String): String? =
    courseCodePattern.find(summary)?.value?.replace(Regex("""\s+"""), " ")?.trim()

class IcsParser {
    fun parse(text: String, defaultZone: ZoneId): List<DueItem> {
        val calendar = Biweekly.parse(text).first() ?: return emptyList()

        return calendar.events.mapNotNull { event ->
            val uid = event.uid?.value?.trim().orEmpty()
            val start = event.dateStart?.value ?: return@mapNotNull null
            if (uid.isEmpty()) return@mapNotNull null

            val dueHasTime = start.hasTime()
            val dueAt = if (dueHasTime) {
                start.toInstant()
            } else {
                start.localDate(defaultZone).atStartOfDay(defaultZone).toInstant()
            }

            val title = event.summary?.value?.trim().orEmpty()

            DueItem(
                id = uid,
                title = title,
                courseCode = event.categories
                    ?.firstOrNull()
                    ?.values
                    ?.firstOrNull()
                    ?.trim()
                    ?.ifEmpty { null }
                    ?: courseCodeFromSummary(title),
                dueAt = dueAt,
                dueHasTime = dueHasTime,
                rawDue = rawValue(start),
            )
        }
    }

    private fun ICalDate.localDate(zone: ZoneId): LocalDate {
        val raw = rawComponents
        if (raw != null) return LocalDate.of(raw.year, raw.month, raw.date)
        return Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
    }

    private fun rawValue(date: ICalDate): String {
        val raw = date.rawComponents ?: return date.toInstant().toString()
        val day = "%04d-%02d-%02d".format(raw.year, raw.month, raw.date)
        if (!date.hasTime()) return day
        val time = "%02d:%02d:%02d".format(raw.hour, raw.minute, raw.second)
        return if (raw.isUtc) "${day}T${time}Z" else "${day}T${time}"
    }
}
