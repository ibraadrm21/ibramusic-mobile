package com.ibramusic.mobile.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.domain.model.PlaybackQueue
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.usecase.ManageQueueUseCase
import com.ibramusic.mobile.domain.usecase.PlayTrackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val manageQueueUseCase: ManageQueueUseCase,
    private val playTrackUseCase: PlayTrackUseCase
) : ViewModel() {

    val queueState: StateFlow<PlaybackQueue> = manageQueueUseCase.queue

    fun removeFromQueue(queueEntryId: String) {
        viewModelScope.launch {
            manageQueueUseCase.removeFromQueue(queueEntryId)
        }
    }

    fun reorderUserQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            manageQueueUseCase.reorderUserQueue(fromIndex, toIndex)
        }
    }

    fun reorderPlaylistQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            manageQueueUseCase.reorderPlaylistQueue(fromIndex, toIndex)
        }
    }

    fun clearUserQueue() {
        viewModelScope.launch {
            manageQueueUseCase.clearUserQueue()
        }
    }

    fun playTrack(track: Track) {
        playTrackUseCase(track)
    }
}

