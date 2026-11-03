package com.ibramusic.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SupabaseAuthRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class SupabaseAuthResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("expires_in") val expiresIn: Long? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("user") val user: SupabaseUserDto? = null
)

data class SupabaseRefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class SupabaseUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String?,
    @SerializedName("user_metadata") val userMetadata: Map<String, Any?>? = null
)

data class WebArtistDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null
)

data class WebTrackDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("artistId") val artistId: String? = null,
    @SerializedName("artists") val artists: List<WebArtistDto>? = null,
    @SerializedName("albumName") val albumName: String? = null,
    @SerializedName("albumId") val albumId: String? = null,
    @SerializedName("duration") val duration: Double? = null, // in seconds (web model)
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("audioUrl") val audioUrl: String? = null,
    @SerializedName("isExplicit") val isExplicit: Boolean? = false
)

data class WebPlaylistDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("isPublic") val isPublic: Boolean? = false,
    @SerializedName("tracks") val tracks: List<WebTrackDto>? = emptyList()
)

data class WebSyncData(
    @SerializedName("favorites") val favorites: List<WebTrackDto>? = null,
    @SerializedName("playlists") val playlists: List<WebPlaylistDto>? = null,
    @SerializedName("followedArtists") val followedArtists: List<Any>? = null,
    @SerializedName("themeSettings") val themeSettings: Any? = null,
    @SerializedName("accumulatedStats") val accumulatedStats: Any? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("pfp") val pfp: String? = null
)

data class SupabaseSyncRow(
    @SerializedName("id") val id: String,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("data") val data: WebSyncData? = null
)
