package dev.carince.eraser.feed

import dev.carince.eraser.data.DueItem
import java.time.Instant

sealed interface FeedUiState {
    data class Setup(
        val url: String? = null,
        val error: String? = null,
    ) : FeedUiState

    data object Loading : FeedUiState

    data class Content(
        val items: List<DueItem>,
        val now: Instant,
        val refreshing: Boolean = false,
        val warning: String? = null,
    ) : FeedUiState

    data class Error(
        val message: String,
        val url: String,
    ) : FeedUiState
}
