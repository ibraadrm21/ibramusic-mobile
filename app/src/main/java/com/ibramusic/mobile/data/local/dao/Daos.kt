package com.ibramusic.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ibramusic.mobile.data.local.entity.FollowedArtistEntity
import com.ibramusic.mobile.data.local.entity.PlaylistEntity
import com.ibramusic.mobile.data.local.entity.PlaylistTrackEntity
import com.ibramusic.mobile.data.local.entity.QueueEntity
import com.ibramusic.mobile.data.local.entity.SavedAlbumEntity
import com.ibramusic.mobile.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE isLiked = 1 ORDER BY rowid ASC")
    fun getLikedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isLiked = 1 ORDER BY rowid ASC")
    suspend fun getLikedTracksList(): List<TrackEntity>

    @Query("SELECT isLiked FROM tracks WHERE id = :trackId")
    suspend fun isTrackLiked(trackId: String): Boolean?

    @Query("UPDATE tracks SET isLiked = :isLiked WHERE id = :trackId")
    suspend fun setLiked(trackId: String, isLiked: Boolean)

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1 ORDER BY COALESCE(lastPlayedAt, 0) DESC, rowid DESC")
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY lastPlayedAt DESC LIMIT 30")
    fun getListeningHistory(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET lastPlayedAt = :timestamp WHERE id = :trackId")
    suspend fun updateLastPlayedAt(trackId: String, timestamp: Long): Int

    @Transaction
    suspend fun upsertAll(tracks: List<TrackEntity>) {
        tracks.forEach { track ->
            upsertTrack(track)
        }
    }

    @Transaction
    suspend fun upsertTrack(track: TrackEntity) {
        val existing = getTrackById(track.id)
        if (existing != null) {
            val merged = existing.copy(
                title = track.title.ifEmpty { existing.title },
                artistName = track.artistName.ifEmpty { existing.artistName },
                artistId = track.artistId ?: existing.artistId,
                albumTitle = track.albumTitle ?: existing.albumTitle,
                albumId = track.albumId ?: existing.albumId,
                durationMs = if (track.durationMs > 0) track.durationMs else existing.durationMs,
                artworkUrl = track.artworkUrl ?: existing.artworkUrl,
                isExplicit = track.isExplicit || existing.isExplicit,
                isLiked = if (track.isLiked) true else existing.isLiked,
                isDownloaded = if (track.isDownloaded) true else existing.isDownloaded,
                localFilePath = track.localFilePath ?: existing.localFilePath,
                lastPlayedAt = track.lastPlayedAt ?: existing.lastPlayedAt
            )
            insertOrUpdate(merged)
        } else {
            insertOrUpdate(track)
        }
    }

    @Transaction
    suspend fun recordTrackPlayed(track: TrackEntity) {
        val existing = getTrackById(track.id)
        if (existing != null) {
            val merged = existing.copy(
                lastPlayedAt = track.lastPlayedAt ?: System.currentTimeMillis()
            )
            insertOrUpdate(merged)
        } else {
            insertOrUpdate(track)
        }
    }
}

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueEntity>)

    @Query("SELECT * FROM queue_items ORDER BY positionOrder ASC")
    suspend fun getAllQueueItems(): List<QueueEntity>

    @Query("DELETE FROM queue_items WHERE queueType = :type")
    suspend fun clearQueueByType(type: String)

    @Query("DELETE FROM queue_items")
    suspend fun clearAll()
}

data class PlaylistWithTrackCount(
    val id: String,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0,
    val downloadedTrackCount: Int = 0
)

@Dao
interface PlaylistDao {
    @Query("""
        SELECT p.id, p.name, p.description,
               COALESCE(
                   NULLIF(p.coverUrl, ''),
                   (
                       SELECT t.artworkUrl FROM tracks t
                       INNER JOIN playlist_tracks pt2 ON t.id = pt2.trackId
                       WHERE pt2.playlistId = p.id
                       ORDER BY pt2.positionOrder ASC
                       LIMIT 1
                   )
               ) AS coverUrl,
               p.createdAt,
               COUNT(pt.trackId) AS trackCount,
               COALESCE(SUM(CASE WHEN t.isDownloaded = 1 THEN 1 ELSE 0 END), 0) AS downloadedTrackCount
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId
        LEFT JOIN tracks t ON pt.trackId = t.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    fun getAllPlaylists(): Flow<List<PlaylistWithTrackCount>>

    @Query("""
        SELECT p.id, p.name, p.description,
               COALESCE(
                   NULLIF(p.coverUrl, ''),
                   (
                       SELECT t.artworkUrl FROM tracks t
                       INNER JOIN playlist_tracks pt2 ON t.id = pt2.trackId
                       WHERE pt2.playlistId = p.id
                       ORDER BY pt2.positionOrder ASC
                       LIMIT 1
                   )
               ) AS coverUrl,
               p.createdAt,
               COUNT(pt.trackId) AS trackCount,
               COALESCE(SUM(CASE WHEN t.isDownloaded = 1 THEN 1 ELSE 0 END), 0) AS downloadedTrackCount
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId
        LEFT JOIN tracks t ON pt.trackId = t.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    suspend fun getAllPlaylistsList(): List<PlaylistWithTrackCount>

    @Query("UPDATE playlists SET name = :name, coverUrl = :coverUrl WHERE id = :playlistId")
    suspend fun updatePlaylistDetails(playlistId: String, name: String, coverUrl: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(playlistTracks: List<PlaylistTrackEntity>)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.positionOrder ASC
    """)
    fun getPlaylistTracks(playlistId: String): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.positionOrder ASC
    """)
    suspend fun getPlaylistTracksList(playlistId: String): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCount(playlistId: String): Flow<Int>
}

@Dao
interface ArtistDao {
    @Query("SELECT * FROM followed_artists ORDER BY followedAt DESC")
    fun getFollowedArtists(): Flow<List<FollowedArtistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_artists WHERE id = :id OR name = :name)")
    fun isArtistFollowed(id: String, name: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followArtist(artist: FollowedArtistEntity)

    @Query("DELETE FROM followed_artists WHERE id = :id OR name = :name")
    suspend fun unfollowArtist(id: String, name: String)
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM saved_albums ORDER BY savedAt DESC")
    fun getSavedAlbums(): Flow<List<SavedAlbumEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_albums WHERE id = :id OR title = :title)")
    fun isAlbumSaved(id: String, title: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAlbum(album: SavedAlbumEntity)

    @Query("DELETE FROM saved_albums WHERE id = :id OR title = :title")
    suspend fun removeAlbum(id: String, title: String)
}
