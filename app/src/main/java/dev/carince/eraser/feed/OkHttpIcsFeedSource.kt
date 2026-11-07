package dev.carince.eraser.feed

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpIcsFeedSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build(),
) : IcsFeedSource {
    override fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Eraser/0.1 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw FeedException(feedErrorMessage(response.code))
                }
                return response.body.string()
            }
        } catch (error: FeedException) {
            throw error
        } catch (_: IOException) {
            throw FeedException("Couldn't reach Blackboard. Check your connection.")
        }
    }
}
