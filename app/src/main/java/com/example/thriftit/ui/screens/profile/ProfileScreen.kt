package com.example.thriftit.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

private const val NAME_MAX_LENGTH = 50
private const val ADDRESS_MAX_LENGTH = 200

@Composable
fun ProfileSetupScreen(
    onNavigateToMain: () -> Unit = {}
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var locationEnabled by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileHeader()

            Spacer(modifier = Modifier.height(32.dp))

            ProfileForm(
                name = name,
                phoneNumber = phoneNumber,
                address = address,
                city = city,
                locationEnabled = locationEnabled,
                onNameChange = { input ->
                    name = input.take(NAME_MAX_LENGTH)
                },
                onPhoneChange = { input ->
                    phoneNumber = input.filter(Char::isDigit).take(10)
                },
                onAddressChange = { input ->
                    address = input.take(ADDRESS_MAX_LENGTH)
                },
                onCityChange = { city = it },
                onEnableLocation = { locationEnabled = !locationEnabled },
            )

            Spacer(modifier = Modifier.height(32.dp))

            SaveButton(
//                enabled =
//                    name.isNotBlank() &&
//                        phoneNumber.length == 10 &&
//                        address.isNotBlank() &&
//                        city.isNotBlank() &&
//                        locationEnabled,
                onClick = {
                    onNavigateToMain()
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            InfoNote()
        }
    }
}

@Composable
private fun ProfileHeader() {
    Text(
        text = "Complete Your Profile",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Help buyers and sellers find you",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ProfileForm(
    name: String,
    phoneNumber: String,
    address: String,
    city: String,
    locationEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onEnableLocation: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Name Field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full Name") },
            placeholder = { Text("John Doe") },
            singleLine = true,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
        )

        // Phone Number Field (Read-only/Pre-filled)
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone Number") },
            placeholder = { Text("9000000000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            enabled = false,
            colors =
                OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )

        // Address Field
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Address") },
            placeholder = { Text("Street, Area, Landmark") },
            minLines = 2,
            maxLines = 3,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
        )

        // City Field
        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("City") },
            placeholder = { Text("Mumbai") },
            singleLine = true,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
        )

        // Location Permission Button
        LocationPermissionCard(
            locationEnabled = locationEnabled,
            onEnableLocation = onEnableLocation,
        )
    }
}

@Composable
private fun LocationPermissionCard(
    locationEnabled: Boolean,
    onEnableLocation: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Location Access",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enable location to show items near you and help others find your listings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onEnableLocation,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint =
                    if (locationEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = if (locationEnabled) "Location Enabled ✓" else "Enable Location",
                color =
                    if (locationEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

@Composable
private fun SaveButton(
//    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
//        enabled = enabled,
    ) {
        Text(
            text = "Save Profile",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun InfoNote() {
    Text(
        text = "Your information helps build trust in the community",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileSetupScreenPreview() {
    MaterialTheme {
        ProfileSetupScreen()
    }
}
