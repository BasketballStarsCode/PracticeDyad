package com.practicedyad.app.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicedyad.app.data.model.*
import com.practicedyad.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: AppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.GERMAN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _weightUnit = MutableStateFlow(WeightUnit.KG)
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    private val _user = MutableStateFlow<AppUser?>(null)
    val user: StateFlow<AppUser?> = _user.asStateFlow()

    private val _organization = MutableStateFlow<Organization?>(null)
    val organization: StateFlow<Organization?> = _organization.asStateFlow()

    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val WEIGHT_UNIT_KEY = stringPreferencesKey("weight_unit")

    init {
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                _darkMode.value = prefs[DARK_MODE_KEY] ?: false
                _language.value = AppLanguage.valueOf(prefs[LANGUAGE_KEY] ?: "GERMAN")
                _weightUnit.value = WeightUnit.valueOf(prefs[WEIGHT_UNIT_KEY] ?: "KG")
            }
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            repo.getUserFlow(repo.currentUserId).collect { u ->
                _user.value = u
                val orgId = u?.organizationId ?: ""
                if (orgId.isNotEmpty()) {
                    repo.getOrganizationFlow(orgId).collect { _organization.value = it }
                }
            }
        }
    }

    fun saveOrganization(org: Organization) {
        viewModelScope.launch {
            repo.saveOrganization(org)
            _organization.value = org
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
            _darkMode.value = enabled
        }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            context.dataStore.edit { it[LANGUAGE_KEY] = lang.name }
            _language.value = lang
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            context.dataStore.edit { it[WEIGHT_UNIT_KEY] = unit.name }
            _weightUnit.value = unit
            repo.updateUser(repo.currentUserId, mapOf("weightUnit" to unit.name))
        }
    }

    fun updateNotifications(
        notifyNewPlan: Boolean? = null,
        notifyWorkout: Boolean? = null,
        notifyWorkoutTime: String? = null,
        notifyMessages: Boolean? = null,
        notifyCompletedWorkouts: Boolean? = null
    ) {
        viewModelScope.launch {
            val updates = buildMap {
                notifyNewPlan?.let { put("notifyNewPlan", it) }
                notifyWorkout?.let { put("notifyWorkout", it) }
                notifyWorkoutTime?.let { put("notifyWorkoutTime", it) }
                notifyMessages?.let { put("notifyMessages", it) }
                notifyCompletedWorkouts?.let { put("notifyCompletedWorkouts", it) }
            }
            if (updates.isNotEmpty()) repo.updateUser(repo.currentUserId, updates)
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            repo.updateUser(repo.currentUserId, mapOf("name" to name))
        }
    }

    fun updateShareProgress(enabled: Boolean) {
        viewModelScope.launch {
            repo.updateUser(repo.currentUserId, mapOf("shareProgressWithCoach" to enabled))
        }
    }

    fun updateRole(role: UserRole) {
        viewModelScope.launch {
            repo.updateUser(repo.currentUserId, mapOf("role" to role.name))
            _user.value = _user.value?.copy(role = role)
        }
    }

    fun logout(authViewModel: AuthViewModel) {
        authViewModel.logout()
    }
}
