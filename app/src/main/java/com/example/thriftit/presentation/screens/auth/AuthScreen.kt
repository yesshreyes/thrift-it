package com.example.thriftit.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val PHONE_LENGTH = 10
private const val OTP_LENGTH = 6

@Composable
fun AuthScreen(onNavigateToProfile: () -> Unit = {}) {
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var isOtpSent by rememberSaveable { mutableStateOf(false) }

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

            if (!isOtpSent) {
                PhoneInputSection(
                    phoneNumber = phoneNumber,
                    onPhoneChange = { input ->
                        phoneNumber = input.filter(Char::isDigit).take(PHONE_LENGTH)
                    },
                    onSendOtp = { isOtpSent = true },
                    isEnabled = phoneNumber.length == PHONE_LENGTH,
                )
            } else {
                OtpInputSection(
                    phoneNumber = phoneNumber,
                    otp = otp,
                    onOtpChange = { input ->
                        otp = input.filter(Char::isDigit).take(OTP_LENGTH)
                    },
                    onVerifyOtp = {
                        onNavigateToProfile()
                    },
                    onChangeNumber = {
                        otp = ""
                        isOtpSent = false
                    },
                    onResendOtp = {},
                    isEnabled = otp.length == OTP_LENGTH,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            FooterNote()
        }
    }
}

@Composable
private fun AuthHeader() {
    Text(
        text = "THRIFT IT",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Buy & Sell Near You",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = phoneNumber,
        onValueChange = onPhoneChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Phone Number") },
        placeholder = { Text("9000000000") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onSendOtp,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
        enabled = isEnabled,
    ) {
        Text(
            text = "Send OTP",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Sent to $phoneNumber",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onVerifyOtp,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
        enabled = isEnabled,
    ) {
        Text(
            text = "Verify OTP",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = onChangeNumber) {
        Text("Change Phone Number")
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(onClick = onResendOtp) {
        Text("Resend OTP")
    }
}

@Composable
private fun FooterNote() {
    Text(
        text = "We'll send you an OTP via SMS to verify your number",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AuthScreenPreview() {
    MaterialTheme {
        AuthScreen()
    }
}
