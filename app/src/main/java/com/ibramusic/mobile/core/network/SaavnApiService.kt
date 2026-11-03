package com.ibramusic.mobile.core.network

import com.ibramusic.mobile.data.remote.dto.SaavnResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SaavnApiService {
    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("limit") limit: Int = 5
    ): SaavnResponse
}

