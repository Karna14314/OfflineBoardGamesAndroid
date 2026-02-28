package com.offlinegames.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinegames.persistence.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for [MainMenuScreen].
 * Exposes player name for greeting.
 */
class MainMenuViewModel(
    private val prefs: PreferencesRepository
) : ViewModel() {

    val playerOneName: StateFlow<String> = prefs.playerOneName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Player 1")
}

class MainMenuViewModelFactory(
    private val prefs: PreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainMenuViewModel(prefs) as T
    }
}
