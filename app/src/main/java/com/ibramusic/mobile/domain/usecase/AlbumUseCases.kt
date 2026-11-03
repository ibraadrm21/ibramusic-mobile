package com.ibramusic.mobile.domain.usecase

import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.AlbumRepository
import com.ibramusic.mobile.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetSavedAlbumsUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    operator fun invoke(): Flow<List<Album>> = albumRepository.getSavedAlbums()
}

class ToggleSaveAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(album: Album): Boolean {
        val isCurrentlySaved = albumRepository.isAlbumSaved(album.id, album.title).first()
        if (isCurrentlySaved) {
            albumRepository.removeAlbum(album.id, album.title)
            return false
        } else {
            albumRepository.saveAlbum(album)
            return true
        }
    }
}

class IsAlbumSavedUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    operator fun invoke(id: String, title: String): Flow<Boolean> = albumRepository.isAlbumSaved(id, title)
}

class GetAlbumTracksUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(albumId: String, albumTitle: String): List<Track> {
        return albumRepository.getAlbumTracks(albumId, albumTitle)
    }
}

class SaveAlbumAsPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(album: Album, tracks: List<Track>): String {
        val playlistId = playlistRepository.createPlaylist(
            name = album.title,
            description = "Álbum de ${album.artistName}",
            coverUrl = album.artworkUrl
        )
        tracks.forEach { track ->
            playlistRepository.addTrackToPlaylist(playlistId, track)
        }
        return playlistId
    }
}

class AddAlbumToPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, tracks: List<Track>) {
        tracks.forEach { track ->
            playlistRepository.addTrackToPlaylist(playlistId, track)
        }
    }
}

