package com.appcloner.ui.clones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcloner.data.CloneManager
import com.appcloner.data.CloneRepository
import com.appcloner.data.model.CloneInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the clones list screen. */
data class ClonesUiState(
    val clones: List<CloneInfo> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * One-shot messages surfaced to the UI (e.g. Snackbars). These are delivered exactly once
 * via a [Channel] so they are not re-shown on configuration changes.
 */
sealed interface ClonesEvent {
    data object LaunchStarted : ClonesEvent
    data class LaunchFailed(val message: String?) : ClonesEvent
    data object CloneDeleted : ClonesEvent
    data object CloneRenamed : ClonesEvent
}

/**
 * Backs [ClonesFragment]: streams all persisted clones and mediates launch/delete/rename.
 */
@HiltViewModel
class ClonesViewModel @Inject constructor(
    private val cloneRepository: CloneRepository,
    private val cloneManager: CloneManager
) : ViewModel() {

    val uiState: StateFlow<ClonesUiState> = cloneRepository.allClones
        .map { ClonesUiState(clones = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ClonesUiState()
        )

    private val _events = Channel<ClonesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Launches the given clone through [CloneManager] and reports the outcome. */
    fun launchClone(clone: CloneInfo) {
        viewModelScope.launch {
            val result = cloneManager.launchClone(clone)
            if (result.isSuccess) {
                cloneRepository.updateClone(clone.copy(lastUsed = System.currentTimeMillis()))
                _events.send(ClonesEvent.LaunchStarted)
            } else {
                _events.send(ClonesEvent.LaunchFailed(result.exceptionOrNull()?.message))
            }
        }
    }

    /** Permanently removes a clone. */
    fun deleteClone(clone: CloneInfo) {
        viewModelScope.launch {
            cloneRepository.removeClone(clone)
            _events.send(ClonesEvent.CloneDeleted)
        }
    }

    /** Renames a clone's user-visible label. */
    fun renameClone(clone: CloneInfo, newLabel: String) {
        val label = newLabel.trim()
        if (label.isEmpty() || label == clone.cloneLabel) return
        viewModelScope.launch {
            cloneRepository.updateClone(clone.copy(cloneLabel = label))
            _events.send(ClonesEvent.CloneRenamed)
        }
    }
}
