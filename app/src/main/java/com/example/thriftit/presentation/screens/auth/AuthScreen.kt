package com.example.thriftit.presentation.screens.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thriftit.presentation.util.AuthUiState
import com.example.thriftit.presentation.viewmodel.AuthViewModel

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
                if (state.user?.displayName.isNullOrBlank()) {
                    onNavigateToProfile()
                } else {
                    onNavigateToHome()
                }
            }

            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }

            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AuthHeader()

                Spacer(modifier = Modifier.height(48.dp))

                when (authState) {
                    is AuthUiState.Idle, is AuthUiState.Error -> {
                        PhoneInputSection(
                            phoneNumber = phoneNumber,
                            onPhoneChange = {
                                viewModel.updatePhoneNumber(
                                    it.filter(Char::isDigit).take(PHONE_LENGTH),
                                )
                            },
                            onSendOtp = {
                                activity?.let { viewModel.sendOtp(it) }
                            },
                            isEnabled = phoneNumber.length == PHONE_LENGTH,
                        )
                    }

                    is AuthUiState.Loading -> LoadingState()

                    is AuthUiState.OtpSent -> {
                        OtpInputSection(
                            phoneNumber = phoneNumber,
                            otp = otp,
                            onOtpChange = {
                                viewModel.updateOtp(
                                    it.filter(Char::isDigit).take(OTP_LENGTH),
                                )
                            },
                            onVerifyOtp = { viewModel.verifyOtp() },
                            onChangeNumber = {
                                viewModel.resetState()
                                viewModel.updatePhoneNumber("")
                                viewModel.updateOtp("")
                            },
                            onResendOtp = {
                                activity?.let { viewModel.sendOtp(it) }
                            },
                            isEnabled = otp.length == OTP_LENGTH,
                        )
                    }

                    else -> Unit
                }

                Spacer(modifier = Modifier.height(32.dp))

                FooterNote()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
        )
    }
}

@Composable
private fun AuthHeader() {
    Text(
        text = "THRIFT IT",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Buy & Sell Near You",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please wait…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PhoneInputSection(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    isEnabled: Boolean,
) {
    Text(
        text = "Enter your phone number",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = phoneNumber,
        onValueChange = onPhoneChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Phone Number") },
        prefix = { Text("+91") },
        placeholder = { Text("9000000000") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onSendOtp,
        enabled = isEnabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
    ) {
        Text(
            text = "Send OTP",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun OtpInputSection(
    phoneNumber: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    onVerifyOtp: () -> Unit,
    onChangeNumber: () -> Unit,
    onResendOtp: () -> Unit,
    isEnabled: Boolean,
) {
    Text(
        text = "Enter OTP",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Sent to +91 $phoneNumber",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = otp,
        onValueChange = onOtpChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("OTP") },
        placeholder = { Text("123456") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onVerifyOtp,
        enabled = isEnabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
    ) {
        Text(
            text = "Verify OTP",
            style = MaterialTheme.typography.labelLarge,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    TextButton(onClick = onChangeNumber) {
        Text(
            text = "Change phone number",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    TextButton(onClick = onResendOtp) {
        Text(
            text = "Resend OTP",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FooterNote() {
    Text(
        text = "We’ll send you an OTP via SMS to verify your number",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Preview
@Composable
private fun AuthScreenPreview() {
    MaterialTheme {
        AuthScreen()
    }
}
