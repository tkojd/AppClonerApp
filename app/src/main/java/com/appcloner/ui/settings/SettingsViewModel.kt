package com.appcloner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcloner.data.CloneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot events for the settings screen. */
sealed interface SettingsEvent {
    data object AllClonesCleared : SettingsEvent
}

/**
 * Backs [SettingsFragment]. Owns the "clear all clones" action which deletes every
 * persisted clone via [CloneRepository].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val cloneRepository: CloneRepository
) : ViewModel() {

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Removes all persisted clone configurations. */
    fun clearAllClones() {
        viewModelScope.launch {
            cloneRepository.removeAll()
            _events.send(SettingsEvent.AllClonesCleared)
        }
    }
}
