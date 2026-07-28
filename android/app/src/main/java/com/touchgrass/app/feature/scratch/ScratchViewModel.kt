package com.touchgrass.app.feature.scratch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchgrass.app.core.data.db.ScratchDao
import com.touchgrass.app.core.data.db.ScratchNote
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phase 0 scaffolding ViewModel. Its only job is to prove the plumbing works:
 * Hilt injects it, Room persists the count, DataStore persists the flag,
 * and both survive an app restart.
 *
 * Delete this whole `feature/scratch` package in Phase 1.
 */
data class ScratchUiState(
    val noteCount: Int = 0,
    val setupComplete: Boolean = false
)

@HiltViewModel
class ScratchViewModel @Inject constructor(
    private val scratchDao: ScratchDao,
    private val settings: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<ScratchUiState> =
        combine(
            scratchDao.observeCount(),
            settings.setupComplete
        ) { count, complete ->
            ScratchUiState(noteCount = count, setupComplete = complete)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScratchUiState()
        )

    fun addNote() = viewModelScope.launch {
        scratchDao.insert(ScratchNote(text = "note"))
    }

    fun clearNotes() = viewModelScope.launch {
        scratchDao.clear()
    }

    fun toggleSetupComplete() = viewModelScope.launch {
        settings.setSetupComplete(!uiState.value.setupComplete)
    }
}
