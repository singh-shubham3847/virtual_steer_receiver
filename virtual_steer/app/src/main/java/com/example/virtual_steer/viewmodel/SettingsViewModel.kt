package com.example.virtual_steer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.virtual_steer.model.*
import com.example.virtual_steer.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val config: StateFlow<ControllerConfig> = repository.configFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ControllerConfig()
        )

    fun updateSteering(transform: (SteeringConfig) -> SteeringConfig) {
        viewModelScope.launch {
            repository.updateSteering(transform)
        }
    }

    fun updatePedals(transform: (PedalConfig) -> PedalConfig) {
        viewModelScope.launch {
            repository.updatePedals(transform)
        }
    }

    fun updateNetwork(transform: (NetworkConfig) -> NetworkConfig) {
        viewModelScope.launch {
            repository.updateNetwork(transform)
        }
    }

    fun updateUI(transform: (UIConfig) -> UIConfig) {
        viewModelScope.launch {
            repository.updateUI(transform)
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            // Manual reset for now
            repository.updateSteering { SteeringConfig() }
            repository.updatePedals { PedalConfig() }
            repository.updateNetwork { NetworkConfig() }
            repository.updateUI { UIConfig() }
        }
    }
}
