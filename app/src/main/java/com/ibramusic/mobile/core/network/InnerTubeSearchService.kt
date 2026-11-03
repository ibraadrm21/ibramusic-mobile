package com.ibramusic.mobile.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InnerTubeSearchService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "InnerTubeSearchService"
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1/music/get_search_suggestions"
        private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private fun getClientVersion(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dateStr = sdf.format(Date())
        return "1.$dateStr.01.00"
    }

    /**
     * Fetches real-time search autocomplete suggestions using InnerTube WEB_REMIX endpoint.
     * Returns a list of suggestion strings matching what the user is typing.
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val clientContext = JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", getClientVersion())
                    put("hl", "es")
                    put("gl", "ES")
                })
                put("user", JSONObject())
            }

            val payload = JSONObject().apply {
                put("context", clientContext)
                put("input", query)
            }

            val url = "$BASE_URL?prettyPrint=false&alt=json&key=$API_KEY"

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Suggestion HTTP call failed: ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(body)

            val suggestions = mutableListOf<String>()
            val contents = root.optJSONArray("contents") ?: return@withContext emptyList()
            if (contents.length() == 0) return@withContext emptyList()

            val section = contents.optJSONObject(0)?.optJSONObject("searchSuggestionsSectionRenderer")
            val items = section?.optJSONArray("contents") ?: return@withContext emptyList()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val suggestionRenderer = item.optJSONObject("searchSuggestionRenderer") ?: continue

                // 1. Try navigationEndpoint.searchEndpoint.query
                val directQuery = suggestionRenderer
                    .optJSONObject("navigationEndpoint")
                    ?.optJSONObject("searchEndpoint")
                    ?.optString("query")

                if (!directQuery.isNullOrBlank()) {
                    suggestions.add(directQuery)
                    continue
                }

                // 2. Fallback to runs text inside suggestion
                val runs = suggestionRenderer.optJSONObject("suggestion")?.optJSONArray("runs")
                if (runs != null && runs.length() > 0) {
                    val sb = StringBuilder()
                    for (r in 0 until runs.length()) {
                        val text = runs.optJSONObject(r)?.optString("text")
                        if (!text.isNullOrEmpty()) {
                            sb.append(text)
                        }
                    }
                    val combined = sb.toString().trim()
                    if (combined.isNotBlank()) {
                        suggestions.add(combined)
                    }
                }
            }

            Log.d(TAG, "Found ${suggestions.size} autocomplete suggestions for: '$query'")
            return@withContext suggestions
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching suggestions for '$query': ${e.message}")
            return@withContext emptyList()
        }
    }
}

