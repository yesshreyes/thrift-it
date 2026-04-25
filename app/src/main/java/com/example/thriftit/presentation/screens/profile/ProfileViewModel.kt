package com.example.thriftit.presentation.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thriftit.data.repository.AuthRepository
import com.example.thriftit.data.repository.UploadRepository
import com.example.thriftit.data.repository.UserRepository
import com.example.thriftit.domain.models.Coordinates
import com.example.thriftit.domain.models.User
import com.example.thriftit.domain.util.Result
import com.example.thriftit.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val uploadRepository: UploadRepository,
    ) : ViewModel() {

        // Internal — not part of UI state
        private var _profileImageUri: Uri? = null

        // Single source of truth for the screen
        private val _uiState = MutableStateFlow(ProfileUiState())
        val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

        init {
            loadCurrentUser()
        }

        // ── Load ─────────────────────────────────────────────────────────────

        private fun loadCurrentUser() {
            viewModelScope.launch {
                val result =
                    authRepository
                        .getCurrentUserProfile()
                        .first { it is Result.Success || it is Result.Error }

                if (result is Result.Success) {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            currentUser = user,
                            displayName = user?.displayName.orEmpty(),
                            location = user?.location.orEmpty(),
                            profileImageUrl = user?.profileImageUrl,
                        )
                    }
                    // If profile already complete, surface that
                    if (!user?.displayName.isNullOrBlank() && !user?.location.isNullOrBlank()) {
                        _uiState.update { it.copy(profileState = UiState.Success(Unit)) }
                    }
                }
            }
        }

        // ── Field updates ────────────────────────────────────────────────────

        fun updateDisplayName(value: String) {
            _uiState.update { it.copy(displayName = value) }
            validateDisplayName()
        }

        fun updateLocation(value: String) {
            _uiState.update { it.copy(location = value) }
            validateLocation()
        }

        fun updateProfileImage(uri: Uri) {
            _profileImageUri = uri
        }

        fun updateLocationPermission(granted: Boolean) {
            _uiState.update { it.copy(locationPermissionGranted = granted) }
        }

        fun updateIsLoadingLocation(loading: Boolean) {
            _uiState.update { it.copy(isLoadingLocation = loading) }
        }

        fun updateLocationFromCoordinates(latitude: Double, longitude: Double) {
            _uiState.update {
                it.copy(
                    coordinates = Coordinates(latitude, longitude),
                    validationErrors = it.validationErrors - "coordinates",
                )
            }
        }

        // ── Validation ───────────────────────────────────────────────────────

        private fun validateDisplayName() {
            val name = _uiState.value.displayName
            val error = when {
                name.isBlank() -> "Name is required"
                name.length < 2 -> "Name must be at least 2 characters"
                name.length > 50 -> "Name must be under 50 characters"
                else -> null
            }
            _uiState.update {
                it.copy(validationErrors = if (error != null) it.validationErrors + ("displayName" to error)
                        else it.validationErrors - "displayName")
            }
        }

        private fun validateLocation() {
            val error = if (_uiState.value.location.isBlank()) "Location is required" else null
            _uiState.update {
                it.copy(validationErrors = if (error != null) it.validationErrors + ("location" to error)
                        else it.validationErrors - "location")
            }
        }

        private fun validateForm(): Boolean {
            validateDisplayName()
            validateLocation()
            val state = _uiState.value
            val errors = state.validationErrors.toMutableMap()
            if (state.coordinates == null) errors["coordinates"] = "Please fetch current location"
            _uiState.update { it.copy(validationErrors = errors) }
            return errors.isEmpty()
        }

        // ── Save ─────────────────────────────────────────────────────────────

        fun saveProfile() {
            if (!validateForm()) {
                _uiState.update { it.copy(profileState = UiState.Error("Please fix validation errors")) }
                return
            }
            val uid = authRepository.getCurrentUserId()
            if (uid == null) {
                _uiState.update { it.copy(profileState = UiState.Error("User not authenticated")) }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(profileState = UiState.Loading) }
                val state = _uiState.value

                var imageUrl = state.profileImageUrl
                _profileImageUri?.let { uri ->
                    when (val res = uploadRepository.uploadProfileImage(uri, uid)) {
                        is Result.Success -> imageUrl = res.data
                        is Result.Error -> {
                            _uiState.update { it.copy(profileState = UiState.Error(res.message)) }
                            return@launch
                        }
                        is Result.Loading -> Unit
                    }
                }

                val updatedUser = User(
                    uid = uid,
                    phoneNumber = authRepository.getCurrentUserPhoneNumber(),
                    displayName = state.displayName.trim(),
                    profileImageUrl = imageUrl,
                    location = state.location.trim(),
                    coordinates = state.coordinates,
                    lastUpdated = System.currentTimeMillis(),
                )

                when (val res = userRepository.updateUserProfile(updatedUser)) {
                    is Result.Success -> {
                        _profileImageUri = null
                        _uiState.update {
                            it.copy(currentUser = updatedUser, profileImageUrl = imageUrl, profileState = UiState.Success(Unit))
                        }
                    }
                    is Result.Error -> _uiState.update { it.copy(profileState = UiState.Error(res.message)) }
                    is Result.Loading -> Unit
                }
            }
        }

        fun resetProfileState() {
            _uiState.update { it.copy(profileState = UiState.Idle) }
        }
    }
