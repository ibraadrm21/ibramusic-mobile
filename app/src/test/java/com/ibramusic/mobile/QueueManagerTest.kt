package com.ibramusic.mobile

import com.ibramusic.mobile.core.common.Constants
import com.ibramusic.mobile.data.local.dao.QueueDao
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.QueueEntity
import com.ibramusic.mobile.data.local.entity.TrackEntity
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.RepeatMode
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.playback.queue.PreviousResult
import com.ibramusic.mobile.playback.queue.QueueManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTrackDao : TrackDao {
    private val tracks = mutableMapOf<String, TrackEntity>()

    override suspend fun insertOrUpdate(track: TrackEntity) {
        tracks[track.id] = track
    }

    override suspend fun insertAll(tracks: List<TrackEntity>) {
        tracks.forEach { this.tracks[it.id] = it }
    }

    override suspend fun getTrackById(id: String): TrackEntity? = tracks[id]

    override fun getLikedTracks(): Flow<List<TrackEntity>> = flowOf(tracks.values.filter { it.isLiked })

    override suspend fun getLikedTracksList(): List<TrackEntity> = tracks.values.filter { it.isLiked }

    override suspend fun isTrackLiked(trackId: String): Boolean? = tracks[trackId]?.isLiked

    override suspend fun setLiked(trackId: String, isLiked: Boolean) {
        tracks[trackId]?.let { tracks[trackId] = it.copy(isLiked = isLiked) }
    }

    override fun getDownloadedTracks(): Flow<List<TrackEntity>> = flowOf(tracks.values.filter { it.isDownloaded })

    override fun getListeningHistory(): Flow<List<TrackEntity>> = flowOf(tracks.values.toList())

    override suspend fun updateLastPlayedAt(trackId: String, timestamp: Long): Int {
        tracks[trackId]?.let {
            tracks[trackId] = it.copy(lastPlayedAt = timestamp)
            return 1
        }
        return 0
    }
}

class FakeQueueDao : QueueDao {
    private val queueItems = mutableListOf<QueueEntity>()

    override suspend fun insertQueueItems(items: List<QueueEntity>) {
        queueItems.addAll(items)
    }

    override suspend fun getAllQueueItems(): List<QueueEntity> = queueItems.sortedBy { it.positionOrder }

    override suspend fun clearQueueByType(type: String) {
        queueItems.removeAll { it.queueType == type }
    }

    override suspend fun clearAll() {
        queueItems.clear()
    }
}

class QueueManagerTest {

    private lateinit var queueManager: QueueManager
    private lateinit var fakeTrackDao: FakeTrackDao
    private lateinit var fakeQueueDao: FakeQueueDao

    private fun createTestTrack(id: String, title: String): Track {
        return Track(
            id = id,
            providerId = "test",
            providerTrackId = id,
            title = title,
            artists = listOf(Artist("art_1", "Test Artist")),
            album = Album("alb_1", "Test Album", "Test Artist"),
            durationMs = 200_000L
        )
    }

    @Before
    fun setUp() {
        fakeTrackDao = FakeTrackDao()
        fakeQueueDao = FakeQueueDao()
        queueManager = QueueManager(fakeQueueDao, fakeTrackDao)
    }

    @Test
    fun testSetContext_setsCurrentAndUpcoming() = runBlocking {
        val tracks = listOf(
            createTestTrack("1", "Track 1"),
            createTestTrack("2", "Track 2"),
            createTestTrack("3", "Track 3")
        )

        val current = queueManager.setContext(tracks, 0)
        assertEquals("Track 1", current?.title)

        val state = queueManager.queueState.value
        assertEquals("Track 1", state.current?.track?.title)
        assertEquals(2, state.playlistQueue.size)
        assertEquals("Track 2", state.playlistQueue[0].track.title)
        assertEquals("Track 3", state.playlistQueue[1].track.title)
    }

    @Test
    fun testPlayNext_insertsAtFrontOfUserQueue() = runBlocking {
        val tracks = listOf(createTestTrack("1", "Track 1"), createTestTrack("2", "Track 2"))
        queueManager.setContext(tracks, 0)

        val priorityTrack = createTestTrack("p1", "Priority Next Track")
        queueManager.playNext(priorityTrack)

        val state = queueManager.queueState.value
        assertEquals(1, state.userQueue.size)
        assertEquals("Priority Next Track", state.userQueue.first().track.title)

        // nextTrack() must deliver user queue first before context
        val next = queueManager.nextTrack()
        assertEquals("Priority Next Track", next?.title)

        val nextAfterPriority = queueManager.nextTrack()
        assertEquals("Track 2", nextAfterPriority?.title)
    }

    @Test
    fun testAddToQueue_appendsToEndOfUserQueue() = runBlocking {
        val tracks = listOf(createTestTrack("1", "Track 1"))
        queueManager.setContext(tracks, 0)

        queueManager.addToQueue(createTestTrack("u1", "Queued 1"))
        queueManager.addToQueue(createTestTrack("u2", "Queued 2"))

        val state = queueManager.queueState.value
        assertEquals(2, state.userQueue.size)
        assertEquals("Queued 1", state.userQueue[0].track.title)
        assertEquals("Queued 2", state.userQueue[1].track.title)
    }

    @Test
    fun testPreviousTrack_thresholdRestartVsPrevious() = runBlocking {
        val tracks = listOf(
            createTestTrack("1", "Track 1"),
            createTestTrack("2", "Track 2")
        )
        queueManager.setContext(tracks, 0)
        queueManager.nextTrack() // Now playing Track 2, Track 1 is in history

        // Case 1: Position > 3000ms -> Must restart current
        val res1 = queueManager.previousTrack(3500L)
        assertTrue(res1 is PreviousResult.RestartCurrent)

        // Case 2: Position <= 3000ms -> Must navigate to previous
        val res2 = queueManager.previousTrack(1500L)
        assertTrue(res2 is PreviousResult.PlayTrack)
        assertEquals("Track 1", (res2 as PreviousResult.PlayTrack).track.title)
    }

    @Test
    fun testRepeatModeOne_keepsCurrentTrack() = runBlocking {
        val tracks = listOf(createTestTrack("1", "Track 1"), createTestTrack("2", "Track 2"))
        queueManager.setContext(tracks, 0)
        queueManager.setRepeatMode(RepeatMode.ONE)

        val next = queueManager.nextTrack()
        assertEquals("Track 1", next?.title)
    }

    @Test
    fun testRepeatModeAll_loopsWholePlaylistWhenLastTrackEnds() = runBlocking {
        val tracks = listOf(
            createTestTrack("1", "Track 1"),
            createTestTrack("2", "Track 2"),
            createTestTrack("3", "Track 3")
        )
        queueManager.setContext(tracks, 0)
        queueManager.setRepeatMode(RepeatMode.ALL)

        assertEquals("Track 2", queueManager.nextTrack()?.title)
        assertEquals("Track 3", queueManager.nextTrack()?.title)

        // At last track, peekNextTrack should peek track 1 for pre-cueing & crossfade
        assertEquals("Track 1", queueManager.peekNextTrack()?.title)
        assertTrue(queueManager.hasNextTrack())

        // Next track should loop back to track 1
        assertEquals("Track 1", queueManager.nextTrack()?.title)
        assertEquals("Track 2", queueManager.nextTrack()?.title)
    }

    @Test
    fun testRepeatModeAll_startingAtLastTrack_loopsBackToBeginning() = runBlocking {
        val tracks = listOf(
            createTestTrack("1", "Track 1"),
            createTestTrack("2", "Track 2"),
            createTestTrack("3", "Track 3")
        )
        // Start directly at the last track
        queueManager.setContext(tracks, 2)
        queueManager.setRepeatMode(RepeatMode.ALL)

        assertEquals("Track 1", queueManager.peekNextTrack()?.title)
        assertTrue(queueManager.hasNextTrack())

        // Advancing from last track should restart from Track 1
        assertEquals("Track 1", queueManager.nextTrack()?.title)
        assertEquals("Track 2", queueManager.nextTrack()?.title)
    }

    @Test
    fun testQueuePersistenceAndRestoration() = runBlocking {
        val tracks = listOf(createTestTrack("1", "Track 1"), createTestTrack("2", "Track 2"))
        queueManager.setContext(tracks, 0)
        queueManager.addToQueue(createTestTrack("u1", "User Track"))

        // Persist
        queueManager.persistQueue()

        // Create new manager instance
        val newManager = QueueManager(fakeQueueDao, fakeTrackDao)
        val restored = newManager.restoreQueue()

        assertEquals("Track 1", restored.current?.track?.title)
        assertEquals(1, restored.userQueue.size)
        assertEquals("User Track", restored.userQueue.first().track.title)
        assertEquals(1, restored.playlistQueue.size)
        assertEquals("Track 2", restored.playlistQueue.first().track.title)
    }
}

