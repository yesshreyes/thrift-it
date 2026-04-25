package com.example.thriftit.presentation.screens.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.thriftit.presentation.util.UiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    locationPermissionsState: MultiplePermissionsState,
    onDisplayNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onEnableLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileHeader()
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full Name") },
            placeholder = { Text("John Doe") },
            singleLine = true,
            isError = uiState.validationErrors.containsKey("displayName"),
            supportingText = { uiState.validationErrors["displayName"]?.let { Text(it, color = MaterialTheme.colorScheme.primary) } },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.currentUser?.phoneNumber.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone Number") },
            singleLine = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.location,
            onValueChange = onLocationChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Location / Address") },
            placeholder = { Text("City, State") },
            minLines = 2,
            maxLines = 3,
            isError = uiState.validationErrors.containsKey("location"),
            supportingText = { uiState.validationErrors["location"]?.let { Text(it, color = MaterialTheme.colorScheme.primary) } },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
        )
        Spacer(modifier = Modifier.height(24.dp))

        LocationPermissionCard(
            locationPermissionsState = locationPermissionsState,
            isLoadingLocation = uiState.isLoadingLocation,
            onEnableLocation = onEnableLocation,
            onOpenSettings = onOpenSettings,
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = uiState.profileState !is UiState.Loading,
        ) {
            if (uiState.profileState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(text = "Save Profile", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your information helps build trust in the community",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun ProfileHeader() {
    Text(text = "Complete Your Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Help buyers and sellers find you", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationPermissionCard(
    locationPermissionsState: MultiplePermissionsState,
    isLoadingLocation: Boolean,
    onEnableLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Location Access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onEnableLocation, modifier = Modifier.fillMaxWidth(), enabled = !isLoadingLocation) {
            if (isLoadingLocation) CircularProgressIndicator(modifier = Modifier.size(20.dp))
            else Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = when {
                isLoadingLocation -> "Getting location…"
                locationPermissionsState.allPermissionsGranted -> "Get current location"
                else -> "Enable location"
            })
        }
        if (!locationPermissionsState.allPermissionsGranted && !locationPermissionsState.shouldShowRationale) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Open App Settings", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    context.startActivity(intent)
}
