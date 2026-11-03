package com.ibramusic.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ibramusic.mobile.data.local.dao.AlbumDao
import com.ibramusic.mobile.data.local.dao.ArtistDao
import com.ibramusic.mobile.data.local.dao.PlaylistDao
import com.ibramusic.mobile.data.local.dao.QueueDao
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.FollowedArtistEntity
import com.ibramusic.mobile.data.local.entity.PlaylistEntity
import com.ibramusic.mobile.data.local.entity.PlaylistTrackEntity
import com.ibramusic.mobile.data.local.entity.QueueEntity
import com.ibramusic.mobile.data.local.entity.SavedAlbumEntity
import com.ibramusic.mobile.data.local.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        QueueEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FollowedArtistEntity::class,
        SavedAlbumEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class IbraMusicDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun queueDao(): QueueDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
}
