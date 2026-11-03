package com.ibramusic.mobile.domain.usecase

import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<Playlist>> = playlistRepository.getPlaylists()
}

class GetPlaylistTracksUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(playlistId: String): Flow<List<Track>> = playlistRepository.getPlaylistTracks(playlistId)
}

class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(name: String, description: String? = null, coverUrl: String? = null): String {
        return playlistRepository.createPlaylist(name, description, coverUrl)
    }
}

class UpdatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, name: String, coverUrl: String?) {
        playlistRepository.updatePlaylist(playlistId, name, coverUrl)
    }
}

class DeletePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String) {
        playlistRepository.deletePlaylist(playlistId)
    }
}

class AddTrackToPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, track: Track) {
        playlistRepository.addTrackToPlaylist(playlistId, track)
    }
}

class RemoveTrackFromPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, trackId: String) {
        playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
    }
}

