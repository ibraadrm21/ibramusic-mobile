package com.ibramusic.mobile.core.di

import android.content.Context
import com.ibramusic.mobile.core.network.DeezerApiService
import com.ibramusic.mobile.data.provider.DeezerProvider
import com.ibramusic.mobile.data.provider.LocalAudioProvider
import com.ibramusic.mobile.data.provider.ProviderRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideDeezerProvider(apiService: DeezerApiService): DeezerProvider {
        return DeezerProvider(apiService)
    }

    @Provides
    @Singleton
    fun provideLocalAudioProvider(@ApplicationContext context: Context): LocalAudioProvider {
        return LocalAudioProvider(context)
    }

    @Provides
    @Singleton
    fun provideProviderRegistry(
        deezerProvider: DeezerProvider,
        localAudioProvider: LocalAudioProvider
    ): ProviderRegistry {
        return ProviderRegistry().apply {
            register(deezerProvider)
            register(localAudioProvider)
        }
    }
}

