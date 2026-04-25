package com.example.thriftit.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thriftit.data.repository.AuthRepository
import com.example.thriftit.data.repository.UserRepository
import com.example.thriftit.domain.models.User
import com.example.thriftit.domain.util.Result
import com.example.thriftit.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _userState = MutableStateFlow<UiState<User?>>(UiState.Loading)
        val userState: StateFlow<UiState<User?>> = _userState.asStateFlow()

        private val _signOutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
        val signOutState: StateFlow<UiState<Unit>> = _signOutState.asStateFlow()

        private val _deleteAccountState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
        val deleteAccountState: StateFlow<UiState<Unit>> = _deleteAccountState.asStateFlow()

        init {
            observeCurrentUser()
        }

        private fun observeCurrentUser() {
            viewModelScope.launch {
                authRepository.getCurrentUserProfile().collect { result ->
                    _userState.value = when (result) {
                        is Result.Loading -> UiState.Loading
                        is Result.Success -> UiState.Success(result.data)
                        is Result.Error -> UiState.Error(result.message)
                    }
                }
            }
        }

        fun signOut() {
            viewModelScope.launch {
                _signOutState.value = UiState.Loading
                when (val res = authRepository.signOut()) {
                    is Result.Success -> _signOutState.value = UiState.Success(Unit)
                    is Result.Error -> _signOutState.value = UiState.Error(res.message)
                    else -> Unit
                }
            }
        }

        fun resetSignOutState() {
            _signOutState.value = UiState.Idle
        }

        fun deleteAccount() {
            viewModelScope.launch {
                _deleteAccountState.value = UiState.Loading
                when (val res = authRepository.deleteAccount()) {
                    is Result.Success -> _deleteAccountState.value = UiState.Success(Unit)
                    is Result.Error -> _deleteAccountState.value = UiState.Error(res.message)
                    is Result.Loading -> Unit
                }
            }
        }

        fun resetDeleteAccountState() {
            _deleteAccountState.value = UiState.Idle
        }
    }
