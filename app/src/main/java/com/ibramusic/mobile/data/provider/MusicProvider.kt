package com.ibramusic.mobile.data.provider

import com.ibramusic.mobile.domain.model.PlaybackSource
import com.ibramusic.mobile.domain.model.ProviderCapabilities
import com.ibramusic.mobile.domain.model.Track

interface MusicProvider {
    val providerId: String
    val displayName: String

    suspend fun search(query: String): List<Track>
    suspend fun getTrack(id: String): Track?
    suspend fun resolvePlayback(track: Track): PlaybackSource?
    suspend fun getCapabilities(): ProviderCapabilities
}

class ProviderRegistry {
    private val providers = mutableMapOf<String, MusicProvider>()

    fun register(provider: MusicProvider) {
        providers[provider.providerId] = provider
    }

    fun getProvider(providerId: String): MusicProvider? {
        return providers[providerId]
    }

    fun getAllProviders(): List<MusicProvider> {
        return providers.values.toList()
    }
}

