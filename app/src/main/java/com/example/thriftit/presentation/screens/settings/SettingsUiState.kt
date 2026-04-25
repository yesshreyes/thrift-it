package com.example.thriftit.presentation.screens.settings

import com.example.thriftit.domain.models.User
import com.example.thriftit.presentation.util.UiState

data class SettingsUiState(
    val userState: UiState<User?> = UiState.Loading,
    val signOutState: UiState<Unit> = UiState.Idle,
    val deleteAccountState: UiState<Unit> = UiState.Idle,
)
