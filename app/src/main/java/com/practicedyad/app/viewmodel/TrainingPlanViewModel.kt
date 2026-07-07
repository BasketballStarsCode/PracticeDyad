package com.practicedyad.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicedyad.app.data.model.*
import com.practicedyad.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TrainingPlanViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _plans = MutableStateFlow<List<TrainingPlan>>(emptyList())
    val plans: StateFlow<List<TrainingPlan>> = _plans.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _athletes = MutableStateFlow<List<AppUser>>(emptyList())
    val athletes: StateFlow<List<AppUser>> = _athletes.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    fun loadCoachPlans() {
        viewModelScope.launch {
            repo.getCoachPlans(repo.currentUserId).collect { _plans.value = it }
        }
    }

    fun loadAthletePlans() {
        viewModelScope.launch {
            repo.getAthletePlans(repo.currentUserId).collect { _plans.value = it }
        }
    }

    fun loadAthletes() {
        viewModelScope.launch {
            repo.getAthletes(repo.currentUserId).collect { _athletes.value = it }
        }
    }

    fun loadTeams() {
        viewModelScope.launch {
            repo.getTeams(repo.currentUserId).collect { _teams.value = it }
        }
    }

    fun savePlan(plan: TrainingPlan, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val id = repo.saveTrainingPlan(plan)
                onDone(id)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            try { repo.deleteTrainingPlan(planId) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun sharePlan(planId: String, athleteIds: List<String>, teamIds: List<String>) {
        viewModelScope.launch {
            try { repo.sharePlanWithAthletes(planId, athleteIds, teamIds) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun endPlan(planId: String) {
        viewModelScope.launch {
            try { repo.sharePlanWithAthletes(planId, emptyList(), emptyList()) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun clearError() { _error.value = null }
}
