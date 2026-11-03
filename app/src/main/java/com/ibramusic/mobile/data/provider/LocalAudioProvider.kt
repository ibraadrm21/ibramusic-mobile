package com.ibramusic.mobile.data.provider

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.PlaybackSource
import com.ibramusic.mobile.domain.model.ProviderCapabilities
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAudioProvider(
    private val context: Context
) : MusicProvider {
    override val providerId: String = "local"
    override val displayName: String = "Local Audio"

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = if (query.isNotBlank()) {
            "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?"
        } else null

        val selectionArgs = if (query.isNotBlank()) {
            arrayOf("%$query%", "%$query%")
        } else null

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn)

                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    tracks.add(
                        Track(
                            id = "loc_$id",
                            providerId = providerId,
                            providerTrackId = id.toString(),
                            title = title,
                            artists = listOf(Artist(id = "loc_art_$artist", name = artist)),
                            album = Album(id = "loc_alb_$albumId", title = album, artistName = artist, artworkUrl = artworkUri),
                            durationMs = duration,
                            artworkUrl = artworkUri,
                            isExplicit = false,
                            availableOffline = true,
                            streamUrl = contentUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Handle permissions or query error
        }
        tracks
    }

    override suspend fun getTrack(id: String): Track? = withContext(Dispatchers.IO) {
        val cleanId = id.removePrefix("loc_").toLongOrNull() ?: return@withContext null
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            cleanId
        )
        // Return track with resolved URI
        Track(
            id = id,
            providerId = providerId,
            providerTrackId = cleanId.toString(),
            title = "Local Track",
            artists = listOf(Artist(id = "local", name = "Local Artist")),
            durationMs = 0L,
            availableOffline = true,
            streamUrl = contentUri.toString()
        )
    }

    override suspend fun resolvePlayback(track: Track): PlaybackSource? {
        val streamUrl = track.streamUrl ?: return null
        return PlaybackSource(
            uri = Uri.parse(streamUrl),
            mimeType = "audio/*",
            providerId = providerId,
            isSeekable = true
        )
    }

    override suspend fun getCapabilities(): ProviderCapabilities {
        return ProviderCapabilities(
            search = true,
            streaming = true,
            downloads = false,
            playlists = true,
            favorites = true,
            offlinePlayback = true
        )
    }
}

