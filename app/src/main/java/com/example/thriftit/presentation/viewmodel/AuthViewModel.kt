package com.example.thriftit.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thriftit.data.repository.AuthRepository
import com.example.thriftit.domain.models.User
import com.example.thriftit.domain.util.Result
import com.example.thriftit.presentation.util.AuthUiState
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        // Use your existing AuthUiState
        private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
        val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

        private val _phoneNumber = MutableStateFlow("")
        val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

        private val _otp = MutableStateFlow("")
        val otp: StateFlow<String> = _otp.asStateFlow()

        private var verificationId: String? = null

        private val _currentUser = MutableStateFlow<User?>(null)
        val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

        init {
            checkAuthStatus()
        }

        // Check if user is already logged in
        private fun checkAuthStatus() {
            viewModelScope.launch {
                if (authRepository.isUserLoggedIn()) {
                    authRepository.getCurrentUserProfile().collect { result ->
                        when (result) {
                            is Result.Success -> {
                                _currentUser.value = result.data
                                _authState.value = AuthUiState.Success(result.data)
                            }
                            is Result.Error -> {
                                _authState.value = AuthUiState.Error(result.message)
                            }
                            is Result.Loading -> { /* Loading state */ }
                        }
                    }
                }
            }
        }

        // Update phone number
        fun updatePhoneNumber(number: String) {
            _phoneNumber.value = number
        }

        // Update OTP
        fun updateOtp(code: String) {
            _otp.value = code
        }

        // Send OTP to phone number
        fun sendOtp(activity: Activity) {
            val phone = "+91${_phoneNumber.value}" // Add country code

            if (!isValidPhoneNumber(phone)) {
                _authState.value = AuthUiState.Error("Invalid phone number")
                return
            }

            _authState.value = AuthUiState.Loading

            viewModelScope.launch {
                val callbacks =
                    object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            signInWithCredential(credential)
                        }

                        override fun onVerificationFailed(exception: com.google.firebase.FirebaseException) {
                            _authState.value = AuthUiState.Error(exception.message ?: "Verification failed")
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken,
                        ) {
                            this@AuthViewModel.verificationId = verificationId
                            _authState.value = AuthUiState.OtpSent(verificationId)
                        }
                    }

                authRepository.sendVerificationCode(phone, activity, callbacks).collect { result ->
                    when (result) {
                        is Result.Loading -> { /* Already loading */ }
                        is Result.Success -> {
                            if (result.data == "auto_verified") {
                                // Auto verification successful
                            }
                        }
                        is Result.Error -> {
                            _authState.value = AuthUiState.Error(result.message)
                        }
                    }
                }
            }
        }

        // Verify OTP
        fun verifyOtp() {
            val code = _otp.value
            val verificationId = this.verificationId

            if (code.length != 6) {
                _authState.value = AuthUiState.Error("Invalid OTP")
                return
            }

            if (verificationId == null) {
                _authState.value = AuthUiState.Error("Verification ID is null")
                return
            }

            _authState.value = AuthUiState.Loading

            viewModelScope.launch {
                val result = authRepository.verifyOtpAndSignIn(verificationId, code)
                when (result) {
                    is Result.Success -> {
                        _currentUser.value = result.data
                        _authState.value = AuthUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        _authState.value = AuthUiState.Error(result.message)
                    }
                    is Result.Loading -> { /* Already loading */ }
                }
            }
        }

        // Sign in with credential (auto-verification)
        private fun signInWithCredential(credential: PhoneAuthCredential) {
            viewModelScope.launch {
                val result = authRepository.signInWithCredential(credential)
                when (result) {
                    is Result.Success -> {
                        _currentUser.value = result.data
                        _authState.value = AuthUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        _authState.value = AuthUiState.Error(result.message)
                    }
                    is Result.Loading -> { /* Loading */ }
                }
            }
        }

        // Validate phone number
        private fun isValidPhoneNumber(phone: String): Boolean = phone.isNotEmpty() && phone.matches(Regex("^\\+?[1-9]\\d{1,14}$"))

        // Reset state
        fun resetState() {
            _authState.value = AuthUiState.Idle
            _otp.value = ""
        }
    }
