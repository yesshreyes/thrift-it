package com.example.thriftit.presentation.screens.auth

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private const val PHONE_LENGTH = 10
private const val OTP_LENGTH = 6

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
) {
    val authState by viewModel.authState.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val otp by viewModel.otp.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthUiState.Success -> {
                snackbarHostState.showSnackbar("Login successful!")
                if (state.user?.displayName.isNullOrBlank()) onNavigateToProfile() else onNavigateToHome()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AuthContent(
                authState = authState,
                phoneNumber = phoneNumber,
                otp = otp,
                onPhoneChange = { viewModel.updatePhoneNumber(it.filter(Char::isDigit).take(PHONE_LENGTH)) },
                onOtpChange = { viewModel.updateOtp(it.filter(Char::isDigit).take(OTP_LENGTH)) },
                onSendOtp = { activity?.let { viewModel.sendOtp(it) } },
                onVerifyOtp = { viewModel.verifyOtp() },
                onChangeNumber = { viewModel.resetState(); viewModel.updatePhoneNumber(""); viewModel.updateOtp("") },
                onResendOtp = { activity?.let { viewModel.sendOtp(it) } },
            )
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
