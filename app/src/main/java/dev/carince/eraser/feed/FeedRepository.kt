package dev.carince.eraser.feed

import dev.carince.eraser.data.DueItem
import dev.carince.eraser.ics.IcsParser
import java.time.ZoneId

class FeedException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun interface IcsFeedSource {
    fun fetch(url: String): String
}

sealed interface FeedResult {
    data class Success(val items: List<DueItem>) : FeedResult

    data class Failure(val message: String) : FeedResult
}

fun feedErrorMessage(status: Int): String = when (status) {
    401, 403 -> "The calendar link was rejected. Copy a fresh link from Blackboard."
    404 -> "That calendar link no longer exists."
    else -> "Blackboard returned HTTP $status."
}

class FeedRepository(
    private val source: IcsFeedSource,
    private val parser: IcsParser = IcsParser(),
) {
    fun load(url: String, zone: ZoneId): FeedResult = try {
        val items = parser.parse(source.fetch(url), zone)
        FeedResult.Success(items.sortedWith(compareBy(nullsLast()) { it.dueAt }))
    } catch (error: FeedException) {
        FeedResult.Failure(error.message ?: "Could not read the calendar feed.")
    } catch (_: Exception) {
        FeedResult.Failure("Could not read the calendar feed.")
    }
}
