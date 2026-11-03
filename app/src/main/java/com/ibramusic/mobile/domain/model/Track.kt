package com.ibramusic.mobile.domain.model

data class Track(
    val id: String,
    val providerId: String,
    val providerTrackId: String,
    val title: String,
    val artists: List<Artist>,
    val album: Album? = null,
    val durationMs: Long = 0L,
    val artworkUrl: String? = null,
    val isExplicit: Boolean = false,
    val availableOffline: Boolean = false,
    val streamUrl: String? = null,
    val isLiked: Boolean = false
) {
    val artistName: String
        get() = artists.joinToString(", ") { it.name }.ifEmpty { "Unknown Artist" }
}

data class Artist(
    val id: String,
    val name: String,
    val artworkUrl: String? = null,
    val monthlyListeners: Long? = null
)

data class Album(
    val id: String,
    val title: String,
    val artistName: String,
    val artworkUrl: String? = null,
    val releaseYear: Int? = null,
    val totalTracks: Int? = null
)
