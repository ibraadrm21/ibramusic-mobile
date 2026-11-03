package com.ibramusic.mobile.data.provider

import android.net.Uri
import com.ibramusic.mobile.core.network.DeezerApiService
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.PlaybackSource
import com.ibramusic.mobile.domain.model.ProviderCapabilities
import com.ibramusic.mobile.domain.model.Track

class DeezerProvider(
    private val apiService: DeezerApiService
) : MusicProvider {
    override val providerId: String = "deezer"
    override val displayName: String = "Deezer"

    override suspend fun search(query: String): List<Track> {
        val response = apiService.searchTracks(query)
        val items = response.data ?: emptyList()
        return items.map { dto -> mapDtoToTrack(dto) }
    }

    suspend fun getChart(limit: Int = 30): List<Track> {
        val response = apiService.getChartTracks(limit)
        val items = response.data ?: emptyList()
        return items.map { dto -> mapDtoToTrack(dto) }
    }

    private fun mapDtoToTrack(dto: com.ibramusic.mobile.data.remote.dto.DeezerTrackDto): Track {
        val artwork = dto.album?.coverXl ?: dto.album?.coverMedium ?: dto.artist?.pictureXl
        return Track(
            id = "dz_${dto.id}",
            providerId = providerId,
            providerTrackId = dto.id.toString(),
            title = dto.titleShort ?: dto.title,
            artists = listOf(
                Artist(
                    id = dto.artist?.id?.toString() ?: "",
                    name = dto.artist?.name ?: "Unknown Artist",
                    artworkUrl = dto.artist?.pictureXl
                )
            ),
            album = if (dto.album != null) {
                Album(
                    id = dto.album.id.toString(),
                    title = dto.album.title,
                    artistName = dto.artist?.name ?: "",
                    artworkUrl = artwork
                )
            } else null,
            durationMs = dto.duration * 1000L,
            artworkUrl = artwork,
            isExplicit = dto.explicitLyrics ?: false,
            streamUrl = dto.preview
        )
    }

    override suspend fun getTrack(id: String): Track? {
        val cleanId = id.removePrefix("dz_")
        val dto = apiService.getTrack(cleanId)
        val artwork = dto.album?.coverXl ?: dto.album?.coverMedium
        return Track(
            id = "dz_${dto.id}",
            providerId = providerId,
            providerTrackId = dto.id.toString(),
            title = dto.titleShort ?: dto.title,
            artists = listOf(
                Artist(
                    id = dto.artist?.id?.toString() ?: "",
                    name = dto.artist?.name ?: "Unknown Artist"
                )
            ),
            album = if (dto.album != null) {
                Album(
                    id = dto.album.id.toString(),
                    title = dto.album.title,
                    artistName = dto.artist?.name ?: "",
                    artworkUrl = artwork
                )
            } else null,
            durationMs = dto.duration * 1000L,
            artworkUrl = artwork,
            isExplicit = dto.explicitLyrics ?: false,
            streamUrl = dto.preview
        )
    }

    override suspend fun resolvePlayback(track: Track): PlaybackSource? {
        val cleanId = track.providerTrackId
        val streamUri = if (!track.streamUrl.isNullOrEmpty()) {
            Uri.parse(track.streamUrl)
        } else {
            val dto = apiService.getTrack(cleanId)
            dto.preview?.let { Uri.parse(it) }
        } ?: return null

        return PlaybackSource(
            uri = streamUri,
            mimeType = "audio/mpeg",
            providerId = providerId,
            isSeekable = true
        )
    }

    override suspend fun getCapabilities(): ProviderCapabilities {
        return ProviderCapabilities(
            search = true,
            streaming = true,
            downloads = true,
            playlists = true,
            favorites = true,
            offlinePlayback = false
        )
    }

    suspend fun searchArtists(query: String): List<Artist> {
        return try {
            val res = apiService.searchArtists(query)
            res.data?.map { dto ->
                Artist(
                    id = dto.id.toString(),
                    name = dto.name,
                    artworkUrl = dto.pictureXl ?: dto.pictureMedium
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchAlbums(query: String): List<Album> {
        return try {
            val res = apiService.searchAlbums(query)
            res.data?.map { dto ->
                Album(
                    id = dto.id.toString(),
                    title = dto.title,
                    artistName = "",
                    artworkUrl = dto.coverXl ?: dto.coverMedium
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getArtistTopTracks(artistId: String, artistName: String): List<Track> {
        return try {
            if (artistId.isNotBlank() && artistId.toLongOrNull() != null) {
                val res = apiService.getArtistTopTracks(artistId, limit = 40)
                val tracks = res.data?.map { mapDtoToTrack(it) } ?: emptyList()
                if (tracks.isNotEmpty()) tracks else search("artist:\"$artistName\"")
            } else {
                search("artist:\"$artistName\"")
            }
        } catch (e: Exception) {
            try {
                search(artistName)
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getArtistAlbums(artistId: String, artistName: String): List<Album> {
        return try {
            if (artistId.isNotBlank() && artistId.toLongOrNull() != null) {
                val res = apiService.getArtistAlbums(artistId, limit = 30)
                res.data?.map { dto ->
                    Album(
                        id = dto.id.toString(),
                        title = dto.title,
                        artistName = artistName,
                        artworkUrl = dto.coverXl ?: dto.coverMedium
                    )
                } ?: emptyList()
            } else {
                val searchRes = apiService.searchAlbums(artistName, limit = 20)
                searchRes.data?.map { dto ->
                    Album(
                        id = dto.id.toString(),
                        title = dto.title,
                        artistName = artistName,
                        artworkUrl = dto.coverXl ?: dto.coverMedium
                    )
                } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAlbumTracks(albumId: String, albumTitle: String): List<Track> {
        return try {
            if (albumId.isNotBlank() && albumId.toLongOrNull() != null) {
                val res = apiService.getAlbum(albumId)
                val tracks = res.tracks?.data?.map { mapDtoToTrack(it) } ?: emptyList()
                if (tracks.isNotEmpty()) tracks else search("album:\"$albumTitle\"")
            } else {
                search("album:\"$albumTitle\"")
            }
        } catch (e: Exception) {
            try {
                search(albumTitle)
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }
}

