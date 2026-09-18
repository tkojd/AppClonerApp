package com.appcloner.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcloner.data.AppRepository
import com.appcloner.data.CloneManager
import com.appcloner.data.CloneRepository
import com.appcloner.data.model.AppInfo
import com.appcloner.data.model.CloneInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the clone-detail screen. */
data class CloneDetailUiState(
    val sourcePackageName: String,
    val appName: String,
    val appInfo: AppInfo? = null,
    val existingClones: List<CloneInfo> = emptyList(),
    val isManagedProfileSupported: Boolean = false
)

/** One-shot events for the clone-detail screen. */
sealed interface CloneDetailEvent {
    data object CloneCreated : CloneDetailEvent
    data class CreateFailed(val message: String?) : CloneDetailEvent
}

/**
 * Backs [CloneDetailFragment]. Reads the source package + app name navigation arguments
 * from [SavedStateHandle] and orchestrates creation of new clones.
 */
@HiltViewModel
class CloneDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appRepository: AppRepository,
    private val cloneRepository: CloneRepository,
    private val cloneManager: CloneManager
) : ViewModel() {

    private val sourcePackageName: String =
        savedStateHandle.get<String>(ARG_SOURCE_PACKAGE).orEmpty()
    private val appName: String =
        savedStateHandle.get<String>(ARG_APP_NAME).orEmpty()

    private val _uiState = MutableStateFlow(
        CloneDetailUiState(
            sourcePackageName = sourcePackageName,
            appName = appName,
            isManagedProfileSupported = cloneManager.isManagedProfileSupported()
        )
    )
    val uiState: StateFlow<CloneDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<CloneDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Reactive list of clones already created for this source package. */
    val existingClones: StateFlow<List<CloneInfo>> =
        cloneRepository.clonesForPackage(sourcePackageName)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    init {
        loadAppInfo()
    }

    private fun loadAppInfo() {
        viewModelScope.launch {
            val info = appRepository.getAppInfo(sourcePackageName)
            _uiState.value = _uiState.value.copy(appInfo = info)
        }
    }

    /**
     * Creates a new clone. When [label] is blank a default label is generated using the
     * next clone index. Emits [CloneDetailEvent.CloneCreated] on success.
     */
    fun createClone(label: String, defaultLabelFactory: (Int) -> String) {
        viewModelScope.launch {
            try {
                val index = cloneRepository.nextCloneIndex(sourcePackageName)
                val now = System.currentTimeMillis()
                val clone = CloneInfo(
                    sourcePackageName = sourcePackageName,
                    cloneLabel = label.trim().ifBlank { defaultLabelFactory(index) },
                    cloneIndex = index,
                    createdAt = now,
                    lastUsed = now,
                    isActive = true,
                    profileId = -1
                )
                cloneRepository.addClone(clone)
                _events.send(CloneDetailEvent.CloneCreated)
            } catch (e: Exception) {
                _events.send(CloneDetailEvent.CreateFailed(e.message))
            }
        }
    }

    companion object {
        const val ARG_SOURCE_PACKAGE = "sourcePackageName"
        const val ARG_APP_NAME = "appName"
    }
}
