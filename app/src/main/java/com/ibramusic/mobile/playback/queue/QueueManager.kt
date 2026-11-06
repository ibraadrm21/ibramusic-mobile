package com.ibramusic.mobile.playback.queue

import com.ibramusic.mobile.core.common.Constants
import com.ibramusic.mobile.data.local.dao.QueueDao
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.QueueEntity
import com.ibramusic.mobile.data.local.entity.TrackEntity
import com.ibramusic.mobile.domain.model.PlaybackQueue
import com.ibramusic.mobile.domain.model.QueueItem
import com.ibramusic.mobile.domain.model.RepeatMode
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

sealed class PreviousResult {
    data object RestartCurrent : PreviousResult()
    data class PlayTrack(val track: Track) : PreviousResult()
    data object None : PreviousResult()
}

@Singleton
class QueueManager @Inject constructor(
    private val queueDao: QueueDao,
    private val trackDao: TrackDao
) {
    private val mutex = Mutex()

    private var currentItem: QueueItem? = null
    private val userQueue = mutableListOf<QueueItem>()
    private val playlistQueue = mutableListOf<QueueItem>()
    private val originalPlaylistQueue = mutableListOf<QueueItem>()
    private val history = mutableListOf<QueueItem>()

    private var repeatMode: RepeatMode = RepeatMode.OFF
    private var isShuffleEnabled: Boolean = false

    private val _queueState = MutableStateFlow(PlaybackQueue())
    val queueState: StateFlow<PlaybackQueue> = _queueState.asStateFlow()

    private fun updateStateLocked() {
        _queueState.value = PlaybackQueue(
            current = currentItem,
            userQueue = userQueue.toList(),
            playlistQueue = playlistQueue.toList()
        )
    }

    fun peekNextTrack(): Track? {
        if (repeatMode == RepeatMode.ONE && currentItem != null) {
            return currentItem?.track
        }
        if (userQueue.isNotEmpty()) return userQueue.first().track
        if (playlistQueue.isNotEmpty()) return playlistQueue.first().track
        if (repeatMode == RepeatMode.ALL && originalPlaylistQueue.isNotEmpty()) {
            return originalPlaylistQueue.first().track
        }
        return null
    }

    fun hasNextTrack(): Boolean {
        return repeatMode == RepeatMode.ONE || userQueue.isNotEmpty() || playlistQueue.isNotEmpty() || (repeatMode == RepeatMode.ALL && originalPlaylistQueue.isNotEmpty())
    }

    fun hasPreviousTrack(): Boolean {
        return history.isNotEmpty() || currentItem != null
    }

    suspend fun setContext(tracks: List<Track>, startIndex: Int = 0): Track? = mutex.withLock {
        if (tracks.isEmpty()) return@withLock null

        val validIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val currentTrack = tracks[validIndex]
        currentItem = QueueItem(track = currentTrack)

        // Keep the full playlist context so RepeatMode.ALL loops all tracks from the beginning
        originalPlaylistQueue.clear()
        originalPlaylistQueue.addAll(tracks.map { QueueItem(track = it) })

        val upcomingContextTracks = if (validIndex + 1 < tracks.size) {
            tracks.subList(validIndex + 1, tracks.size)
        } else {
            emptyList()
        }

        playlistQueue.clear()
        val upcomingItems = upcomingContextTracks.map { QueueItem(track = it) }
        if (isShuffleEnabled) {
            val shuffled = upcomingItems.toMutableList()
            shuffled.shuffle()
            playlistQueue.addAll(shuffled)
        } else {
            playlistQueue.addAll(upcomingItems)
        }

        // Keep past tracks in history if started mid-list
        history.clear()
        if (validIndex > 0) {
            for (i in 0 until validIndex) {
                history.add(QueueItem(track = tracks[i]))
            }
        }

        updateStateLocked()
        persistQueueLocked()
        currentTrack
    }

    suspend fun playNext(track: Track): Unit = mutex.withLock {
        val item = QueueItem(track = track)
        userQueue.add(0, item)
        updateStateLocked()
        persistQueueLocked()
    }

    suspend fun addToQueue(track: Track): Unit = mutex.withLock {
        val item = QueueItem(track = track)
        userQueue.add(item)
        updateStateLocked()
        persistQueueLocked()
    }

    suspend fun removeFromQueue(queueEntryId: String): Unit = mutex.withLock {
        userQueue.removeAll { it.queueEntryId == queueEntryId }
        playlistQueue.removeAll { it.queueEntryId == queueEntryId }
        originalPlaylistQueue.removeAll { it.queueEntryId == queueEntryId }
        updateStateLocked()
        persistQueueLocked()
    }

    suspend fun reorderUserQueue(fromIndex: Int, toIndex: Int): Unit = mutex.withLock {
        if (fromIndex in userQueue.indices && toIndex in userQueue.indices && fromIndex != toIndex) {
            val item = userQueue.removeAt(fromIndex)
            userQueue.add(toIndex, item)
            updateStateLocked()
            persistQueueLocked()
        }
    }

    suspend fun reorderPlaylistQueue(fromIndex: Int, toIndex: Int): Unit = mutex.withLock {
        if (fromIndex in playlistQueue.indices && toIndex in playlistQueue.indices && fromIndex != toIndex) {
            val item = playlistQueue.removeAt(fromIndex)
            playlistQueue.add(toIndex, item)
            updateStateLocked()
            persistQueueLocked()
        }
    }

    suspend fun clearUserQueue(): Unit = mutex.withLock {
        userQueue.clear()
        updateStateLocked()
        persistQueueLocked()
    }

    suspend fun clearAll(): Unit = mutex.withLock {
        currentItem = null
        userQueue.clear()
        playlistQueue.clear()
        originalPlaylistQueue.clear()
        history.clear()
        updateStateLocked()
        queueDao.clearAll()
    }

    suspend fun setRepeatMode(mode: RepeatMode): Unit = mutex.withLock {
        repeatMode = mode
    }

    suspend fun toggleShuffle(): Boolean = mutex.withLock {
        isShuffleEnabled = !isShuffleEnabled
        if (isShuffleEnabled) {
            val shuffled = playlistQueue.toMutableList()
            shuffled.shuffle()
            playlistQueue.clear()
            playlistQueue.addAll(shuffled)
        } else {
            // Restore remaining unplayed items in their original order
            val remainingTrackIds = playlistQueue.map { it.track.id }.toSet()
            val restored = originalPlaylistQueue
                .filter { it.track.id in remainingTrackIds }
                .map { QueueItem(track = it.track) }
            playlistQueue.clear()
            playlistQueue.addAll(restored)
        }
        updateStateLocked()
        persistQueueLocked()
        isShuffleEnabled
    }

    suspend fun nextTrack(): Track? = mutex.withLock {
        // Repeat One keeps current track
        if (repeatMode == RepeatMode.ONE && currentItem != null) {
            return@withLock currentItem?.track
        }

        // Archive current item to history
        currentItem?.let { history.add(it) }

        // 1. User priority queue first
        if (userQueue.isNotEmpty()) {
            val nextItem = userQueue.removeAt(0)
            currentItem = nextItem
            updateStateLocked()
            persistQueueLocked()
            return@withLock nextItem.track
        }

        // 2. Playlist / Context queue
        if (playlistQueue.isNotEmpty()) {
            val nextItem = playlistQueue.removeAt(0)
            currentItem = nextItem
            updateStateLocked()
            persistQueueLocked()
            return@withLock nextItem.track
        }

        // 3. Repeat ALL mode loops the whole playlist from the beginning
        if (repeatMode == RepeatMode.ALL && originalPlaylistQueue.isNotEmpty()) {
            playlistQueue.clear()
            val freshItems = originalPlaylistQueue.map { QueueItem(track = it.track) }
            if (isShuffleEnabled) {
                val shuffled = freshItems.toMutableList()
                shuffled.shuffle()
                playlistQueue.addAll(shuffled)
            } else {
                playlistQueue.addAll(freshItems)
            }

            val nextItem = playlistQueue.removeAt(0)
            currentItem = nextItem
            updateStateLocked()
            persistQueueLocked()
            return@withLock nextItem.track
        }

        // End of queue
        currentItem = null
        updateStateLocked()
        persistQueueLocked()
        null
    }

    suspend fun previousTrack(currentPositionMs: Long): PreviousResult = mutex.withLock {
        // Threshold check (>3000ms restarts current track)
        if (currentPositionMs > Constants.PREVIOUS_TRACK_RESTART_THRESHOLD_MS) {
            return@withLock PreviousResult.RestartCurrent
        }

        if (history.isNotEmpty()) {
            val prevItem = history.removeAt(history.lastIndex)
            // Push current back to the front of playlistQueue so forward navigation works
            currentItem?.let { playlistQueue.add(0, it) }
            currentItem = prevItem
            updateStateLocked()
            persistQueueLocked()
            return@withLock PreviousResult.PlayTrack(prevItem.track)
        }

        // If no history, restart current if present
        if (currentItem != null) {
            return@withLock PreviousResult.RestartCurrent
        }

        PreviousResult.None
    }

    suspend fun getCurrentTrack(): Track? = mutex.withLock {
        currentItem?.track
    }

    private suspend fun persistQueueLocked() {
        val entitiesToSave = mutableListOf<QueueEntity>()
        var order = 0

        currentItem?.let { item ->
            trackDao.upsertTrack(TrackEntity.fromDomain(item.track))
            entitiesToSave.add(
                QueueEntity(
                    queueEntryId = item.queueEntryId,
                    trackId = item.track.id,
                    queueType = "CURRENT",
                    positionOrder = order++,
                    addedAt = item.addedAt
                )
            )
        }

        userQueue.forEach { item ->
            trackDao.upsertTrack(TrackEntity.fromDomain(item.track))
            entitiesToSave.add(
                QueueEntity(
                    queueEntryId = item.queueEntryId,
                    trackId = item.track.id,
                    queueType = "USER",
                    positionOrder = order++,
                    addedAt = item.addedAt
                )
            )
        }

        playlistQueue.forEach { item ->
            trackDao.upsertTrack(TrackEntity.fromDomain(item.track))
            entitiesToSave.add(
                QueueEntity(
                    queueEntryId = item.queueEntryId,
                    trackId = item.track.id,
                    queueType = "CONTEXT",
                    positionOrder = order++,
                    addedAt = item.addedAt
                )
            )
        }

        originalPlaylistQueue.forEach { item ->
            trackDao.upsertTrack(TrackEntity.fromDomain(item.track))
            entitiesToSave.add(
                QueueEntity(
                    queueEntryId = "orig_" + item.queueEntryId,
                    trackId = item.track.id,
                    queueType = "ORIGINAL_CONTEXT",
                    positionOrder = order++,
                    addedAt = item.addedAt
                )
            )
        }

        queueDao.clearAll()
        queueDao.insertQueueItems(entitiesToSave)
    }

    suspend fun persistQueue(): Unit = mutex.withLock {
        persistQueueLocked()
    }

    suspend fun restoreQueue(): PlaybackQueue = mutex.withLock {
        val entities = queueDao.getAllQueueItems()
        if (entities.isEmpty()) return@withLock _queueState.value

        currentItem = null
        userQueue.clear()
        playlistQueue.clear()
        originalPlaylistQueue.clear()
        history.clear()

        var hasOriginalContext = false

        for (entity in entities) {
            val trackEntity = trackDao.getTrackById(entity.trackId) ?: continue
            val track = trackEntity.toDomain()
            val queueItem = QueueItem(
                queueEntryId = entity.queueEntryId.removePrefix("orig_"),
                track = track,
                addedAt = entity.addedAt
            )

            when (entity.queueType) {
                "CURRENT" -> currentItem = queueItem
                "USER" -> userQueue.add(queueItem)
                "CONTEXT" -> playlistQueue.add(queueItem)
                "ORIGINAL_CONTEXT" -> {
                    originalPlaylistQueue.add(queueItem)
                    hasOriginalContext = true
                }
            }
        }

        if (!hasOriginalContext) {
            originalPlaylistQueue.clear()
            currentItem?.let { originalPlaylistQueue.add(it) }
            originalPlaylistQueue.addAll(playlistQueue)
        }

        updateStateLocked()
        _queueState.value
    }
}

