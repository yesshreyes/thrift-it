package com.example.thriftit.presentation.screens.profile

import com.example.thriftit.domain.models.Coordinates
import com.example.thriftit.domain.models.User
import com.example.thriftit.presentation.util.UiState

data class ProfileUiState(
    val displayName: String = "",
    val location: String = "",
    val coordinates: Coordinates? = null,
    val currentUser: User? = null,
    val profileImageUrl: String? = null,
    val profileState: UiState<Unit> = UiState.Idle,
    val validationErrors: Map<String, String> = emptyMap(),
    val isLoadingLocation: Boolean = false,
    val locationPermissionGranted: Boolean = false,
)
