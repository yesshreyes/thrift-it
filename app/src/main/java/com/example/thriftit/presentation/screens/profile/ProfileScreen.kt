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

    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()

    val locationPermissionsState =
        rememberMultiplePermissionsState(
            permissions =
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
        )

    var isLoadingLocation by remember { mutableStateOf(false) }

    LaunchedEffect(profileState) {
        when (profileState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("Profile saved successfully")
                viewModel.resetProfileState()
                onNavigateToMain()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    (profileState as UiState.Error).message ?: "Error saving profile",
                )
                viewModel.resetProfileState()
            }
            else -> Unit
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

                // Name
                OutlinedTextField(
                    value = displayName,
                    onValueChange = viewModel::updateDisplayName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full Name") },
                    placeholder = { Text("John Doe") },
                    singleLine = true,
                    isError = validationErrors.containsKey("displayName"),
                    supportingText = {
                        validationErrors["displayName"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor =
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone (read only)
                OutlinedTextField(
                    value = currentUser?.phoneNumber.orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone Number") },
                    singleLine = true,
                    enabled = false,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            disabledBorderColor =
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor =
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = viewModel::updateLocation,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location / Address") },
                    placeholder = { Text("City, State") },
                    minLines = 2,
                    maxLines = 3,
                    isError = validationErrors.containsKey("location"),
                    supportingText = {
                        validationErrors["location"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor =
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
                                    isLoadingLocation = true
                                    val coords =
                                        LocationHelper.getCurrentLocation(context)
                                    coords?.let { (lat, lng) ->
                                        viewModel.updateLocationFromCoordinates(lat, lng)
                                        snackbarHostState.showSnackbar("Location captured")
                                    } ?: snackbarHostState.showSnackbar(
                                        "Unable to get location",
                                    )
                                    isLoadingLocation = false
                                }
                            }
                            locationPermissionsState.shouldShowRationale -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Location helps show nearby listings",
                                    )
                                }
                            }
                            else -> {
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        }
                    },
                    onOpenSettings = { openAppSettings(context) },
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = viewModel::saveProfile,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    enabled = profileState !is UiState.Loading,
                ) {
                    if (profileState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = "Save Profile",
                            style = MaterialTheme.typography.labelLarge,
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
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationPermissionCard(
    locationPermissionsState: MultiplePermissionsState,
    isLoadingLocation: Boolean,
    onEnableLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    context: Context = LocalContext.current,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Location Access",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                        isLoadingLocation -> "Getting location…"
                        locationPermissionsState.allPermissionsGranted -> "Get current location"
                        else -> "Enable location"
                    },
            )
        }

        if (!locationPermissionsState.allPermissionsGranted &&
            !locationPermissionsState.shouldShowRationale
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Open App Settings",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InfoNote() {
    Text(
        text = "Your information helps build trust in the community",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
