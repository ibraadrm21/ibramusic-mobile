package com.ibramusic.mobile.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.data.stats.UserStatsManager
import com.ibramusic.mobile.domain.model.UserStats
import com.ibramusic.mobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsManager: UserStatsManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    val stats: StateFlow<UserStats> = statsManager.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStats())

    val userAvatar: StateFlow<String?> = authRepository.currentUserAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userName: StateFlow<String?> = authRepository.currentUserName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

