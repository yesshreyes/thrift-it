package com.example.thriftit.presentation.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thriftit.presentation.util.UiState

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToAuth: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onUpdateLocation: () -> Unit = {},
) {
    val userState by viewModel.userState.collectAsState()
    val signOutState by viewModel.signOutState.collectAsState()
    val deleteAccountState by viewModel.deleteAccountState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(signOutState) {
        when (signOutState) {
            is UiState.Success -> { viewModel.resetSignOutState(); onNavigateToAuth() }
            is UiState.Error -> { snackbarHostState.showSnackbar("Sign out failed"); viewModel.resetSignOutState() }
            else -> Unit
        }
    }

    LaunchedEffect(deleteAccountState) {
        when (deleteAccountState) {
            is UiState.Success -> { viewModel.resetDeleteAccountState(); onNavigateToAuth() }
            is UiState.Error -> { snackbarHostState.showSnackbar("Delete account failed"); viewModel.resetDeleteAccountState() }
            else -> Unit
        }
    }

    val uiState = SettingsUiState(userState = userState, signOutState = signOutState, deleteAccountState = deleteAccountState)

    Box(modifier = Modifier.fillMaxSize()) {
        when (userState) {
            is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Success -> SettingsContent(uiState = uiState, onEditProfile = onEditProfile, onUpdateLocation = onUpdateLocation, onSignOut = { showSignOutDialog = true }, onDeleteAccount = { showDeleteAccountDialog = true })
            is UiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error loading settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            else -> Unit
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showSignOutDialog) {
        SettingsConfirmDialog(
            title = "Sign Out", message = "Are you sure you want to sign out?",
            isLoading = signOutState is UiState.Loading, confirmText = "Sign Out",
            onConfirm = { viewModel.signOut(); showSignOutDialog = false },
            onDismiss = { showSignOutDialog = false },
        )
    }

    if (showDeleteAccountDialog) {
        SettingsConfirmDialog(
            title = "Delete Account", message = "This action cannot be undone. All your data will be permanently deleted.",
            isLoading = deleteAccountState is UiState.Loading, confirmText = "Delete Forever", isDestructive = true,
            onConfirm = { viewModel.deleteAccount(); showDeleteAccountDialog = false },
            onDismiss = { showDeleteAccountDialog = false },
        )
    }
}
