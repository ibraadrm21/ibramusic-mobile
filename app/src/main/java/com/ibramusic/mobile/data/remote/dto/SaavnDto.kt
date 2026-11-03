package com.ibramusic.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SaavnResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: SaavnData?
)

data class SaavnData(
    @SerializedName("total") val total: Int?,
    @SerializedName("results") val results: List<SaavnSongDto>?
)

data class SaavnSongDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("downloadUrl") val downloadUrl: List<SaavnDownloadUrlDto>?,
    @SerializedName("artists") val artists: SaavnArtistsDto?
)

data class SaavnArtistsDto(
    @SerializedName("primary") val primary: List<SaavnArtistDto>?,
    @SerializedName("all") val all: List<SaavnArtistDto>?
)

data class SaavnArtistDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class SaavnDownloadUrlDto(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url") val url: String?
)
