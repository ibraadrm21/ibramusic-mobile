package com.ibramusic.mobile.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}

object Constants {
    const val PREVIOUS_TRACK_RESTART_THRESHOLD_MS = 3000L
    const val NOTIFICATION_CHANNEL_ID = "ibramusic_playback"
    const val NOTIFICATION_CHANNEL_NAME = "IbraMusic Playback"
    const val NOTIFICATION_ID = 1001
    const val PREFS_NAME = "ibramusic_preferences"
    const val DATABASE_NAME = "ibramusic.db"
    
    // Deezer API (Public Metadata)
    const val DEEZER_API_BASE_URL = "https://api.deezer.com/"

    // Full High-Quality 320kbps Audio Stream Resolver (Saavn)
    const val SAAVN_API_BASE_URL = "https://saavn.sumit.co/"

    // Supabase Cloud Sync & Authentication
    const val SUPABASE_URL = "https://nkhonqrseaymilneurgj.supabase.co/"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5raG9ucXJzZWF5bWlsbmV1cmdqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE1NDQ1NTgsImV4cCI6MjA5NzEyMDU1OH0.zdzwopG3BXoTmqzuoEPQ0FcsBUhmjgrgMirWZFTZcPo"
    
    // Fallback Stream Proxies (Piped / Invidious)
    val PIPED_HOSTS = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.video",
        "https://pipedapi.drgns.space"
    )
}

