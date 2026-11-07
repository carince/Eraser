package dev.carince.eraser.feed

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface FeedUrlStore {
    fun read(): String?

    fun write(url: String)
}

@Suppress("DEPRECATION")
class EncryptedFeedUrlStore(context: Context) : FeedUrlStore {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "eraser_feed",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun read(): String? = prefs.getString(KEY_URL, null)

    override fun write(url: String) {
        prefs.edit().putString(KEY_URL, url).apply()
    }

    private companion object {
        const val KEY_URL = "feed_url"
    }
}
