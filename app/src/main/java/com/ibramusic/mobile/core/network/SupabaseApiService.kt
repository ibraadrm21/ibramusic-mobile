package com.ibramusic.mobile.core.network

import com.ibramusic.mobile.data.remote.dto.SupabaseAuthRequest
import com.ibramusic.mobile.data.remote.dto.SupabaseAuthResponse
import com.ibramusic.mobile.data.remote.dto.SupabaseRefreshRequest
import com.ibramusic.mobile.data.remote.dto.SupabaseSyncRow
import com.ibramusic.mobile.data.remote.dto.SupabaseUserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SupabaseRefreshRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<Unit>

    @GET("auth/v1/user")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<SupabaseUserDto>

    @GET("rest/v1/user_sync")
    suspend fun getUserSync(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "id,updated_at,data"
    ): Response<List<SupabaseSyncRow>>

    @POST("rest/v1/user_sync")
    suspend fun upsertUserSync(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body payload: SupabaseSyncRow
    ): Response<Unit>
}
