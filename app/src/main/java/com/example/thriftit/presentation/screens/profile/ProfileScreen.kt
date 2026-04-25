package com.example.thriftit.presentation.screens.profile

import android.Manifest
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thriftit.presentation.util.LocationHelper
import com.example.thriftit.presentation.util.UiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    )

    LaunchedEffect(uiState.profileState) {
        when (val state = uiState.profileState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("Profile saved successfully")
                viewModel.resetProfileState()
                onNavigateToMain()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetProfileState()
            }
            else -> Unit
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
        ProfileContent(
            uiState = uiState,
            locationPermissionsState = locationPermissionsState,
            onDisplayNameChange = viewModel::updateDisplayName,
            onLocationChange = viewModel::updateLocation,
            onEnableLocation = {
                when {
                    locationPermissionsState.allPermissionsGranted -> {
                        scope.launch {
                            viewModel.updateIsLoadingLocation(true)
                            val coords = LocationHelper.getCurrentLocation(context)
                            coords?.let { (lat, lng) ->
                                viewModel.updateLocationFromCoordinates(lat, lng)
                                snackbarHostState.showSnackbar("Location captured")
                            } ?: snackbarHostState.showSnackbar("Unable to get location")
                            viewModel.updateIsLoadingLocation(false)
                        }
                    }
                    locationPermissionsState.shouldShowRationale -> {
                        scope.launch { snackbarHostState.showSnackbar("Location helps show nearby listings") }
                    }
                    else -> locationPermissionsState.launchMultiplePermissionRequest()
                }
            },
            onOpenSettings = { openAppSettings(context) },
            onSave = viewModel::saveProfile,
        )
    }
}
