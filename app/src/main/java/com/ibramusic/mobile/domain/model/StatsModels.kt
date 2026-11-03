package com.ibramusic.mobile.domain.model

data class UserStats(
    val totalSeconds: Long = 0L,
    val totalPlays: Int = 0,
    val topTracks: List<TopTrackStat> = emptyList(),
    val topArtists: List<TopArtistStat> = emptyList()
) {
    val totalMinutes: Long get() = totalSeconds / 60L
    val totalHours: Long get() = totalMinutes / 60L
}

data class TopTrackStat(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val count: Int = 0,
    val seconds: Long = 0L
) {
    val minutes: Long get() = seconds / 60L
}

data class TopArtistStat(
    val name: String,
    val artworkUrl: String? = null,
    val count: Int = 0,
    val seconds: Long = 0L
) {
    val minutes: Long get() = seconds / 60L
}

