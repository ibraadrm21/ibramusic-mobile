package com.ibramusic.mobile.data.stats

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ibramusic.mobile.domain.model.TopArtistStat
import com.ibramusic.mobile.domain.model.TopTrackStat
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.model.UserStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStatsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "UserStatsManager"
        private const val STATS_FILE_NAME = "accumulated_stats.json"
    }

    private val gson: Gson = Gson()

    private val statsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val statsFile: File get() = File(context.filesDir, STATS_FILE_NAME)

    private val _stats = MutableStateFlow(UserStats())
    val stats: StateFlow<UserStats> = _stats.asStateFlow()

    // In-memory data structures matching ibramusic web accumulatedStats
    private var totalSeconds: Long = 0L
    private val tracksMap = mutableMapOf<String, StoredTrackStat>()
    private val artistsMap = mutableMapOf<String, StoredArtistStat>()

    init {
        statsScope.launch {
            loadLocalStats()
        }
    }

    private suspend fun loadLocalStats(): Unit = mutex.withLock {
        try {
            if (statsFile.exists() && statsFile.length() > 0) {
                val jsonStr = statsFile.readText()
                parseStatsJson(jsonStr)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading local stats: ${e.message}")
        }
        Unit
    }

    private fun parseStatsJson(jsonStr: String) {
        try {
            val root = JsonParser.parseString(jsonStr).asJsonObject
            totalSeconds = root.get("totalSeconds")?.asLong ?: 0L

            tracksMap.clear()
            val tracksObj = root.getAsJsonObject("tracks")
            if (tracksObj != null) {
                for (key in tracksObj.keySet()) {
                    val item = tracksObj.getAsJsonObject(key) ?: continue
                    val tObj = item.getAsJsonObject("track")
                    val id = tObj?.get("id")?.asString ?: key
                    val title = tObj?.get("title")?.asString ?: "Unknown"
                    val artist = tObj?.get("artist")?.asString ?: ""
                    val thumb = tObj?.get("thumbnail")?.asString
                    val count = item.get("count")?.asInt ?: 0
                    val seconds = item.get("seconds")?.asLong ?: 0L

                    tracksMap[id] = StoredTrackStat(id, title, artist, thumb, count, seconds)
                }
            }

            artistsMap.clear()
            val artistsObj = root.getAsJsonObject("artists")
            if (artistsObj != null) {
                for (key in artistsObj.keySet()) {
                    val item = artistsObj.getAsJsonObject(key) ?: continue
                    val name = item.get("name")?.asString ?: key
                    val count = item.get("count")?.asInt ?: 0
                    val seconds = item.get("seconds")?.asLong ?: 0L
                    val thumb = item.get("thumbnail")?.asString

                    artistsMap[name] = StoredArtistStat(name, thumb, count, seconds)
                }
            }

            publishStats()
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing stats JSON: ${e.message}")
        }
    }

    private fun publishStats() {
        val totalPlays = tracksMap.values.sumOf { it.count }

        val topTracks = tracksMap.values
            .sortedWith(compareByDescending<StoredTrackStat> { it.seconds }.thenByDescending { it.count })
            .take(20)
            .map {
                TopTrackStat(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    artworkUrl = it.artworkUrl,
                    count = it.count,
                    seconds = it.seconds
                )
            }

        val topArtists = artistsMap.values
            .sortedWith(compareByDescending<StoredArtistStat> { it.seconds }.thenByDescending { it.count })
            .take(20)
            .map {
                TopArtistStat(
                    name = it.name,
                    artworkUrl = it.artworkUrl,
                    count = it.count,
                    seconds = it.seconds
                )
            }

        _stats.value = UserStats(
            totalSeconds = totalSeconds,
            totalPlays = totalPlays,
            topTracks = topTracks,
            topArtists = topArtists
        )
    }

    private fun persistStats() {
        try {
            val root = JsonObject().apply {
                addProperty("totalSeconds", totalSeconds)

                val tObj = JsonObject()
                for ((id, t) in tracksMap) {
                    val itemObj = JsonObject().apply {
                        val trackObj = JsonObject().apply {
                            addProperty("id", t.id)
                            addProperty("title", t.title)
                            addProperty("artist", t.artist)
                            if (t.artworkUrl != null) addProperty("thumbnail", t.artworkUrl)
                        }
                        add("track", trackObj)
                        addProperty("count", t.count)
                        addProperty("seconds", t.seconds)
                    }
                    tObj.add(id, itemObj)
                }
                add("tracks", tObj)

                val aObj = JsonObject()
                for ((name, a) in artistsMap) {
                    val itemObj = JsonObject().apply {
                        addProperty("name", a.name)
                        addProperty("count", a.count)
                        addProperty("seconds", a.seconds)
                        if (a.artworkUrl != null) addProperty("thumbnail", a.artworkUrl)
                    }
                    aObj.add(name, itemObj)
                }
                add("artists", aObj)
            }

            statsFile.writeText(root.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Error persisting stats: ${e.message}")
        }
    }

    /**
     * Records seconds listened (incremented periodically during active playback).
     */
    fun recordListeningTime(track: Track, seconds: Long) {
        if (seconds <= 0) return
        statsScope.launch {
            mutex.withLock {
                totalSeconds += seconds

                // Update track stat
                val existingT = tracksMap[track.id] ?: StoredTrackStat(
                    id = track.id,
                    title = track.title,
                    artist = track.artistName,
                    artworkUrl = track.artworkUrl,
                    count = 0,
                    seconds = 0L
                )
                tracksMap[track.id] = existingT.copy(seconds = existingT.seconds + seconds)

                // Update artist stat
                val artistName = track.artistName.ifBlank { "Unknown" }
                val existingA = artistsMap[artistName] ?: StoredArtistStat(
                    name = artistName,
                    artworkUrl = track.artworkUrl,
                    count = 0,
                    seconds = 0L
                )
                artistsMap[artistName] = existingA.copy(seconds = existingA.seconds + seconds)

                publishStats()
                persistStats()
            }
        }
    }

    /**
     * Records a play completion (counts when track completes or plays > 30s).
     */
    fun recordPlayCompleted(track: Track) {
        statsScope.launch {
            mutex.withLock {
                val existingT = tracksMap[track.id] ?: StoredTrackStat(
                    id = track.id,
                    title = track.title,
                    artist = track.artistName,
                    artworkUrl = track.artworkUrl,
                    count = 0,
                    seconds = 0L
                )
                tracksMap[track.id] = existingT.copy(count = existingT.count + 1)

                val artistName = track.artistName.ifBlank { "Unknown" }
                val existingA = artistsMap[artistName] ?: StoredArtistStat(
                    name = artistName,
                    artworkUrl = track.artworkUrl,
                    count = 0,
                    seconds = 0L
                )
                artistsMap[artistName] = existingA.copy(count = existingA.count + 1)

                publishStats()
                persistStats()
            }
        }
    }

    /**
     * Merges stats received from Supabase cloud into local stats.
     */
    fun mergeFromCloud(cloudStatsObj: Any?) {
        if (cloudStatsObj == null) return
        statsScope.launch {
            mutex.withLock {
                try {
                    val jsonStr = gson.toJson(cloudStatsObj)
                    val root = JsonParser.parseString(jsonStr).asJsonObject
                    val cloudSecs = root.get("totalSeconds")?.asLong ?: 0L
                    if (cloudSecs > totalSeconds) {
                        totalSeconds = cloudSecs
                    }

                    val tracksObj = root.getAsJsonObject("tracks")
                    if (tracksObj != null) {
                        for (key in tracksObj.keySet()) {
                            val item = tracksObj.getAsJsonObject(key) ?: continue
                            val tObj = item.getAsJsonObject("track")
                            val id = tObj?.get("id")?.asString ?: key
                            val title = tObj?.get("title")?.asString ?: "Unknown"
                            val artist = tObj?.get("artist")?.asString ?: ""
                            val thumb = tObj?.get("thumbnail")?.asString
                            val cCount = item.get("count")?.asInt ?: 0
                            val cSecs = item.get("seconds")?.asLong ?: 0L

                            val local = tracksMap[id]
                            if (local == null) {
                                tracksMap[id] = StoredTrackStat(id, title, artist, thumb, cCount, cSecs)
                            } else {
                                tracksMap[id] = local.copy(
                                    count = maxOf(local.count, cCount),
                                    seconds = maxOf(local.seconds, cSecs)
                                )
                            }
                        }
                    }

                    val artistsObj = root.getAsJsonObject("artists")
                    if (artistsObj != null) {
                        for (key in artistsObj.keySet()) {
                            val item = artistsObj.getAsJsonObject(key) ?: continue
                            val name = item.get("name")?.asString ?: key
                            val cCount = item.get("count")?.asInt ?: 0
                            val cSecs = item.get("seconds")?.asLong ?: 0L
                            val thumb = item.get("thumbnail")?.asString

                            val local = artistsMap[name]
                            if (local == null) {
                                artistsMap[name] = StoredArtistStat(name, thumb, cCount, cSecs)
                            } else {
                                artistsMap[name] = local.copy(
                                    count = maxOf(local.count, cCount),
                                    seconds = maxOf(local.seconds, cSecs)
                                )
                            }
                        }
                    }

                    publishStats()
                    persistStats()
                    Log.d(TAG, "Successfully merged cloud accumulatedStats (totalMinutes: ${totalSeconds / 60})")
                } catch (e: Exception) {
                    Log.w(TAG, "Error merging cloud stats: ${e.message}")
                }
            }
        }
    }

    /**
     * Serializes current stats into a Map suitable for Supabase WebSyncData.
     */
    fun toCloudJson(): Any? {
        if (totalSeconds == 0L && tracksMap.isEmpty()) return null
        return try {
            val root = mutableMapOf<String, Any>()
            root["totalSeconds"] = totalSeconds

            val tMap = mutableMapOf<String, Any>()
            for ((id, t) in tracksMap) {
                val item = mutableMapOf<String, Any>()
                val tInner = mutableMapOf<String, Any>()
                tInner["id"] = t.id
                tInner["title"] = t.title
                tInner["artist"] = t.artist
                if (t.artworkUrl != null) tInner["thumbnail"] = t.artworkUrl
                item["track"] = tInner
                item["count"] = t.count
                item["seconds"] = t.seconds
                tMap[id] = item
            }
            root["tracks"] = tMap

            val aMap = mutableMapOf<String, Any>()
            for ((name, a) in artistsMap) {
                val item = mutableMapOf<String, Any>()
                item["name"] = a.name
                item["count"] = a.count
                item["seconds"] = a.seconds
                if (a.artworkUrl != null) item["thumbnail"] = a.artworkUrl
                aMap[name] = item
            }
            root["artists"] = aMap

            root
        } catch (e: Exception) {
            null
        }
    }

    private data class StoredTrackStat(
        val id: String,
        val title: String,
        val artist: String,
        val artworkUrl: String?,
        val count: Int,
        val seconds: Long
    )

    private data class StoredArtistStat(
        val name: String,
        val artworkUrl: String?,
        val count: Int,
        val seconds: Long
    )
}

