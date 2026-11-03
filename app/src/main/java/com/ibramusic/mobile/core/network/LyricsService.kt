package com.ibramusic.mobile.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class LyricLine(
    val timeMs: Long,
    val text: String
)

@Singleton
class LyricsService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "LyricsService"
        private val TIME_PATTERN = Pattern.compile("\\[(\\d+):(\\d+)(?:\\.(\\d+))?\\]")
    }

    suspend fun fetchLyrics(artist: String, title: String, durationSec: Long? = null): List<LyricLine> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title
                .replace(Regex("\\(feat\\..*?\\)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\[feat\\..*?\\]", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\(with.*?\\)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\(.*\\)"), "")
                .trim()

            val encodedArtist = URLEncoder.encode(artist.trim(), StandardCharsets.UTF_8.toString())
            val encodedTitle = URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8.toString())

            val url = StringBuilder("https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle")
            if (durationSec != null && durationSec > 0) {
                url.append("&duration=$durationSec")
            }

            val request = Request.Builder()
                .url(url.toString())
                .header("User-Agent", "IbraMusic-Mobile/1.0.0 (https://github.com/ibramusic)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)

            val syncedLyrics = json.optString("syncedLyrics", "")
            if (syncedLyrics.isNotBlank()) {
                return@withContext parseLrc(syncedLyrics)
            }

            val plainLyrics = json.optString("plainLyrics", "")
            if (plainLyrics.isNotBlank()) {
                return@withContext plainLyrics.lines().mapIndexed { index, line ->
                    LyricLine(timeMs = index * 3000L, text = line)
                }
            }

            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch lyrics for $artist - $title: ${e.message}")
            emptyList()
        }
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val lines = lrc.lines()

        for (line in lines) {
            val matcher = TIME_PATTERN.matcher(line)
            val text = matcher.replaceAll("").trim()

            matcher.reset()
            while (matcher.find()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msPart = matcher.group(3)
                val ms = when {
                    msPart == null -> 0L
                    msPart.length == 2 -> (msPart.toLongOrNull() ?: 0L) * 10L
                    msPart.length >= 3 -> (msPart.take(3).toLongOrNull() ?: 0L)
                    else -> (msPart.toLongOrNull() ?: 0L) * 100L
                }
                val totalMs = (min * 60 + sec) * 1000 + ms
                if (text.isNotBlank()) {
                    result.add(LyricLine(timeMs = totalMs, text = text))
                }
            }
        }

        return result.sortedBy { it.timeMs }
    }
}

