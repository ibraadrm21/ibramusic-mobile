package com.ibramusic.mobile.data.repository

import com.ibramusic.mobile.domain.model.PlaybackQueue
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.QueueRepository
import com.ibramusic.mobile.playback.queue.QueueManager
import com.ibramusic.mobile.playback.resolver.UnifiedPlaybackResolver
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueRepositoryImpl @Inject constructor(
    private val queueManager: QueueManager,
    private val resolver: UnifiedPlaybackResolver
) : QueueRepository {

    override val queue: StateFlow<PlaybackQueue> = queueManager.queueState

    override suspend fun setContextQueue(tracks: List<Track>, startIndex: Int) {
        queueManager.setContext(tracks, startIndex)
        val upcoming = tracks.drop(startIndex + 1).take(2)
        upcoming.forEach { resolver.preResolve(it) }
    }

    override suspend fun playNext(track: Track) {
        queueManager.playNext(track)
        resolver.preResolve(track)
    }

    override suspend fun addToQueue(track: Track) {
        queueManager.addToQueue(track)
        resolver.preResolve(track)
    }

    override suspend fun removeFromQueue(queueEntryId: String) {
        queueManager.removeFromQueue(queueEntryId)
    }

    override suspend fun reorderUserQueue(fromIndex: Int, toIndex: Int) {
        queueManager.reorderUserQueue(fromIndex, toIndex)
    }

    override suspend fun reorderPlaylistQueue(fromIndex: Int, toIndex: Int) {
        queueManager.reorderPlaylistQueue(fromIndex, toIndex)
    }

    override suspend fun clearUserQueue() {
        queueManager.clearUserQueue()
    }

    override suspend fun clearAll() {
        queueManager.clearAll()
    }

    override suspend fun restorePersistedQueue(): PlaybackQueue {
        return queueManager.restoreQueue()
    }

    override suspend fun persistQueue() {
        queueManager.persistQueue()
    }
}

