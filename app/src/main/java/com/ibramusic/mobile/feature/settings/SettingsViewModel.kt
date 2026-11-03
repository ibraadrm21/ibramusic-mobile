package com.ibramusic.mobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.data.repository.AuthRepositoryImpl
import com.ibramusic.mobile.domain.repository.AuthRepository
import com.ibramusic.mobile.domain.repository.SettingsRepository
import com.ibramusic.mobile.playback.audiofx.EqualizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    val equalizerManager: EqualizerManager,
    private val offlineModeManager: com.ibramusic.mobile.core.common.OfflineModeManager
) : ViewModel() {

    val isManualOfflineMode: StateFlow<Boolean> = offlineModeManager.isManualOfflineMode
    val isOnline: StateFlow<Boolean> = offlineModeManager.isOnline
    val isOfflineMode: StateFlow<Boolean> = offlineModeManager.isOfflineMode

    fun setManualOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            offlineModeManager.setManualOfflineMode(enabled)
        }
    }

    val audioQuality: StateFlow<String> = settingsRepository.audioQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "high")

    val crossfadeEnabled: StateFlow<Boolean> = settingsRepository.crossfadeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val crossfadeDuration: StateFlow<Int> = settingsRepository.crossfadeDurationSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val theme: StateFlow<String> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    val isGuest: StateFlow<Boolean> = authRepository.isGuestMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val userEmail: StateFlow<String?> = authRepository.currentUserEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userAvatar: StateFlow<String?> = authRepository.currentUserAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userName: StateFlow<String?> = authRepository.currentUserName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoadingAuth = MutableStateFlow(false)
    val isLoadingAuth: StateFlow<Boolean> = _isLoadingAuth.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoadingAuth.value = true
            _authError.value = null
            val result = authRepository.signIn(email, password)
            _isLoadingAuth.value = false
            if (result.isFailure) {
                _authError.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoadingAuth.value = true
            _authError.value = null
            val result = authRepository.signUp(email, password)
            _isLoadingAuth.value = false
            if (result.isFailure) {
                _authError.value = result.exceptionOrNull()?.message ?: "Registration failed"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _syncStatus.value = "Sincronizando con Supabase..."
            val result = authRepository.syncWithCloud()
            if (result.isSuccess) {
                _syncStatus.value = "¡Sincronización con IbraMusic Web completada!"
            } else {
                _syncStatus.value = "Error al sincronizar: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun setAudioQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.setAudioQuality(quality)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDuration(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setCrossfadeDuration(seconds)
        }
    }

    fun setTheme(newTheme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(newTheme)
        }
    }
}
