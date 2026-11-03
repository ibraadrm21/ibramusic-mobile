package com.ibramusic.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DeezerSearchResponse(
    @SerializedName("data") val data: List<DeezerTrackDto>?,
    @SerializedName("total") val total: Int?
)

data class DeezerTrackDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("title_short") val titleShort: String?,
    @SerializedName("duration") val duration: Int,
    @SerializedName("preview") val preview: String?,
    @SerializedName("explicit_lyrics") val explicitLyrics: Boolean?,
    @SerializedName("artist") val artist: DeezerArtistDto?,
    @SerializedName("album") val album: DeezerAlbumDto?
)

data class DeezerArtistDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("picture_medium") val pictureMedium: String?,
    @SerializedName("picture_xl") val pictureXl: String?
)

data class DeezerAlbumDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("cover_medium") val coverMedium: String?,
    @SerializedName("cover_xl") val coverXl: String?
)

data class DeezerAlbumResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("cover_xl") val coverXl: String?,
    @SerializedName("tracks") val tracks: DeezerTracksContainer?
)

data class DeezerTracksContainer(
    @SerializedName("data") val data: List<DeezerTrackDto>?
)

data class DeezerArtistResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("picture_xl") val pictureXl: String?,
    @SerializedName("nb_fan") val nbFan: Long?
)

data class DeezerArtistSearchResponse(
    @SerializedName("data") val data: List<DeezerArtistDto>?,
    @SerializedName("total") val total: Int?
)

data class DeezerAlbumSearchResponse(
    @SerializedName("data") val data: List<DeezerAlbumDto>?,
    @SerializedName("total") val total: Int?
)

