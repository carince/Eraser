package dev.carince.eraser.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.carince.eraser.data.ItemCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class FeedViewModel(
    private val repository: FeedRepository,
    private val store: FeedUrlStore,
    private val cache: ItemCache,
    private val zone: ZoneId,
) : ViewModel() {
    private var savedUrl: String? = store.read()
    private var refreshJob: Job? = null

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { cache.read() }
            val url = savedUrl

            if (url == null) {
                _state.value = if (cached.isEmpty()) {
                    FeedUiState.Setup()
                } else {
                    FeedUiState.Content(items = cached, now = Instant.now())
                }
                return@launch
            }

            if (cached.isNotEmpty()) {
                _state.value = FeedUiState.Content(
                    items = cached,
                    now = Instant.now(),
                    refreshing = true,
                )
            }
            refresh(url, keepExisting = cached.isNotEmpty())
        }
    }

    fun saveAndLoad(raw: String) {
        val url = normalizeFeedUrl(raw)
        if (url == null) {
            _state.value = FeedUiState.Setup(
                url = raw.trim().ifEmpty { null },
                error = "That doesn't look like a calendar link.",
            )
            return
        }
        store.write(url)
        savedUrl = url
        refresh(url, keepExisting = false)
    }

    fun refresh() {
        val url = savedUrl
        if (url == null) {
            _state.value = FeedUiState.Setup()
            return
        }
        refresh(url, keepExisting = true)
    }

    fun editFeed() {
        _state.value = FeedUiState.Setup(url = savedUrl)
    }

    private fun refresh(url: String, keepExisting: Boolean) {
        val existing = if (keepExisting) {
            (_state.value as? FeedUiState.Content)?.items.orEmpty()
        } else {
            emptyList()
        }

        _state.value = if (existing.isEmpty()) {
            FeedUiState.Loading
        } else {
            FeedUiState.Content(items = existing, now = Instant.now(), refreshing = true)
        }

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) { repository.load(url, zone) }) {
                is FeedResult.Success -> {
                    withContext(Dispatchers.IO) {
                        cache.replace(result.items, System.currentTimeMillis())
                    }
                    _state.value = FeedUiState.Content(items = result.items, now = Instant.now())
                }

                is FeedResult.Failure -> {
                    _state.value = if (existing.isEmpty()) {
                        FeedUiState.Error(message = result.message, url = url)
                    } else {
                        FeedUiState.Content(
                            items = existing,
                            now = Instant.now(),
                            warning = "Couldn't refresh. Showing saved items.",
                        )
                    }
                }
            }
        }
    }
}
