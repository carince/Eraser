package dev.carince.eraser.feed

import java.net.URI

fun normalizeFeedUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    val withScheme = if (trimmed.startsWith("webcal://", ignoreCase = true)) {
        "https://" + trimmed.substring("webcal://".length)
    } else {
        trimmed
    }

    val uri = try {
        URI(withScheme)
    } catch (_: Exception) {
        return null
    }

    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    if (uri.host.isNullOrBlank()) return null
    return withScheme
}
