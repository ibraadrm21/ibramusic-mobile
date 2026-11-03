package com.ibramusic.mobile.data.repository

import com.ibramusic.mobile.data.local.dao.PlaylistDao
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.PlaylistEntity
import com.ibramusic.mobile.data.local.entity.PlaylistTrackEntity
import com.ibramusic.mobile.data.local.entity.TrackEntity
import com.ibramusic.mobile.data.sync.SupabaseSyncManager
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao,
    private val supabaseSyncManager: SupabaseSyncManager
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { items ->
            items.map { item ->
                Playlist(
                    id = item.id,
                    name = item.name,
                    description = item.description,
                    coverUrl = item.coverUrl,
                    trackCount = item.trackCount,
                    downloadedTrackCount = item.downloadedTrackCount,
                    createdAt = item.createdAt
                )
            }
        }
    }

    override fun getPlaylistTracks(playlistId: String): Flow<List<Track>> {
        return playlistDao.getPlaylistTracks(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createPlaylist(name: String, description: String?, coverUrl: String?): String {
        val id = UUID.randomUUID().toString()
        val entity = PlaylistEntity(
            id = id,
            name = name,
            description = description,
            coverUrl = coverUrl,
            createdAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(entity)
        supabaseSyncManager.scheduleSyncToCloud()
        return id
    }

    override suspend fun updatePlaylist(playlistId: String, name: String, coverUrl: String?) {
        playlistDao.updatePlaylistDetails(playlistId, name, coverUrl)
        supabaseSyncManager.scheduleSyncToCloud()
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylistTracks(playlistId)
        playlistDao.deletePlaylist(playlistId)
        supabaseSyncManager.scheduleSyncToCloud()
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        // Ensure track is stored in tracks table without wiping offline status
        val trackEntity = TrackEntity.fromDomain(track)
        trackDao.upsertTrack(trackEntity)

        val playlistTrack = PlaylistTrackEntity(
            playlistId = playlistId,
            trackId = track.id,
            positionOrder = System.currentTimeMillis().toInt(),
            addedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylistTrack(playlistTrack)
        supabaseSyncManager.scheduleSyncToCloud()
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        supabaseSyncManager.scheduleSyncToCloud()
    }
}
