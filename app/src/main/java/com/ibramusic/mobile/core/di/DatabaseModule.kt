package com.ibramusic.mobile.core.di

import android.content.Context
import androidx.room.Room
import com.ibramusic.mobile.core.common.Constants
import com.ibramusic.mobile.core.database.IbraMusicDatabase
import com.ibramusic.mobile.data.local.dao.AlbumDao
import com.ibramusic.mobile.data.local.dao.ArtistDao
import com.ibramusic.mobile.data.local.dao.PlaylistDao
import com.ibramusic.mobile.data.local.dao.QueueDao
import com.ibramusic.mobile.data.local.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IbraMusicDatabase {
        return Room.databaseBuilder(
            context,
            IbraMusicDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTrackDao(database: IbraMusicDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    fun provideQueueDao(database: IbraMusicDatabase): QueueDao {
        return database.queueDao()
    }

    @Provides
    fun providePlaylistDao(database: IbraMusicDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideArtistDao(database: IbraMusicDatabase): ArtistDao {
        return database.artistDao()
    }

    @Provides
    fun provideAlbumDao(database: IbraMusicDatabase): AlbumDao {
        return database.albumDao()
    }
}
