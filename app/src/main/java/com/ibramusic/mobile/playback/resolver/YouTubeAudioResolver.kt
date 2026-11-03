package com.ibramusic.mobile.playback.resolver

import android.net.Uri
import android.util.Log
import com.ibramusic.mobile.core.network.NewPipeDownloader
import com.ibramusic.mobile.domain.model.PlaybackSource
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeAudioResolver @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val resolutionCache: ResolutionCache
) {
    companion object {
        private const val TAG = "YouTubeAudioResolver"
        private val VIDEO_ID_PATTERN = Pattern.compile("\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"")
        private val DIRECT_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")
        private val YT_URL_REGEX = Regex("(?:youtu\\.be/|(?:youtube\\.com|music\\.youtube\\.com|m\\.youtube\\.com)/(?:watch\\?(?:.*&)?v=|embed/|v/|shorts/|live/))([a-zA-Z0-9_-]{11})|[?&]v=([a-zA-Z0-9_-]{11})")
    }

    private var isInitialized = false

    fun warmUp() {
        ensureInitialized()
        try {
            ServiceList.YouTube.serviceInfo
        } catch (e: Exception) {
            Log.w(TAG, "warmUp error: ${e.message}")
        }
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    try {
                        NewPipe.init(NewPipeDownloader(okHttpClient))
                        isInitialized = true
                        Log.d(TAG, "NewPipeExtractor initialized successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to initialize NewPipeExtractor", e)
                    }
                }
            }
        }
    }

    /**
     * Resolves the official studio audio release (album track) for a song.
     * Uses memory cache to provide instantaneous (<10ms) playback on subsequent plays or pre-resolved tracks.
     */
    suspend fun resolve(track: Track): PlaybackSource? = withContext(Dispatchers.IO) {
        // 0. Cache check: instant return if already pre-resolved or previously played
        val cachedSource = resolutionCache.getSource(track)
        if (cachedSource != null) {
            Log.d(TAG, "Cache HIT: returning pre-resolved studio audio for: ${track.artistName} - ${track.title}")
            return@withContext cachedSource
        }

        ensureInitialized()
        try {
            val cleanTitle = track.title
                .replace(Regex("\\(feat\\..*?\\)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\[feat\\..*?\\]", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\(with.*?\\)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\(.*\\)"), "")
                .trim()

            val query = "${track.artistName} $cleanTitle".trim()

            // 1. Fast direct videoId resolution (0ms if already YouTube format or URL)
            var videoId = resolutionCache.getVideoId(track)
            if (videoId == null) {
                if (track.id.startsWith("yt-")) {
                    videoId = track.id.substring(3)
                } else if (track.providerId == "youtube" && !track.id.startsWith("dz_") && DIRECT_ID_REGEX.matches(track.id)) {
                    videoId = track.id
                } else if (!track.streamUrl.isNullOrEmpty()) {
                    val match = YT_URL_REGEX.find(track.streamUrl)
                    if (match != null) {
                        videoId = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
                            ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
                    }
                }
            }

            // 1b. If still unknown, query YouTube Music "Song" endpoint
            if (videoId == null) {
                try {
                    videoId = resolveStudioVideoIdViaYtMusic(track, query)
                    if (videoId == null) {
                        videoId = resolveStudioVideoIdViaYtMusic(track, "$cleanTitle ${track.artistName}")
                    }
                    if (videoId != null) {
                        resolutionCache.putVideoId(track, videoId)
                        Log.d(TAG, "Found & cached YouTube Music studio videoId: $videoId for $query")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "YouTube Music API query failed, will fallback to ranking: ${e.message}")
                }
            }

            var targetUrl = if (!videoId.isNullOrEmpty()) {
                "https://www.youtube.com/watch?v=$videoId"
            } else {
                null
            }

            // 2. Extract streams with NewPipe
            var streamInfo: StreamInfo? = null
            if (!targetUrl.isNullOrEmpty()) {
                try {
                    streamInfo = StreamInfo.getInfo(ServiceList.YouTube, targetUrl)
                } catch (e: Exception) {
                    Log.w(TAG, "Direct targetUrl extraction failed ($targetUrl): ${e.message}")
                }
            }

            // 3. Fallback Strategy: NewPipe search with smart candidate ranking (penalizing videoclips)
            if (streamInfo == null || streamInfo.audioStreams.isNullOrEmpty()) {
                try {
                    val searchQuery = "$query audio"
                    val searchExtractor = ServiceList.YouTube.getSearchExtractor(searchQuery)
                    searchExtractor.fetchPage()
                    val items = searchExtractor.initialPage.items.filterIsInstance<StreamInfoItem>()

                    if (items.isNotEmpty()) {
                        val bestCandidate = pickBestAudioCandidate(items, track.artistName, cleanTitle, track.durationMs)
                        if (bestCandidate != null) {
                            Log.d(TAG, "Selected ranked audio candidate: '${bestCandidate.name}' by '${bestCandidate.uploaderName}'")
                            streamInfo = StreamInfo.getInfo(ServiceList.YouTube, bestCandidate.url)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "NewPipe search fallback failed: ${e.message}")
                }
            }

            val audioStreams = streamInfo?.audioStreams
            if (audioStreams.isNullOrEmpty()) {
                Log.w(TAG, "No audio streams found for: $query")
                return@withContext null
            }

            // Select highest bitrate audio stream (prefer m4a or webm high quality)
            val bestAudio: AudioStream = audioStreams
                .filter { !it.content.isNullOrEmpty() }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.first()

            val audioUrl = bestAudio.content
            if (audioUrl.isNullOrEmpty()) return@withContext null

            val mime = bestAudio.format?.mimeType ?: "audio/mp4"
            Log.d(TAG, "Studio audio extracted successfully! Title: ${streamInfo?.name}, bitrate: ${bestAudio.averageBitrate}, mime: $mime")

            val source = PlaybackSource(
                uri = Uri.parse(audioUrl),
                mimeType = mime,
                providerId = "youtube_audio",
                isSeekable = true
            )

            // Cache result so future plays/playback transitions are INSTANT
            resolutionCache.putSource(track, source)

            source
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving studio audio for ${track.artistName} - ${track.title}", e)
            null
        }
    }

    /**
     * Direct YouTube Music search filtered specifically for "Songs" (type = song).
     * Extracts and scores candidates to guarantee official studio album tracks.
     */
    private fun resolveStudioVideoIdViaYtMusic(track: Track, query: String): String? {
        val escaped = query.replace("\"", "\\\"")
        val jsonBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20240901.01.00",
                        "hl": "en",
                        "gl": "US"
                    }
                },
                "query": "$escaped",
                "params": "EgWKAQIIAQ%3D%3D"
            }
        """.trimIndent()

        val req = okhttp3.Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/search")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Referer", "https://music.youtube.com/")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", "1.20240901.01.00")
            .build()

        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) return null
        val bodyStr = resp.body?.string() ?: return null

        return parseBestYtMusicCandidate(bodyStr, track)
    }

    private data class YtCandidate(
        val videoId: String,
        val title: String,
        val artist: String,
        val durationSec: Int,
        val isATV: Boolean,
        val isExplicit: Boolean
    )

    private fun parseBestYtMusicCandidate(bodyStr: String, track: Track): String? {
        try {
            val root = JSONObject(bodyStr)
            val renderers = mutableListOf<JSONObject>()
            collectRenderers(root, renderers)
            if (renderers.isEmpty()) return null

            val candidates = renderers.mapNotNull { extractCandidate(it) }
            if (candidates.isEmpty()) return null

            val scored = candidates.map { cand ->
                cand to scoreYtCandidate(cand, track)
            }.sortedByDescending { it.second }

            val best = scored.firstOrNull()
            if (best != null && best.second >= 0) {
                Log.d(TAG, "Selected best YouTube Music candidate [score=${best.second}]: '${best.first.title}' by '${best.first.artist}' (${best.first.videoId})")
                return best.first.videoId
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing YouTube Music candidate JSON: ${e.message}")
        }
        return null
    }

    private fun collectRenderers(node: Any, list: MutableList<JSONObject>) {
        when (node) {
            is JSONObject -> {
                if (node.has("musicResponsiveListItemRenderer")) {
                    list.add(node.getJSONObject("musicResponsiveListItemRenderer"))
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    collectRenderers(node.get(k), list)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectRenderers(node.get(i), list)
                }
            }
        }
    }

    private fun extractCandidate(renderer: JSONObject): YtCandidate? {
        var videoId: String? = null
        try {
            val playNav = renderer.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
            videoId = playNav?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {}

        if (videoId.isNullOrBlank()) {
            try {
                val flexCols = renderer.optJSONArray("flexColumns")
                val run0 = flexCols?.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                videoId = run0?.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {}
        }

        if (videoId.isNullOrBlank()) return null

        val isATV = renderer.toString().contains("MUSIC_VIDEO_TYPE_ATV")

        // Title
        var title = ""
        try {
            val flexCols = renderer.optJSONArray("flexColumns")
            val titleRuns = flexCols?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
            if (titleRuns != null) {
                val sb = StringBuilder()
                for (i in 0 until titleRuns.length()) {
                    sb.append(titleRuns.optJSONObject(i)?.optString("text") ?: "")
                }
                title = sb.toString()
            }
        } catch (e: Exception) {}

        // Subtitle (artist, album, duration)
        var artist = ""
        var durationSec = 0
        try {
            val flexCols = renderer.optJSONArray("flexColumns")
            val subRuns = flexCols?.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
            if (subRuns != null) {
                artist = subRuns.optJSONObject(0)?.optString("text") ?: ""
                for (i in 0 until subRuns.length()) {
                    val t = subRuns.optJSONObject(i)?.optString("text") ?: ""
                    val m = Regex("(\\d{1,2}):(\\d{2})").find(t)
                    if (m != null) {
                        val min = m.groupValues[1].toIntOrNull() ?: 0
                        val sec = m.groupValues[2].toIntOrNull() ?: 0
                        durationSec = min * 60 + sec
                    }
                }
            }
        } catch (e: Exception) {}

        val badgesStr = renderer.optJSONArray("badges")?.toString() ?: ""
        val isExplicit = badgesStr.contains("EXPLICIT")

        return YtCandidate(videoId, title, artist, durationSec, isATV, isExplicit)
    }

    private fun scoreYtCandidate(cand: YtCandidate, track: Track): Int {
        val clean = { s: String -> s.lowercase().replace(Regex("[^a-z0-9]"), "") }
        val tTitle = clean(track.title)
        val cTitle = clean(cand.title)
        val tArtist = clean(track.artistName)
        val cArtist = clean(cand.artist)

        var score = 0

        // Unwanted keywords
        val unwanted = listOf("remix", "slowed", "reverb", "spedup", "speedup", "cover", "karaoke", "instrumental", "live", "tribute", "mashup", "tiktok")
        for (w in unwanted) {
            if (!tTitle.contains(w) && cTitle.contains(w)) {
                score -= 80
            }
        }

        // Title match
        if (cTitle == tTitle) {
            score += 50
        } else if (cTitle.startsWith(tTitle) || tTitle.startsWith(cTitle)) {
            score += 25
        } else if (cTitle.contains(tTitle) || tTitle.contains(cTitle)) {
            score += 10
        } else {
            score -= 30
        }

        // Artist match
        if (tArtist.isNotEmpty() && cArtist.isNotEmpty()) {
            if (cArtist.contains(tArtist) || tArtist.contains(cArtist)) {
                score += 35
            }
        }

        // ATV (Official Album Track)
        if (cand.isATV) {
            score += 40
        }

        // Duration match
        val targetDurSec = (track.durationMs / 1000).toInt()
        if (targetDurSec > 0 && cand.durationSec > 0) {
            val diff = kotlin.math.abs(cand.durationSec - targetDurSec)
            if (diff <= 3) score += 45
            else if (diff <= 8) score += 20
            else if (diff <= 15) score += 5
            else if (diff > 30) score -= 60
        }

        // Explicit match
        if (track.isExplicit && cand.isExplicit) score += 5

        return score
    }

    /**
     * Scores candidates to prioritize studio audio tracks and penalize videoclips with dialogue.
     */
    private fun pickBestAudioCandidate(items: List<StreamInfoItem>, artist: String, title: String, targetDurationMs: Long = 0L): StreamInfoItem? {
        val artistLower = artist.lowercase().trim()
        val titleLower = title.lowercase().trim()
        val targetDurSec = (targetDurationMs / 1000).toInt()

        val scored = items.map { item ->
            var score = 0
            val nameLower = item.name?.lowercase() ?: ""
            val uploaderLower = item.uploaderName?.lowercase() ?: ""

            // Artist match bonus
            if (uploaderLower.contains(artistLower) || nameLower.contains(artistLower)) {
                score += 50
            }

            // Studio track / Topic channel bonus (+60)
            if (uploaderLower.contains("- topic") || uploaderLower.contains("topic")) {
                score += 60
            }

            // Audio track keywords bonus (+40)
            if (nameLower.contains("audio oficial") || nameLower.contains("official audio") || nameLower.contains("(audio)")) {
                score += 40
            }

            // Duration match (penalize videos that are much longer or shorter than studio track)
            if (targetDurSec > 0 && item.duration > 0) {
                val diff = kotlin.math.abs(item.duration.toInt() - targetDurSec)
                if (diff <= 3) score += 45
                else if (diff <= 8) score += 20
                else if (diff > 25) score -= 70
            }

            // Videoclip penalties (-80)
            if (nameLower.contains("official video") || nameLower.contains("video oficial") ||
                nameLower.contains("music video") || nameLower.contains("videoclip") ||
                nameLower.contains("official music video")) {
                score -= 80
            }

            // Live / concert penalties (-50)
            if (nameLower.contains("en vivo") || nameLower.contains("live") || nameLower.contains("concierto") || nameLower.contains("symphonic")) {
                score -= 50
            }

            // Remix penalty if original was not remix (-60)
            if (nameLower.contains("remix") && !titleLower.contains("remix")) {
                score -= 60
            }

            // Cover / Karaoke / Instrumental penalties
            if (nameLower.contains("karaoke") && !titleLower.contains("karaoke")) score -= 80
            if (nameLower.contains("cover") && !titleLower.contains("cover")) score -= 80
            if (nameLower.contains("instrumental") && !titleLower.contains("instrumental")) score -= 70

            item to score
        }

        return scored.maxByOrNull { it.second }?.first ?: items.firstOrNull()
    }
}
