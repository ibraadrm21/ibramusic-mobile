package com.ibramusic.mobile.playback.resolver

import android.util.Log
import android.util.LruCache
import com.ibramusic.mobile.domain.model.PlaybackSource
import com.ibramusic.mobile.domain.model.Track
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResolutionCache @Inject constructor() {

    companion object {
        private const val TAG = "ResolutionCache"
        private const val MAX_STREAM_CACHE = 100
        private const val MAX_VIDEO_ID_CACHE = 300
    }

    // Cache of full resolved PlaybackSource objects
    private val streamCache = object : LruCache<String, CachedSource>(MAX_STREAM_CACHE) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: CachedSource?, newValue: CachedSource?) {
            if (evicted) {
                Log.d(TAG, "Evicted cached stream for key: $key")
            }
        }
    }

    // Cache of resolved studio videoIds
    private val videoIdCache = ConcurrentHashMap<String, String>(MAX_VIDEO_ID_CACHE)

    private fun getCacheKey(track: Track): String {
        return track.id
    }

    private fun getNormalizedKey(track: Track): String {
        val cleanTitle = track.title.lowercase().replace(Regex("[^a-z0-9]"), "")
        val cleanArtist = track.artistName.lowercase().replace(Regex("[^a-z0-9]"), "")
        return "$cleanArtist-$cleanTitle"
    }

    @Synchronized
    fun getSource(track: Track): PlaybackSource? {
        val primaryKey = getCacheKey(track)
        val normalizedKey = getNormalizedKey(track)
        val cached = streamCache.get(primaryKey) ?: streamCache.get(normalizedKey) ?: return null

        // Ignore and purge any legacy Saavn entries
        if (cached.source.providerId == "saavn_direct" || cached.source.providerId == "saavn") {
            streamCache.remove(primaryKey)
            streamCache.remove(normalizedKey)
            return null
        }

        // Check if stream URL is still valid (streams are typically valid for at least 4 hours)
        val ageMs = System.currentTimeMillis() - cached.timestamp
        if (ageMs > 4 * 60 * 60 * 1000L) { // 4 hours
            streamCache.remove(primaryKey)
            streamCache.remove(normalizedKey)
            return null
        }

        Log.d(TAG, "Cache HIT for track stream: ${track.artistName} - ${track.title}")
        return cached.source
    }

    @Synchronized
    fun putSource(track: Track, source: PlaybackSource) {
        val primaryKey = getCacheKey(track)
        val normalizedKey = getNormalizedKey(track)
        val entry = CachedSource(source, System.currentTimeMillis())
        streamCache.put(primaryKey, entry)
        if (normalizedKey.isNotBlank()) {
            streamCache.put(normalizedKey, entry)
        }
        Log.d(TAG, "Cached playback source for: ${track.artistName} - ${track.title} (size: ${streamCache.size()})")
    }

    fun getVideoId(track: Track): String? {
        return videoIdCache[getCacheKey(track)] ?: videoIdCache[getNormalizedKey(track)]
    }

    fun putVideoId(track: Track, videoId: String) {
        val primaryKey = getCacheKey(track)
        val normalizedKey = getNormalizedKey(track)
        videoIdCache[primaryKey] = videoId
        if (normalizedKey.isNotBlank()) {
            videoIdCache[normalizedKey] = videoId
        }
        Log.d(TAG, "Cached studio videoId $videoId for: ${track.artistName} - ${track.title}")
    }

    @Synchronized
    fun clear() {
        streamCache.evictAll()
        videoIdCache.clear()
    }

    private data class CachedSource(
        val source: PlaybackSource,
        val timestamp: Long
    )
}

