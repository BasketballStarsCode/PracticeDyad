package com.practicedyad.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicedyad.app.data.model.*
import com.practicedyad.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AthleteViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    val currentUserId: String get() = repo.currentUserId

    private val _athletes = MutableStateFlow<List<AppUser>>(emptyList())
    val athletes: StateFlow<List<AppUser>> = _athletes.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _selectedAthlete = MutableStateFlow<AppUser?>(null)
    val selectedAthlete: StateFlow<AppUser?> = _selectedAthlete.asStateFlow()

    private val _connectCode = MutableStateFlow("")
    val connectCode: StateFlow<String> = _connectCode.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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

    fun loadCurrentUserCode() {
        viewModelScope.launch {
            val user = repo.getUser(repo.currentUserId)
            _connectCode.value = user?.connectCode ?: ""
        }
    }

    fun loadAthlete(athleteId: String) {
        viewModelScope.launch {
            _selectedAthlete.value = repo.getUser(athleteId)
        }
    }

    fun saveTeam(team: Team, onDone: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repo.saveTeam(team)
                onDone()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch {
            try { repo.deleteTeam(teamId) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun connectWithCode(code: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val coach = repo.getUserByCode(code)
                if (coach != null) {
                    repo.connectAthleteToCoach(repo.currentUserId, coach.id)
                    onResult(true)
                } else {
                    _error.value = "Ungültiger Code"
                    onResult(false)
                }
            } catch (e: Exception) {
                _error.value = e.message
                onResult(false)
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
