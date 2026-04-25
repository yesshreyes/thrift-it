package com.example.thriftit.presentation.screens.auth

import com.example.thriftit.domain.models.User

sealed class AuthUiState {
    data object Idle : AuthUiState()

    data object Loading : AuthUiState()

    data class OtpSent(
        val verificationId: String,
    ) : AuthUiState()

    data class Success(
        val user: User?,
    ) : AuthUiState()

    data class Error(
        val message: String,
    ) : AuthUiState()
}
