package com.ibramusic.mobile.domain.usecase

import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMusicUseCase @Inject constructor(
    private val trackRepository: TrackRepository
) {
    suspend fun execute(query: String): List<Track> {
        return trackRepository.searchTracks(query)
    }

    suspend fun searchAlbums(query: String): List<Album> {
        return trackRepository.searchAlbums(query)
    }

    suspend fun searchArtists(query: String): List<Artist> {
        return trackRepository.searchArtists(query)
    }
}

class GetHomeRecommendationsUseCase @Inject constructor(
    private val trackRepository: TrackRepository
) {
    suspend operator fun invoke(): List<Track> {
        return trackRepository.getHomeRecommendations()
    }
}

class GetTrendingTracksUseCase @Inject constructor(
    private val trackRepository: TrackRepository
) {
    suspend operator fun invoke(): List<Track> {
        return trackRepository.getTrending()
    }
}

class LikedTracksUseCase @Inject constructor(
    private val trackRepository: TrackRepository
) {
    fun getLikedTracks(): Flow<List<Track>> {
        return trackRepository.getLikedTracks()
    }

    suspend fun toggleLike(track: Track): Boolean {
        return trackRepository.toggleLike(track)
    }

    suspend fun isLiked(trackId: String): Boolean {
        return trackRepository.isLiked(trackId)
    }
}

