package com.ibramusic.mobile.data.sync

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ibramusic.mobile.core.common.Constants
import com.ibramusic.mobile.core.di.AuthDataStore
import com.ibramusic.mobile.core.network.SupabaseApiService
import com.ibramusic.mobile.data.local.dao.PlaylistDao
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.PlaylistEntity
import com.ibramusic.mobile.data.local.entity.PlaylistTrackEntity
import com.ibramusic.mobile.data.local.entity.TrackEntity
import com.ibramusic.mobile.data.remote.dto.SupabaseRefreshRequest
import com.ibramusic.mobile.data.remote.dto.SupabaseSyncRow
import com.ibramusic.mobile.data.remote.dto.WebArtistDto
import com.ibramusic.mobile.data.remote.dto.WebPlaylistDto
import com.ibramusic.mobile.data.remote.dto.WebSyncData
import com.ibramusic.mobile.data.remote.dto.WebTrackDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @AuthDataStore private val authDataStore: DataStore<Preferences>,
    private val supabaseApiService: SupabaseApiService,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val userStatsManager: com.ibramusic.mobile.data.stats.UserStatsManager
) {
    companion object {
        private const val TAG = "SupabaseSync"
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_EXPIRES_AT = longPreferencesKey("expires_at")
        private val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
    }

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debouncedSyncJob: Job? = null
    private var lastKnownCloudUpdatedAt: String? = null

    init {
        // Continuous background Realtime poller: checks for remote changes every 15s
        syncScope.launch {
            while (true) {
                delay(15_000L)
                try {
                    val creds = getFreshToken()
                    if (creds != null) {
                        val (userId, token) = creds
                        val checkResponse = supabaseApiService.getUserSync(
                            apiKey = Constants.SUPABASE_ANON_KEY,
                            authorization = "Bearer $token",
                            idFilter = "eq.$userId",
                            select = "id,updated_at"
                        )
                        if (checkResponse.isSuccessful) {
                            val row = checkResponse.body()?.firstOrNull()
                            val cloudUpdatedAt = row?.updatedAt
                            if (cloudUpdatedAt != null && cloudUpdatedAt != lastKnownCloudUpdatedAt) {
                                if (lastKnownCloudUpdatedAt != null) {
                                    Log.d(TAG, "Realtime cloud modification detected ($cloudUpdatedAt). Syncing down...")
                                    syncFromCloud(userId, token)
                                }
                                lastKnownCloudUpdatedAt = cloudUpdatedAt
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Silent retry on network blips
                }
            }
        }
    }

    fun triggerInstantSync() {
        syncScope.launch {
            val creds = getFreshToken() ?: return@launch
            syncFromCloud(creds.first, creds.second)
        }
    }

    /**
     * Schedules a debounced push of local data to Supabase cloud (1.5 seconds inactivity).
     */
    fun scheduleSyncToCloud() {
        debouncedSyncJob?.cancel()
        debouncedSyncJob = syncScope.launch {
            delay(1500L)
            val creds = getFreshToken() ?: return@launch
            Log.d(TAG, "Triggering scheduled auto-sync to Supabase cloud...")
            syncToCloud(creds.first, creds.second)
        }
    }

    private suspend fun getFreshToken(): Pair<String, String>? {
        val prefs = authDataStore.data.firstOrNull() ?: return null
        val isGuest = prefs[KEY_IS_GUEST] ?: true
        if (isGuest) return null

        val userId = prefs[KEY_USER_ID] ?: return null
        var token = prefs[KEY_ACCESS_TOKEN] ?: return null
        val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
        val refreshToken = prefs[KEY_REFRESH_TOKEN]

        val now = System.currentTimeMillis()
        if (expiresAt > 0 && now >= expiresAt - 60_000L && !refreshToken.isNullOrEmpty()) {
            try {
                Log.d(TAG, "Refreshing expired token before sync...")
                val response = supabaseApiService.refreshToken(
                    apiKey = Constants.SUPABASE_ANON_KEY,
                    authorization = "Bearer ${Constants.SUPABASE_ANON_KEY}",
                    request = SupabaseRefreshRequest(refreshToken)
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    token = body.accessToken ?: token
                    val newRefreshToken = body.refreshToken ?: refreshToken
                    val expiresInSeconds = body.expiresIn ?: 3600L
                    val newExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
                    authDataStore.edit { p ->
                        p[KEY_ACCESS_TOKEN] = token
                        p[KEY_REFRESH_TOKEN] = newRefreshToken
                        p[KEY_EXPIRES_AT] = newExpiresAt
                    }
                    Log.d(TAG, "Token refreshed successfully before sync")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed auto-refresh in sync manager: ${e.message}")
            }
        }

        return Pair(userId, token)
    }

    private suspend fun tryRefreshDirectly(): String? {
        val prefs = authDataStore.data.firstOrNull() ?: return null
        val refreshToken = prefs[KEY_REFRESH_TOKEN] ?: return null
        return try {
            val response = supabaseApiService.refreshToken(
                apiKey = Constants.SUPABASE_ANON_KEY,
                authorization = "Bearer ${Constants.SUPABASE_ANON_KEY}",
                request = SupabaseRefreshRequest(refreshToken)
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val newToken = body.accessToken ?: ""
                val newRefreshToken = body.refreshToken ?: refreshToken
                val expiresInSeconds = body.expiresIn ?: 3600L
                val newExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
                authDataStore.edit { p ->
                    p[KEY_ACCESS_TOKEN] = newToken
                    p[KEY_REFRESH_TOKEN] = newRefreshToken
                    p[KEY_EXPIRES_AT] = newExpiresAt
                }
                newToken
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sanitizes huge Base64 data URIs by saving them to disk and returning a compact file URI.
     * Prevents SQLite CursorWindow (2MB) SQLiteBlobTooBigException.
     */
    fun sanitizeAndPersistImage(rawUri: String?, prefix: String, id: String): String? {
        if (rawUri.isNullOrEmpty()) return null
        if (rawUri.startsWith("data:image", ignoreCase = true) || (rawUri.length > 2000 && !rawUri.startsWith("http", ignoreCase = true))) {
            return try {
                val cleanId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

                // Save raw string to disk so we can preserve it when syncing back to Supabase cloud
                val rawFile = File(coversDir, "${prefix}_${cleanId}.raw")
                rawFile.writeText(rawUri)

                // Decode base64 to jpg file for smooth Coil image rendering
                val base64Data = if (rawUri.contains(",")) rawUri.substringAfter(",") else rawUri
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                val imgFile = File(coversDir, "${prefix}_${cleanId}.jpg")
                imgFile.writeBytes(bytes)

                imgFile.toURI().toString()
            } catch (e: Exception) {
                Log.w(TAG, "Error saving base64 image for $prefix $id: ${e.message}")
                null
            }
        }
        return rawUri
    }

    /**
     * Restores original Base64 URI if this image was saved locally to avoid SQLite limits.
     */
    fun restoreOriginalCover(uri: String?, prefix: String, id: String): String? {
        if (uri == null) return null
        if (uri.startsWith("file:", ignoreCase = true)) {
            val cleanId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val rawFile = File(context.filesDir, "covers/${prefix}_${cleanId}.raw")
            if (rawFile.exists()) {
                return try {
                    rawFile.readText()
                } catch (e: Exception) {
                    uri
                }
            }
        }
        return uri
    }

    /**
     * Downloads user data (favorites and playlists) from Supabase user_sync table into local Room DB.
     */
    suspend fun syncFromCloud(userId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching user_sync row from Supabase for user: $userId")
            var currentToken = accessToken
            var response = supabaseApiService.getUserSync(
                apiKey = Constants.SUPABASE_ANON_KEY,
                authorization = "Bearer $currentToken",
                idFilter = "eq.$userId"
            )

            // If JWT expired, attempt an auto-refresh and retry once
            if (!response.isSuccessful) {
                val errStr = response.errorBody()?.string() ?: ""
                if (response.code() == 401 || errStr.contains("JWT expired") || errStr.contains("PGRST303")) {
                    Log.d(TAG, "JWT expired encountered in syncFromCloud. Renewing token...")
                    val refreshedToken = tryRefreshDirectly()
                    if (refreshedToken != null) {
                        currentToken = refreshedToken
                        response = supabaseApiService.getUserSync(
                            apiKey = Constants.SUPABASE_ANON_KEY,
                            authorization = "Bearer $currentToken",
                            idFilter = "eq.$userId"
                        )
                    }
                }

                if (!response.isSuccessful) {
                    val finalErr = response.errorBody()?.string() ?: "Sync fetch failed (${response.code()})"
                    Log.e(TAG, "Supabase fetch error: $finalErr")
                    return@withContext Result.failure(Exception(finalErr))
                }
            }

            val rows = response.body()
            val userRow = rows?.firstOrNull()

            if (userRow?.data != null) {
                lastKnownCloudUpdatedAt = userRow.updatedAt
                val data = userRow.data
                val favorites = data.favorites ?: emptyList()
                val playlists = data.playlists ?: emptyList()

                // Merge cloud stats into local stats manager
                if (data.accumulatedStats != null) {
                    userStatsManager.mergeFromCloud(data.accumulatedStats)
                }

                // Sync avatar if present
                val cloudAvatar = data.avatarUrl
                    ?: data.pfp
                    ?: data.avatar
                    ?: data.avatarUrlCamel
                    ?: (data.userProfile?.get("avatar_url") as? String)
                    ?: (data.userProfile?.get("pfp") as? String)
                    ?: (data.userProfile?.get("avatar") as? String)
                if (!cloudAvatar.isNullOrEmpty()) {
                    val safeAvatar = sanitizeAndPersistImage(cloudAvatar, "avatar", userId) ?: cloudAvatar
                    authDataStore.edit { p ->
                        p[stringPreferencesKey("user_avatar")] = safeAvatar
                    }
                }

                Log.d(TAG, "Cloud data received: ${favorites.size} favorites, ${playlists.size} playlists")

                // 1. Process and insert Favorites with chunking and image sanitization
                if (favorites.isNotEmpty()) {
                    val trackEntities = favorites.distinctBy { it.id }.map { dto ->
                        val durationMs = ((dto.duration ?: 0.0) * 1000.0).toLong()
                        val artistName = dto.artist ?: dto.artists?.firstOrNull()?.name ?: "Unknown Artist"
                        val artistId = dto.artistId ?: dto.artists?.firstOrNull()?.id
                        val safeArtwork = sanitizeAndPersistImage(dto.thumbnail, "track", dto.id)
                        val safeAudioUrl = if (dto.audioUrl != null && dto.audioUrl.length < 2000) dto.audioUrl else null

                        TrackEntity(
                            id = dto.id,
                            providerId = if (dto.id.startsWith("yt-")) "youtube" else "deezer",
                            providerTrackId = dto.id,
                            title = dto.title ?: "Untitled",
                            artistName = artistName,
                            artistId = artistId,
                            albumTitle = dto.albumName,
                            albumId = dto.albumId,
                            durationMs = durationMs,
                            artworkUrl = safeArtwork,
                            isExplicit = dto.isExplicit ?: false,
                            isLiked = true,
                            isDownloaded = false,
                            localFilePath = null,
                            lastPlayedAt = System.currentTimeMillis()
                        )
                    }
                    trackEntities.chunked(200).forEach { batch ->
                        trackDao.upsertAll(batch)
                    }
                    Log.d(TAG, "Synced ${trackEntities.size} liked tracks from Web account into Room")
                }

                // 2. Process and insert Playlists and their tracks
                if (playlists.isNotEmpty()) {
                    val playlistEntities = playlists.distinctBy { it.id }.map { pDto ->
                        val safeCover = sanitizeAndPersistImage(pDto.coverUrl, "playlist", pDto.id)
                        PlaylistEntity(
                            id = pDto.id,
                            name = pDto.name,
                            coverUrl = safeCover,
                            createdAt = System.currentTimeMillis()
                        )
                    }
                    playlistEntities.chunked(100).forEach { batch ->
                        playlistDao.insertPlaylists(batch)
                    }

                    for (pDto in playlists) {
                        val tracks = pDto.tracks ?: emptyList()
                        val pTrackEntities = tracks.distinctBy { it.id }.map { tDto ->
                            val durationMs = ((tDto.duration ?: 0.0) * 1000.0).toLong()
                            val artistName = tDto.artist ?: tDto.artists?.firstOrNull()?.name ?: "Unknown Artist"
                            val isCurrentlyLiked = trackDao.isTrackLiked(tDto.id) ?: false
                            val safeArtwork = sanitizeAndPersistImage(tDto.thumbnail, "track", tDto.id)
                            val safeAudioUrl = if (tDto.audioUrl != null && tDto.audioUrl.length < 2000) tDto.audioUrl else null

                            TrackEntity(
                                id = tDto.id,
                                providerId = if (tDto.id.startsWith("yt-")) "youtube" else "deezer",
                                providerTrackId = tDto.id,
                                title = tDto.title ?: "Untitled",
                                artistName = artistName,
                                artistId = tDto.artistId ?: tDto.artists?.firstOrNull()?.id,
                                albumTitle = tDto.albumName,
                                albumId = tDto.albumId,
                                durationMs = durationMs,
                                artworkUrl = safeArtwork,
                                isExplicit = tDto.isExplicit ?: false,
                                isLiked = isCurrentlyLiked,
                                isDownloaded = false,
                                localFilePath = null,
                                lastPlayedAt = System.currentTimeMillis()
                            )
                        }
                        if (pTrackEntities.isNotEmpty()) {
                            pTrackEntities.chunked(200).forEach { batch ->
                                trackDao.upsertAll(batch)
                            }
                        }

                        val crossRefs = tracks.distinctBy { it.id }.mapIndexed { index, tDto ->
                            PlaylistTrackEntity(
                                playlistId = pDto.id,
                                trackId = tDto.id,
                                positionOrder = index,
                                addedAt = System.currentTimeMillis()
                            )
                        }
                        if (crossRefs.isNotEmpty()) {
                            crossRefs.chunked(500).forEach { batch ->
                                playlistDao.insertPlaylistTracks(batch)
                            }
                        }
                    }
                    Log.d(TAG, "Synced ${playlists.size} playlists from Web account into Room")
                }

                Result.success(Unit)
            } else {
                Log.d(TAG, "Cloud data is empty for user $userId. Initializing cloud state with local data...")
                syncToCloud(userId, currentToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync from cloud", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads local library state (favorites and playlists) to Supabase user_sync table.
     */
    suspend fun syncToCloud(userId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val likedEntities = trackDao.getLikedTracksList()
            val playlistEntities = playlistDao.getAllPlaylistsList()

            val webFavorites = likedEntities.map { entity ->
                val restoredArtwork = restoreOriginalCover(entity.artworkUrl, "track", entity.id)
                WebTrackDto(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artistName,
                    artistId = entity.artistId,
                    artists = listOf(WebArtistDto(id = entity.artistId, name = entity.artistName)),
                    albumName = entity.albumTitle,
                    albumId = entity.albumId,
                    duration = entity.durationMs / 1000.0,
                    thumbnail = restoredArtwork,
                    audioUrl = entity.localFilePath,
                    isExplicit = entity.isExplicit
                )
            }

            val webPlaylists = playlistEntities.map { pEntity ->
                val pTracks = playlistDao.getPlaylistTracksList(pEntity.id).map { tEntity ->
                    val restoredArtwork = restoreOriginalCover(tEntity.artworkUrl, "track", tEntity.id)
                    WebTrackDto(
                        id = tEntity.id,
                        title = tEntity.title,
                        artist = tEntity.artistName,
                        artistId = tEntity.artistId,
                        artists = listOf(WebArtistDto(id = tEntity.artistId, name = tEntity.artistName)),
                        albumName = tEntity.albumTitle,
                        albumId = tEntity.albumId,
                        duration = tEntity.durationMs / 1000.0,
                        thumbnail = restoredArtwork,
                        audioUrl = tEntity.localFilePath,
                        isExplicit = tEntity.isExplicit
                    )
                }
                val restoredCover = restoreOriginalCover(pEntity.coverUrl, "playlist", pEntity.id)
                WebPlaylistDto(
                    id = pEntity.id,
                    name = pEntity.name,
                    coverUrl = restoredCover,
                    isPublic = false,
                    tracks = pTracks
                )
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val nowIso = dateFormat.format(Date())

            val currentAvatar = authDataStore.data.firstOrNull()?.get(stringPreferencesKey("user_avatar"))
            val restoredAvatar = restoreOriginalCover(currentAvatar, "avatar", userId) ?: currentAvatar

            val syncData = WebSyncData(
                favorites = webFavorites,
                playlists = webPlaylists,
                accumulatedStats = userStatsManager.toCloudJson(),
                avatarUrl = restoredAvatar,
                pfp = restoredAvatar
            )

            val payload = SupabaseSyncRow(
                id = userId,
                updatedAt = nowIso,
                data = syncData
            )

            var currentToken = accessToken
            var response = supabaseApiService.upsertUserSync(
                apiKey = Constants.SUPABASE_ANON_KEY,
                authorization = "Bearer $currentToken",
                payload = payload
            )

            if (!response.isSuccessful) {
                val errStr = response.errorBody()?.string() ?: ""
                if (response.code() == 401 || errStr.contains("JWT expired") || errStr.contains("PGRST303")) {
                    Log.d(TAG, "JWT expired encountered in syncToCloud. Renewing token...")
                    val refreshedToken = tryRefreshDirectly()
                    if (refreshedToken != null) {
                        currentToken = refreshedToken
                        response = supabaseApiService.upsertUserSync(
                            apiKey = Constants.SUPABASE_ANON_KEY,
                            authorization = "Bearer $currentToken",
                            payload = payload
                        )
                    }
                }

                if (!response.isSuccessful) {
                    val finalErr = response.errorBody()?.string() ?: "Upsert failed (${response.code()})"
                    Log.e(TAG, "Supabase upsert error: $finalErr")
                    return@withContext Result.failure(Exception(finalErr))
                }
            }

            lastKnownCloudUpdatedAt = nowIso
            Log.d(TAG, "Successfully synced local library (${webFavorites.size} liked, ${webPlaylists.size} playlists) to Supabase cloud!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push sync to cloud", e)
            Result.failure(e)
        }
    }
}
