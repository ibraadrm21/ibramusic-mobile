package com.ibramusic.mobile.domain.model

import java.util.UUID

data class QueueItem(
    val queueEntryId: String = UUID.randomUUID().toString(),
    val track: Track,
    val addedAt: Long = System.currentTimeMillis()
)

data class PlaybackQueue(
    val current: QueueItem? = null,
    val userQueue: List<QueueItem> = emptyList(),
    val playlistQueue: List<QueueItem> = emptyList()
) {
    val totalCount: Int
        get() = (if (current != null) 1 else 0) + userQueue.size + playlistQueue.size

    val allUpcoming: List<QueueItem>
        get() = userQueue + playlistQueue
}

