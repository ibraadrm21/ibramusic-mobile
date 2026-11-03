package com.ibramusic.mobile.core.network

import com.ibramusic.mobile.data.remote.dto.DeezerAlbumResponse
import com.ibramusic.mobile.data.remote.dto.DeezerAlbumSearchResponse
import com.ibramusic.mobile.data.remote.dto.DeezerArtistResponse
import com.ibramusic.mobile.data.remote.dto.DeezerArtistSearchResponse
import com.ibramusic.mobile.data.remote.dto.DeezerSearchResponse
import com.ibramusic.mobile.data.remote.dto.DeezerTrackDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DeezerApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 30
    ): DeezerSearchResponse

    @GET("search/artist")
    suspend fun searchArtists(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): DeezerArtistSearchResponse

    @GET("search/album")
    suspend fun searchAlbums(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): DeezerAlbumSearchResponse

    @GET("chart/0/tracks")
    suspend fun getChartTracks(
        @Query("limit") limit: Int = 30
    ): DeezerSearchResponse

    @GET("track/{id}")
    suspend fun getTrack(
        @Path("id") id: String
    ): DeezerTrackDto

    @GET("album/{id}")
    suspend fun getAlbum(
        @Path("id") id: String
    ): DeezerAlbumResponse

    @GET("artist/{id}")
    suspend fun getArtist(
        @Path("id") id: String
    ): DeezerArtistResponse

    @GET("artist/{id}/top")
    suspend fun getArtistTopTracks(
        @Path("id") id: String,
        @Query("limit") limit: Int = 30
    ): DeezerSearchResponse

    @GET("artist/{id}/albums")
    suspend fun getArtistAlbums(
        @Path("id") id: String,
        @Query("limit") limit: Int = 30
    ): DeezerAlbumSearchResponse
}

