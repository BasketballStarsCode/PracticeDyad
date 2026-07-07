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
class AuthViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean get() = repo.currentUserId.isNotEmpty()

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    init {
        if (isLoggedIn) loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            repo.getUserFlow(repo.currentUserId).collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun register(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        orgCode: String = ""
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val uid = repo.register(email, password)
                val code = repo.generateConnectCode()
                var orgId = ""

                if (orgCode.isNotBlank()) {
                    val org = repo.getOrgByCode(orgCode)
                    if (org != null) {
                        orgId = org.id
                        repo.saveOrganization(
                            org.copy(coachIds = org.coachIds + uid)
                        )
                    }
                }

                val user = AppUser(
                    id = uid,
                    email = email,
                    name = name,
                    role = role,
                    connectCode = code,
                    organizationId = orgId
                )
                repo.createUser(user)
                _currentUser.value = user
                _uiState.update { it.copy(loading = false, success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                repo.login(email, password)
                loadCurrentUser()
                _uiState.update { it.copy(loading = false, success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun logout() {
        repo.logout()
        _currentUser.value = null
        _uiState.update { AuthUiState() }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearSuccess() = _uiState.update { it.copy(success = false) }
}

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
