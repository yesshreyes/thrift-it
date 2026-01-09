package com.example.thriftit.presentation.screens.profile

import android.Manifest
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thriftit.presentation.util.LocationHelper
import com.example.thriftit.presentation.util.UiState
import com.example.thriftit.presentation.viewmodel.ProfileViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProfileSetupScreen(
    onNavigateToMain: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ViewModel states
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val coordinates by viewModel.coordinates.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()

    // Location permission state
    val locationPermissionsState =
        rememberMultiplePermissionsState(
            permissions =
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
        )

    var isLoadingLocation by remember { mutableStateOf(false) }

    // Handle profile save success
    LaunchedEffect(profileState) {
        when (profileState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("Profile saved successfully!")
                viewModel.resetProfileState()
                onNavigateToMain()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    (profileState as UiState.Error).message ?: "Error saving profile",
                )
                viewModel.resetProfileState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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

                // Name Field
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { viewModel.updateDisplayName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full Name") },
                    placeholder = { Text("John Doe") },
                    singleLine = true,
                    isError = validationErrors.containsKey("displayName"),
                    supportingText = {
                        validationErrors["displayName"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number Field (Read-only)
                OutlinedTextField(
                    value = currentUser?.phoneNumber ?: "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone Number") },
                    singleLine = true,
                    enabled = false,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location Field
                OutlinedTextField(
                    value = location,
                    onValueChange = { viewModel.updateLocation(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location/Address") },
                    placeholder = { Text("City, State") },
                    minLines = 2,
                    maxLines = 3,
                    isError = validationErrors.containsKey("location"),
                    supportingText = {
                        validationErrors["location"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                LocationPermissionCard(
                    locationPermissionsState = locationPermissionsState,
                    isLoadingLocation = isLoadingLocation,
                    onEnableLocation = {
                        when {
                            locationPermissionsState.allPermissionsGranted -> {
                                scope.launch {
                                    // ✅ scope.launch needed
                                    isLoadingLocation = true
                                    val coords = LocationHelper.getCurrentLocation(context)
                                    coords?.let { (lat, lng) ->
                                        viewModel.updateLocationFromCoordinates(lat, lng)
                                        snackbarHostState.showSnackbar("Location coordinates captured!")
                                    } ?: run {
                                        snackbarHostState.showSnackbar("Unable to get location. Try GPS ON + Google Maps first")
                                    }
                                    isLoadingLocation = false
                                }
                            }
                            locationPermissionsState.shouldShowRationale -> {
                                scope.launch {
                                    // ✅ scope.launch needed
                                    snackbarHostState.showSnackbar("Location needed for nearby listings")
                                }
                            }
                            else -> {
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        }
                    },
                    onOpenSettings = { openAppSettings(context) },
                )

                validationErrors["coordinates"]?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = { viewModel.saveProfile() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    enabled = profileState !is UiState.Loading,
                ) {
                    if (profileState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = "Save Profile",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                InfoNote()
            }
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationPermissionCard(
    locationPermissionsState: MultiplePermissionsState, // ✅ Fixed param
    isLoadingLocation: Boolean,
    onEnableLocation: () -> Unit,
    onOpenSettings: () -> Unit, // ✅ Fixed param
    context: Context = LocalContext.current,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Location Access",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        // Debug info
        Text(
            text =
                buildString {
                    append("Permissions: ${locationPermissionsState.allPermissionsGranted}")
                    append(" | GPS OK: ${LocationHelper.hasLocationPermission(context)}")
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Main button
        OutlinedButton(
            onClick = onEnableLocation,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoadingLocation,
        ) {
            if (isLoadingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text =
                    when {
                        isLoadingLocation -> "Getting Location..."
                        locationPermissionsState.allPermissionsGranted -> "Get Current Location"
                        else -> "Enable Location"
                    },
            )
        }

        // Settings button (if permanently denied)
        if (!locationPermissionsState.allPermissionsGranted && !locationPermissionsState.shouldShowRationale) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open App Settings", color = MaterialTheme.colorScheme.error)
            }
        }
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

private fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    context.startActivity(intent)
}
