package com.ibramusic.mobile.core.di

import com.ibramusic.mobile.data.repository.AlbumRepositoryImpl
import com.ibramusic.mobile.data.repository.ArtistRepositoryImpl
import com.ibramusic.mobile.data.repository.AuthRepositoryImpl
import com.ibramusic.mobile.data.repository.DownloadRepositoryImpl
import com.ibramusic.mobile.data.repository.PlaybackRepositoryImpl
import com.ibramusic.mobile.data.repository.PlaylistRepositoryImpl
import com.ibramusic.mobile.data.repository.QueueRepositoryImpl
import com.ibramusic.mobile.data.repository.SettingsRepositoryImpl
import com.ibramusic.mobile.data.repository.TrackRepositoryImpl
import com.ibramusic.mobile.domain.repository.AlbumRepository
import com.ibramusic.mobile.domain.repository.ArtistRepository
import com.ibramusic.mobile.domain.repository.AuthRepository
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.domain.repository.PlaybackRepository
import com.ibramusic.mobile.domain.repository.PlaylistRepository
import com.ibramusic.mobile.domain.repository.QueueRepository
import com.ibramusic.mobile.domain.repository.SettingsRepository
import com.ibramusic.mobile.domain.repository.TrackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(impl: PlaybackRepositoryImpl): PlaybackRepository

    @Binds
    @Singleton
    abstract fun bindQueueRepository(impl: QueueRepositoryImpl): QueueRepository

    @Binds
    @Singleton
    abstract fun bindTrackRepository(impl: TrackRepositoryImpl): TrackRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindArtistRepository(impl: ArtistRepositoryImpl): ArtistRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: AlbumRepositoryImpl): AlbumRepository
}
