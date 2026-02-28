package com.offlinegames.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.persistence.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the editable state for [SettingsScreen].
 */
class SettingsViewModel(
    private val prefs: PreferencesRepository
) : ViewModel() {

    val soundEnabled: StateFlow<Boolean> = prefs.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val hapticsEnabled: StateFlow<Boolean> = prefs.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val darkTheme: StateFlow<Boolean> = prefs.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val playerOneName: StateFlow<String> = prefs.playerOneName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Player 1")

    val playerTwoName: StateFlow<String> = prefs.playerTwoName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Player 2")

    val aiDifficulty: StateFlow<String> = prefs.aiDifficulty
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "MEDIUM")

    fun setSoundEnabled(v: Boolean) = viewModelScope.launch { prefs.setSoundEnabled(v) }
    fun setHapticsEnabled(v: Boolean) = viewModelScope.launch { prefs.setHapticsEnabled(v) }
    fun setDarkTheme(v: Boolean) = viewModelScope.launch { prefs.setDarkTheme(v) }
    fun setPlayerOneName(name: String) = viewModelScope.launch { prefs.setPlayerOneName(name) }
    fun setPlayerTwoName(name: String) = viewModelScope.launch { prefs.setPlayerTwoName(name) }
    fun setAiDifficulty(d: String) = viewModelScope.launch { prefs.setAiDifficulty(d) }
}

class SettingsViewModelFactory(
    private val prefs: PreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(prefs) as T
    }
}
