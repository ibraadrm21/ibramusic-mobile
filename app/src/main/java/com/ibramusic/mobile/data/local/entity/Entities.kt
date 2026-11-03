package com.ibramusic.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val providerTrackId: String,
    val title: String,
    val artistName: String,
    val artistId: String?,
    val albumTitle: String?,
    val albumId: String?,
    val durationMs: Long,
    val artworkUrl: String?,
    val isExplicit: Boolean,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val lastPlayedAt: Long? = null
) {
    fun toDomain(): Track {
        return Track(
            id = id,
            providerId = providerId,
            providerTrackId = providerTrackId,
            title = title,
            artists = listOf(Artist(id = artistId ?: "", name = artistName)),
            album = if (albumTitle != null) Album(id = albumId ?: "", title = albumTitle, artistName = artistName) else null,
            durationMs = durationMs,
            artworkUrl = artworkUrl,
            isExplicit = isExplicit,
            availableOffline = isDownloaded,
            streamUrl = localFilePath,
            isLiked = isLiked
        )
    }

    companion object {
        fun fromDomain(
            track: Track,
            isLiked: Boolean = track.isLiked,
            isDownloaded: Boolean = track.availableOffline,
            localFilePath: String? = track.streamUrl
        ): TrackEntity {
            return TrackEntity(
                id = track.id,
                providerId = track.providerId,
                providerTrackId = track.providerTrackId,
                title = track.title,
                artistName = track.artistName,
                artistId = track.artists.firstOrNull()?.id,
                albumTitle = track.album?.title,
                albumId = track.album?.id,
                durationMs = track.durationMs,
                artworkUrl = track.artworkUrl,
                isExplicit = track.isExplicit,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                localFilePath = localFilePath,
                lastPlayedAt = System.currentTimeMillis()
            )
        }
    }
}

@Entity(tableName = "queue_items")
data class QueueEntity(
    @PrimaryKey val queueEntryId: String,
    val trackId: String,
    val queueType: String, // "USER" or "CONTEXT"
    val positionOrder: Int,
    val addedAt: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val positionOrder: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "followed_artists")
data class FollowedArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artworkUrl: String? = null,
    val followedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_albums")
data class SavedAlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val artworkUrl: String? = null,
    val releaseYear: Int? = null,
    val totalTracks: Int? = null,
    val savedAt: Long = System.currentTimeMillis()
)
