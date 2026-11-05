package com.ibramusic.mobile.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.TrackEntity
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.playback.resolver.YouTubeAudioResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val youTubeAudioResolver: YouTubeAudioResolver,
    okHttpClient: OkHttpClient
) : DownloadRepository {

    companion object {
        private const val TAG = "DownloadRepo"
        private const val CHANNEL_ID = "downloads_channel"
        private const val ACTIVE_NOTIFICATION_ID = 9001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val downloadHttpClient = okHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadQueue = Channel<Track>(Channel.UNLIMITED)
    private val _progressMap = MutableStateFlow<Map<String, Int>>(emptyMap())

    override val activeDownloads: StateFlow<Map<String, Int>> = _progressMap.asStateFlow()

    init {
        createNotificationChannel()

        // Sequential atomic worker: processes one download at a time to prevent rate-limiting or network saturation
        downloadScope.launch {
            for (track in downloadQueue) {
                try {
                    processDownload(track)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during download of ${track.title}", e)
                    notifyDownloadError(track)
                    _progressMap.value = _progressMap.value + (track.id to -1)
                    delay(3000)
                    _progressMap.value = _progressMap.value - track.id
                }
            }
        }

        // Scan downloads directory on startup to ensure all existing downloaded audio files are recorded in Room
        downloadScope.launch {
            try {
                val downloadsDir = File(context.filesDir, "downloads")
                if (downloadsDir.exists() && downloadsDir.isDirectory) {
                    val m4aFiles = downloadsDir.listFiles { _, name -> name.endsWith(".m4a") } ?: emptyArray()
                    for (file in m4aFiles) {
                        if (file.length() > 0) {
                            val trackId = file.name.removeSuffix(".m4a")
                            val existing = trackDao.getTrackById(trackId)
                            val metaFile = File(downloadsDir, "$trackId.json")
                            val thumbFile = File(downloadsDir, "$trackId.jpg")
                            val localArtworkUri = if (thumbFile.exists()) Uri.fromFile(thumbFile).toString() else null

                            if (existing != null) {
                                val needsUpdate = !existing.isDownloaded || 
                                    existing.localFilePath != file.absolutePath ||
                                    (localArtworkUri != null && (existing.artworkUrl == null || existing.artworkUrl.startsWith("file:/data/")))
                                if (needsUpdate) {
                                    val updatedArt = localArtworkUri ?: existing.artworkUrl
                                    trackDao.insertOrUpdate(existing.copy(isDownloaded = true, localFilePath = file.absolutePath, artworkUrl = updatedArt))
                                    Log.d(TAG, "Restored offline track in Room: $trackId -> ${file.absolutePath}")
                                }
                            } else if (metaFile.exists()) {
                                try {
                                    val metaJson = JSONObject(metaFile.readText())
                                    val art = localArtworkUri ?: metaJson.optString("artworkUrl").takeIf { it.isNotBlank() }
                                    val restored = TrackEntity(
                                        id = trackId,
                                        providerId = metaJson.optString("providerId", "download"),
                                        providerTrackId = metaJson.optString("providerTrackId", trackId),
                                        title = metaJson.optString("title", "Pista descargada"),
                                        artistName = metaJson.optString("artistName", "Artista desconocido"),
                                        artistId = null,
                                        albumTitle = metaJson.optString("albumTitle").takeIf { it.isNotBlank() },
                                        albumId = null,
                                        durationMs = metaJson.optLong("durationMs", 0L),
                                        artworkUrl = art,
                                        isExplicit = metaJson.optBoolean("isExplicit", false),
                                        isLiked = false,
                                        isDownloaded = true,
                                        localFilePath = file.absolutePath,
                                        lastPlayedAt = System.currentTimeMillis()
                                    )
                                    trackDao.insertOrUpdate(restored)
                                    Log.d(TAG, "Restored missing download from JSON metadata: $trackId -> ${restored.title}")
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed parsing metadata for $trackId: ${e.message}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring downloaded files on startup: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Descargas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso y estado de descargas offline"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notifyDownloadProgress(track: Track, progress: Int) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Descargando ${track.title}")
                .setContentText("${track.artistName} • $progress%")
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
            notificationManager.notify(ACTIVE_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Notification progress error: ${e.message}")
        }
    }

    private fun notifyDownloadComplete(track: Track) {
        try {
            notificationManager.cancel(ACTIVE_NOTIFICATION_ID)
            val notifId = (track.id.hashCode() and 0x7FFFFFFF) % 10000 + 1000
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Descarga completada")
                .setContentText("${track.title} - ${track.artistName}")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
            notificationManager.notify(notifId, builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Notification complete error: ${e.message}")
        }
    }

    private fun notifyDownloadError(track: Track) {
        try {
            notificationManager.cancel(ACTIVE_NOTIFICATION_ID)
            val notifId = (track.id.hashCode() and 0x7FFFFFFF) % 10000 + 1000
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Error en la descarga")
                .setContentText("No se pudo descargar \"${track.title}\"")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
            notificationManager.notify(notifId, builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Notification error: ${e.message}")
        }
    }

    override val downloadedTracks: Flow<List<Track>> = trackDao.getDownloadedTracks().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun downloadTrack(track: Track) {
        // Check if already downloaded
        if (isDownloaded(track.id)) {
            Log.d(TAG, "Track '${track.title}' is already downloaded offline")
            return
        }
        _progressMap.value = _progressMap.value + (track.id to 1)
        notifyDownloadProgress(track, 1)
        downloadQueue.send(track)
    }

    private suspend fun processDownload(track: Track) = withContext(Dispatchers.IO) {
        val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
        val tmpAudioFile = File(downloadsDir, "${track.id}.tmp")
        val finalAudioFile = File(downloadsDir, "${track.id}.m4a")
        val finalThumbFile = File(downloadsDir, "${track.id}.jpg")

        if (finalAudioFile.exists() && finalAudioFile.length() > 0) {
            val entity = TrackEntity.fromDomain(
                track = track,
                isDownloaded = true,
                localFilePath = finalAudioFile.absolutePath
            )
            trackDao.insertOrUpdate(entity)
            _progressMap.value = _progressMap.value + (track.id to 100)
            notifyDownloadComplete(track)
            return@withContext
        }

        try {
            Log.d(TAG, "Starting offline download for: ${track.artistName} - ${track.title}")
            _progressMap.value = _progressMap.value + (track.id to 5)
            notifyDownloadProgress(track, 5)

            // Step 1: Resolve high-bitrate audio stream URL
            val resolvedSource = youTubeAudioResolver.resolve(track)
            val streamUrl = resolvedSource?.uri?.toString() ?: track.streamUrl

            if (streamUrl.isNullOrEmpty()) {
                Log.e(TAG, "Could not resolve audio stream URL for ${track.title}")
                notifyDownloadError(track)
                _progressMap.value = _progressMap.value + (track.id to -1)
                delay(3000)
                _progressMap.value = _progressMap.value - track.id
                return@withContext
            }

            // Step 2: Stream audio bytes using byte-range request
            _progressMap.value = _progressMap.value + (track.id to 15)
            notifyDownloadProgress(track, 15)
            val request = Request.Builder()
                .url(streamUrl)
                .header("Range", "bytes=0-")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            downloadHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    Log.e(TAG, "HTTP error downloading ${track.title}: code ${response.code}")
                    notifyDownloadError(track)
                    _progressMap.value = _progressMap.value + (track.id to -1)
                    delay(3000)
                    _progressMap.value = _progressMap.value - track.id
                    return@withContext
                }

                val body = response.body ?: run {
                    notifyDownloadError(track)
                    _progressMap.value = _progressMap.value + (track.id to -1)
                    delay(3000)
                    _progressMap.value = _progressMap.value - track.id
                    return@withContext
                }

                val contentLength = body.contentLength()
                var bytesRead = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tmpAudioFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var lastProgressReport = 15
                        var lastNotificationTime = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (contentLength > 0) {
                                val progress = (15 + (bytesRead * 75 / contentLength).toInt()).coerceIn(15, 90)
                                if (progress >= lastProgressReport + 5) {
                                    lastProgressReport = progress
                                    _progressMap.value = _progressMap.value + (track.id to progress)
                                    val now = System.currentTimeMillis()
                                    if (now - lastNotificationTime >= 1200L) {
                                        lastNotificationTime = now
                                        notifyDownloadProgress(track, progress)
                                    }
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (!tmpAudioFile.exists() || tmpAudioFile.length() == 0L) {
                Log.e(TAG, "Downloaded file is empty for ${track.title}")
                notifyDownloadError(track)
                _progressMap.value = _progressMap.value + (track.id to -1)
                delay(3000)
                _progressMap.value = _progressMap.value - track.id
                return@withContext
            }

            if (finalAudioFile.exists()) {
                finalAudioFile.delete()
            }
            tmpAudioFile.renameTo(finalAudioFile)
            _progressMap.value = _progressMap.value + (track.id to 92)
            notifyDownloadProgress(track, 92)

            // Step 3: Download thumbnail for offline artwork display
            if (!track.artworkUrl.isNullOrEmpty() && !track.artworkUrl.startsWith("file://")) {
                try {
                    val imgReq = Request.Builder().url(track.artworkUrl).build()
                    downloadHttpClient.newCall(imgReq).execute().use { imgResp ->
                        if (imgResp.isSuccessful) {
                            imgResp.body?.byteStream()?.use { imgIn ->
                                FileOutputStream(finalThumbFile).use { imgOut ->
                                    imgIn.copyTo(imgOut)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed downloading artwork for ${track.id}: ${e.message}")
                }
            }

            // Step 4: Register in Room DB
            val localArtwork = if (finalThumbFile.exists()) Uri.fromFile(finalThumbFile).toString() else track.artworkUrl
            val entity = TrackEntity.fromDomain(
                track = track.copy(
                    artworkUrl = localArtwork,
                    availableOffline = true
                ),
                isDownloaded = true,
                localFilePath = finalAudioFile.absolutePath
            )
            trackDao.insertOrUpdate(entity)

            // Persist standalone JSON metadata to guarantee download is never lost on sync or cache clear
            try {
                val metaObj = JSONObject().apply {
                    put("id", track.id)
                    put("providerId", track.providerId)
                    put("providerTrackId", track.providerTrackId)
                    put("title", track.title)
                    put("artistName", track.artistName)
                    put("albumTitle", track.album?.title ?: "")
                    put("durationMs", track.durationMs)
                    put("artworkUrl", localArtwork ?: "")
                    put("isExplicit", track.isExplicit)
                }
                File(downloadsDir, "${track.id}.json").writeText(metaObj.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed writing metadata json for ${track.id}: ${e.message}")
            }
            _progressMap.value = _progressMap.value + (track.id to 100)
            notifyDownloadComplete(track)
            Log.d(TAG, "Completed offline download: ${track.title} [${finalAudioFile.length()} bytes]")

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading ${track.title}", e)
            if (tmpAudioFile.exists()) tmpAudioFile.delete()
            notifyDownloadError(track)
            _progressMap.value = _progressMap.value + (track.id to -1)
            delay(3000)
            _progressMap.value = _progressMap.value - track.id
        }
    }

    override suspend fun removeDownload(trackId: String) = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(trackId)
        if (existing != null) {
            existing.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            val downloadsDir = File(context.filesDir, "downloads")
            val thumbFile = File(downloadsDir, "$trackId.jpg")
            if (thumbFile.exists()) thumbFile.delete()
            val metaFile = File(downloadsDir, "$trackId.json")
            if (metaFile.exists()) metaFile.delete()

            trackDao.insertOrUpdate(existing.copy(isDownloaded = false, localFilePath = null))
            _progressMap.value = _progressMap.value - trackId
            Log.d(TAG, "Removed download for track $trackId")
        }
    }

    override suspend fun isDownloaded(trackId: String): Boolean = withContext(Dispatchers.IO) {
        val dbTrack = trackDao.getTrackById(trackId)
        if (dbTrack != null && dbTrack.isDownloaded && !dbTrack.localFilePath.isNullOrEmpty()) {
            val f = File(dbTrack.localFilePath)
            if (f.exists() && f.length() > 0) return@withContext true
        }
        val fallbackFile = File(context.filesDir, "downloads/$trackId.m4a")
        if (fallbackFile.exists() && fallbackFile.length() > 0) {
            if (dbTrack != null) {
                trackDao.insertOrUpdate(dbTrack.copy(isDownloaded = true, localFilePath = fallbackFile.absolutePath))
            }
            return@withContext true
        }
        false
    }

    override suspend fun getDownloadProgress(trackId: String): Flow<Int> = _progressMap.map { map ->
        map[trackId] ?: if (isDownloaded(trackId)) 100 else 0
    }.distinctUntilChanged()
}
