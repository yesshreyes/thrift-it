package com.example.thriftit.presentation.viewmodel

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
        // Whole screen state (loading / success / error while saving)
        private val _profileState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
        val profileState: StateFlow<UiState<Unit>> = _profileState.asStateFlow()

        // Existing user data
        private val _currentUser = MutableStateFlow<User?>(null)
        val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

        // Form fields
        private val _displayName = MutableStateFlow("")
        val displayName: StateFlow<String> = _displayName.asStateFlow()

        private val _location = MutableStateFlow("")
        val location: StateFlow<String> = _location.asStateFlow()

        private val _coordinates = MutableStateFlow<Coordinates?>(null)
        val coordinates: StateFlow<Coordinates?> = _coordinates.asStateFlow()

        private val _profileImageUri = MutableStateFlow<Uri?>(null)
        val profileImageUri: StateFlow<Uri?> = _profileImageUri.asStateFlow()

        private val _profileImageUrl = MutableStateFlow<String?>(null)
        val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

        // Validation errors (key = field name)
        private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
        val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

        init {
            loadCurrentUser()
        }

        private fun loadCurrentUser() {
            viewModelScope.launch {
                authRepository.getCurrentUserProfile().collect { result ->
                    when (result) {
                        is Result.Loading -> _profileState.value = UiState.Loading
                        is Result.Error -> _profileState.value = UiState.Error(result.message)
                        is Result.Success -> {
                            val user = result.data
                            _currentUser.value = user
                            _displayName.value = user?.displayName.orEmpty()
                            _location.value = user?.location.orEmpty()
                            _coordinates.value = user?.coordinates
                            _profileImageUrl.value = user?.profileImageUrl
                            _profileState.value = UiState.Idle
                        }
                    }
                }
            }
        }

        // Field updates
        fun updateDisplayName(value: String) {
            _displayName.value = value
            validateDisplayName()
        }

        fun updateLocation(value: String) {
            _location.value = value
            validateLocation()
        }

        fun updateCoordinates(
            lat: Double,
            lng: Double,
        ) {
            _coordinates.value = Coordinates(lat, lng)
        }

        fun updateProfileImage(uri: Uri) {
            _profileImageUri.value = uri
        }

        // Validation helpers
        private fun validateDisplayName() {
            val errors = _validationErrors.value.toMutableMap()
            when {
                _displayName.value.isBlank() -> errors["displayName"] = "Name is required"
                _displayName.value.length < 2 -> errors["displayName"] = "Name must be at least 2 characters"
                _displayName.value.length > 50 -> errors["displayName"] = "Name must be under 50 characters"
                else -> errors.remove("displayName")
            }
            _validationErrors.value = errors
        }

        private fun validateLocation() {
            val errors = _validationErrors.value.toMutableMap()
            if (_location.value.isBlank()) {
                errors["location"] = "Location is required"
            } else {
                errors.remove("location")
            }
            _validationErrors.value = errors
        }

        private fun validateForm(): Boolean {
            validateDisplayName()
            validateLocation()
            return _validationErrors.value.isEmpty()
        }

        fun saveProfile() {
            if (!validateForm()) {
                _profileState.value = UiState.Error("Please fix validation errors")
                return
            }

            val uid = authRepository.getCurrentUserId()
            if (uid == null) {
                _profileState.value = UiState.Error("User not authenticated")
                return
            }

            viewModelScope.launch {
                _profileState.value = UiState.Loading

                // Step 1: upload profile image if user picked a new one
                var imageUrl = _profileImageUrl.value
                _profileImageUri.value?.let { uri ->
                    when (val res = uploadRepository.uploadProfileImage(uri, uid)) {
                        is Result.Success -> imageUrl = res.data
                        is Result.Error -> {
                            _profileState.value = UiState.Error(res.message)
                            return@launch
                        }
                        is Result.Loading -> Unit
                    }
                }

                // Step 2: build updated User object
                val existing = _currentUser.value
                val updatedUser =
                    User(
                        uid = uid,
                        phoneNumber = existing?.phoneNumber.orEmpty(),
                        displayName = _displayName.value.trim(),
                        profileImageUrl = imageUrl,
                        location = _location.value.trim(),
                        coordinates = _coordinates.value,
                        lastUpdated = System.currentTimeMillis(),
                    )

                // Step 3: push to Firestore + Room via repository
                when (val res = userRepository.updateUserProfile(updatedUser)) {
                    is Result.Success -> {
                        _currentUser.value = updatedUser
                        _profileImageUrl.value = imageUrl
                        _profileImageUri.value = null
                        _profileState.value = UiState.Success(Unit)
                    }
                    is Result.Error -> _profileState.value = UiState.Error(res.message)
                    is Result.Loading -> Unit
                }
            }
        }

        fun resetProfileState() {
            _profileState.value = UiState.Idle
        }
    }
