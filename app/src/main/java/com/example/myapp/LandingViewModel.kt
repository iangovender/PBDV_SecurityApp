package com.example.myapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.IncidentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LandingViewModel(
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _recentIncidents = MutableStateFlow<List<IncidentUiModel>>(emptyList())
    val recentIncidents = _recentIncidents.asStateFlow()

    init {
        loadRecentIncidents()
    }

    private fun loadRecentIncidents() {
        viewModelScope.launch {
            incidentRepository.getIncidents()
                .map { incidents ->
                    incidents
                        .sortedByDescending { it.createdAt }
                        .take(5)
                        .map { it.toUiModel() }
                }
                .collect { _recentIncidents.value = it }
        }
    }

    fun refreshIncidents() {
        loadRecentIncidents()
    }
}