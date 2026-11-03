package com.ibramusic.mobile.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val downloadedTrackCount: Int = 0,
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

