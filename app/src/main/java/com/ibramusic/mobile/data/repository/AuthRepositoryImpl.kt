package com.ibramusic.mobile.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ibramusic.mobile.core.common.Constants
import com.ibramusic.mobile.core.di.AuthDataStore
import com.ibramusic.mobile.core.network.SupabaseApiService
import com.ibramusic.mobile.data.remote.dto.SupabaseAuthRequest
import com.ibramusic.mobile.data.remote.dto.SupabaseRefreshRequest
import com.ibramusic.mobile.data.sync.SupabaseSyncManager
import com.ibramusic.mobile.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @AuthDataStore private val authDataStore: DataStore<Preferences>,
    private val supabaseApiService: SupabaseApiService,
    private val supabaseSyncManager: SupabaseSyncManager
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val KEY_EXPIRES_AT = longPreferencesKey("expires_at")
        val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
    }

    private fun extractAvatar(meta: Map<String, Any?>?): String? {
        if (meta == null) return null
        return (meta["avatar_url"] as? String)
            ?: (meta["picture"] as? String)
            ?: (meta["avatar"] as? String)
            ?: (meta["pfp"] as? String)
            ?: (meta["profile_image"] as? String)
            ?: (meta["photoURL"] as? String)
            ?: (meta["image"] as? String)
    }

    private fun extractName(meta: Map<String, Any?>?): String? {
        if (meta == null) return null
        return (meta["username"] as? String)
            ?: (meta["name"] as? String)
            ?: (meta["full_name"] as? String)
            ?: (meta["user_name"] as? String)
    }

    init {
        // Auto-sync library on application start and fetch profile metadata
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = authDataStore.data.firstOrNull() ?: return@launch
                val isGuest = prefs[KEY_IS_GUEST] ?: true
                val userId = prefs[KEY_USER_ID]
                if (!isGuest && !userId.isNullOrEmpty()) {
                    refreshUserProfile()
                    val token = getValidAccessToken()
                    if (!token.isNullOrEmpty()) {
                        supabaseSyncManager.syncFromCloud(userId, token)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Startup cloud sync error: ${e.message}")
            }
        }
    }

    override val currentUserId: Flow<String?> = authDataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    override val currentUserEmail: Flow<String?> = authDataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL]
    }

    override val currentUserAvatar: Flow<String?> = authDataStore.data.map { prefs ->
        prefs[KEY_USER_AVATAR]
    }

    override val currentUserName: Flow<String?> = authDataStore.data.map { prefs ->
        prefs[KEY_USER_NAME]
    }

    override val isGuestMode: Flow<Boolean> = authDataStore.data.map { prefs ->
        prefs[KEY_IS_GUEST] ?: true
    }

    override suspend fun continueAsGuest() {
        authDataStore.edit { prefs ->
            prefs[KEY_IS_GUEST] = true
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_AVATAR)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_EXPIRES_AT)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        return try {
            val response = supabaseApiService.signIn(
                apiKey = Constants.SUPABASE_ANON_KEY,
                authorization = "Bearer ${Constants.SUPABASE_ANON_KEY}",
                request = SupabaseAuthRequest(cleanEmail, cleanPassword)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val token = body.accessToken ?: ""
                val refreshToken = body.refreshToken ?: ""
                val expiresInSeconds = body.expiresIn ?: 3600L
                val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
                val userId = body.user?.id ?: ""
                val userEmail = body.user?.email ?: cleanEmail
                val avatar = extractAvatar(body.user?.userMetadata)
                val userName = extractName(body.user?.userMetadata)

                authDataStore.edit { prefs ->
                    prefs[KEY_USER_ID] = userId
                    prefs[KEY_USER_EMAIL] = userEmail
                    if (!avatar.isNullOrEmpty()) prefs[KEY_USER_AVATAR] = avatar
                    if (!userName.isNullOrEmpty()) prefs[KEY_USER_NAME] = userName
                    prefs[KEY_ACCESS_TOKEN] = token
                    if (refreshToken.isNotEmpty()) {
                        prefs[KEY_REFRESH_TOKEN] = refreshToken
                    }
                    prefs[KEY_EXPIRES_AT] = expiresAt
                    prefs[KEY_IS_GUEST] = false
                }

                // Immediately fetch and synchronize all playlists & favorites from Supabase Web account
                supabaseSyncManager.syncFromCloud(userId, token)

                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                val formattedMsg = parseAuthErrorMessage(rawError, response.code())
                Result.failure(Exception(formattedMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        return try {
            val response = supabaseApiService.signUp(
                apiKey = Constants.SUPABASE_ANON_KEY,
                authorization = "Bearer ${Constants.SUPABASE_ANON_KEY}",
                request = SupabaseAuthRequest(cleanEmail, cleanPassword)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.accessToken.isNullOrEmpty()) {
                    Result.failure(Exception("Cuenta creada con éxito. Si tu cuenta requiere confirmación, revisa tu correo electrónico antes de iniciar sesión."))
                } else {
                    signIn(cleanEmail, cleanPassword)
                }
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                val formattedMsg = parseAuthErrorMessage(rawError, response.code())
                Result.failure(Exception(formattedMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAuthErrorMessage(rawError: String, statusCode: Int): String {
        return try {
            val json = com.google.gson.JsonParser.parseString(rawError).asJsonObject
            val msg = json.get("msg")?.asString 
                ?: json.get("message")?.asString 
                ?: json.get("error_description")?.asString 
                ?: rawError

            when {
                msg.contains("invalid login credentials", ignoreCase = true) || msg.contains("invalid_credentials", ignoreCase = true) ->
                    "Credenciales incorrectas: comprueba que el correo y la contraseña coincidan con tu cuenta de IbraMusic Web."
                msg.contains("email not confirmed", ignoreCase = true) ->
                    "Correo electrónico no verificado. Por favor, confirma tu email."
                msg.contains("user already registered", ignoreCase = true) ->
                    "Ya existe un usuario con este correo electrónico."
                msg.contains("password should be at least", ignoreCase = true) ->
                    "La contraseña debe tener al menos 6 caracteres."
                else -> msg
            }
        } catch (e: Exception) {
            if (rawError.isNotBlank()) rawError else "Error de autenticación ($statusCode)"
        }
    }

    override suspend fun refreshAccessToken(): Result<String> {
        val prefs = authDataStore.data.firstOrNull() ?: return Result.failure(Exception("No session"))
        val refreshToken = prefs[KEY_REFRESH_TOKEN]
        if (refreshToken.isNullOrEmpty()) {
            // Cannot refresh: session is stale, cleanly transition to guest mode
            signOut()
            return Result.failure(Exception("Sesión caducada. Por favor, inicia sesión con tu cuenta de nuevo."))
        }

        return try {
            Log.d(TAG, "Refreshing Supabase access token...")
            val response = supabaseApiService.refreshToken(
                apiKey = Constants.SUPABASE_ANON_KEY,
                authorization = "Bearer ${Constants.SUPABASE_ANON_KEY}",
                request = SupabaseRefreshRequest(refreshToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val newAccessToken = body.accessToken ?: ""
                val newRefreshToken = body.refreshToken ?: refreshToken
                val expiresInSeconds = body.expiresIn ?: 3600L
                val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)

                authDataStore.edit { p ->
                    p[KEY_ACCESS_TOKEN] = newAccessToken
                    p[KEY_REFRESH_TOKEN] = newRefreshToken
                    p[KEY_EXPIRES_AT] = expiresAt
                }
                Log.d(TAG, "Access token refreshed successfully")
                Result.success(newAccessToken)
            } else {
                Log.w(TAG, "Token refresh failed with code ${response.code()}; clearing session")
                signOut()
                Result.failure(Exception("Sesión caducada. Por favor, vuelve a iniciar sesión."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh exception", e)
            Result.failure(e)
        }
    }

    override suspend fun getValidAccessToken(): String? {
        val prefs = authDataStore.data.firstOrNull() ?: return null
        val isGuest = prefs[KEY_IS_GUEST] ?: true
        if (isGuest) return null

        val token = prefs[KEY_ACCESS_TOKEN]
        if (token.isNullOrEmpty()) return null

        val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
        val now = System.currentTimeMillis()

        // If expired or expiring within 60 seconds, refresh token
        if (expiresAt > 0 && now >= expiresAt - 60_000L) {
            val refreshResult = refreshAccessToken()
            return refreshResult.getOrNull()
        }

        // If no expiration recorded but refresh token exists, return current token
        return token
    }

    override suspend fun signOut() {
        try {
            var token: String? = null
            authDataStore.edit { prefs ->
                token = prefs[KEY_ACCESS_TOKEN]
                prefs.remove(KEY_USER_ID)
                prefs.remove(KEY_USER_EMAIL)
                prefs.remove(KEY_USER_AVATAR)
                prefs.remove(KEY_USER_NAME)
                prefs.remove(KEY_ACCESS_TOKEN)
                prefs.remove(KEY_REFRESH_TOKEN)
                prefs.remove(KEY_EXPIRES_AT)
                prefs[KEY_IS_GUEST] = true
            }
            if (!token.isNullOrEmpty()) {
                supabaseApiService.signOut(Constants.SUPABASE_ANON_KEY, "Bearer $token")
            }
        } catch (e: Exception) {
            // Ignore network logout errors
        }
    }

    override suspend fun syncWithCloud(): Result<Unit> {
        val prefs = authDataStore.data.firstOrNull() ?: return Result.failure(Exception("No session"))
        val isGuest = prefs[KEY_IS_GUEST] ?: true
        if (isGuest) return Result.failure(Exception("Inicia sesión para sincronizar"))

        val userId = prefs[KEY_USER_ID] ?: return Result.failure(Exception("No hay usuario conectado"))
        var token = getValidAccessToken()

        if (token.isNullOrEmpty()) {
            // Attempt an explicit refresh
            val refreshResult = refreshAccessToken()
            if (refreshResult.isSuccess) {
                token = refreshResult.getOrNull()
            } else {
                return Result.failure(Exception("Sesión expirada. Por favor, inicia sesión de nuevo."))
            }
        }

        val syncResult = supabaseSyncManager.syncFromCloud(userId, token!!)
        if (syncResult.isFailure) {
            val errorMsg = syncResult.exceptionOrNull()?.message ?: ""
            if (errorMsg.contains("JWT expired") || errorMsg.contains("PGRST303") || errorMsg.contains("401")) {
                Log.d(TAG, "JWT expired during sync. Attempting token renewal and retry...")
                val refreshResult = refreshAccessToken()
                if (refreshResult.isSuccess) {
                    val newToken = refreshResult.getOrNull()!!
                    return supabaseSyncManager.syncFromCloud(userId, newToken)
                } else {
                    return Result.failure(Exception("Sesión expirada. Inicia sesión de nuevo."))
                }
            }
        }
        return syncResult
    }

    override suspend fun linkGuestAccountToPermanent(email: String, password: String): Result<Unit> {
        return signUp(email, password)
    }

    override suspend fun refreshUserProfile(): Result<Unit> {
        val prefs = authDataStore.data.firstOrNull() ?: return Result.failure(Exception("No session"))
        val isGuest = prefs[KEY_IS_GUEST] ?: true
        val userId = prefs[KEY_USER_ID]
        if (isGuest || userId.isNullOrEmpty()) return Result.success(Unit)

        val token = getValidAccessToken() ?: return Result.failure(Exception("No token"))
        return try {
            val userRes = supabaseApiService.getUser(
                apiKey = Constants.SUPABASE_ANON_KEY,
                authorization = "Bearer $token"
            )
            if (userRes.isSuccessful && userRes.body() != null) {
                val user = userRes.body()!!
                val avatar = extractAvatar(user.userMetadata)
                val name = extractName(user.userMetadata)

                authDataStore.edit { p ->
                    if (!avatar.isNullOrEmpty()) p[KEY_USER_AVATAR] = avatar
                    if (!name.isNullOrEmpty()) p[KEY_USER_NAME] = name
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "refreshUserProfile failed: ${e.message}")
            Result.failure(e)
        }
    }
}
