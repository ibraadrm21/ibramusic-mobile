package com.ibramusic.mobile.data.repository

import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.TrackEntity
import com.ibramusic.mobile.data.provider.DeezerProvider
import com.ibramusic.mobile.data.provider.LocalAudioProvider
import com.ibramusic.mobile.data.sync.SupabaseSyncManager
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.TrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepositoryImpl @Inject constructor(
    private val deezerProvider: DeezerProvider,
    private val localAudioProvider: LocalAudioProvider,
    private val trackDao: TrackDao,
    private val supabaseSyncManager: SupabaseSyncManager
) : TrackRepository {

    override suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        val remote = try {
            deezerProvider.search(query)
        } catch (e: Exception) {
            emptyList()
        }
        val local = try {
            localAudioProvider.search(query)
        } catch (e: Exception) {
            emptyList()
        }
        // Local files prioritized, followed by remote search
        local + remote
    }

    override suspend fun searchAlbums(query: String): List<Album> = withContext(Dispatchers.IO) {
        val directAlbums = deezerProvider.searchAlbums(query)
        if (directAlbums.isNotEmpty()) {
            directAlbums
        } else {
            val tracks = searchTracks(query)
            tracks.mapNotNull { it.album }.distinctBy { it.title }
        }
    }

    override suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        val directArtists = deezerProvider.searchArtists(query)
        if (directArtists.isNotEmpty()) {
            directArtists
        } else {
            val tracks = searchTracks(query)
            tracks.flatMap { it.artists }.distinctBy { it.name }
        }
    }

    override suspend fun getHomeRecommendations(): List<Track> = withContext(Dispatchers.IO) {
        try {
            val liked = trackDao.getLikedTracksList()
            if (liked.isNotEmpty()) {
                val favoriteArtists = liked.map { it.artistName }.filter { it.isNotBlank() }.distinct()
                val randomArtist = favoriteArtists.shuffled().firstOrNull()
                if (randomArtist != null) {
                    val artistTracks = deezerProvider.search("artist:\"$randomArtist\"")
                    if (artistTracks.isNotEmpty()) {
                        val chart = deezerProvider.getChart(limit = 15)
                        return@withContext (artistTracks.take(15) + chart).distinctBy { it.id }.shuffled()
                    }
                }
            }
            deezerProvider.getChart(limit = 30).shuffled()
        } catch (e: Exception) {
            localAudioProvider.search("")
        }
    }

    companion object {
        private val GLOBAL_TRENDING_ARTISTS = listOf(
            "The Weeknd",
            "Bad Bunny",
            "Kendrick Lamar",
            "Billie Eilish",
            "Travis Scott",
            "Bruno Mars",
            "Sabrina Carpenter",
            "Taylor Swift",
            "Drake",
            "Feid",
            "Karol G",
            "Dua Lipa",
            "Post Malone",
            "Olivia Rodrigo"
        )
    }

    override suspend fun getTrending(): List<Track> = withContext(Dispatchers.IO) {
        try {
            // Official Global Editorial Top Hits (Real worldwide chart: Bruno Mars, Lady Gaga, Sabrina Carpenter, Billie Eilish, etc.)
            val chartTracks = deezerProvider.getChart(limit = 40)
            if (chartTracks.isNotEmpty()) {
                chartTracks
            } else {
                deezerProvider.search("top global hits")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAlbumTracks(albumId: String): List<Track> = withContext(Dispatchers.IO) {
        // Search by album ID or name
        emptyList()
    }

    override suspend fun getArtistTracks(artistId: String): List<Track> = withContext(Dispatchers.IO) {
        emptyList()
    }

    override fun getLikedTracks(): Flow<List<Track>> {
        return trackDao.getLikedTracks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun toggleLike(track: Track): Boolean = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(track.id)
        val newLikedState = !(existing?.isLiked ?: false)
        if (existing != null) {
            trackDao.insertOrUpdate(existing.copy(isLiked = newLikedState))
        } else {
            trackDao.upsertTrack(TrackEntity.fromDomain(track, isLiked = newLikedState))
        }
        supabaseSyncManager.scheduleSyncToCloud()
        newLikedState
    }

    override suspend fun isLiked(trackId: String): Boolean = withContext(Dispatchers.IO) {
        trackDao.isTrackLiked(trackId) ?: false
    }
}

