package com.example.thriftit.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.thriftit.domain.models.User
import com.example.thriftit.presentation.util.UiState
import com.example.thriftit.presentation.viewmodel.SettingsViewModel

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
            is UiState.Success -> {
                viewModel.resetSignOutState()
                onNavigateToAuth()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar("Sign out failed")
                viewModel.resetSignOutState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(deleteAccountState) {
        when (deleteAccountState) {
            is UiState.Success -> {
                viewModel.resetDeleteAccountState()
                onNavigateToAuth()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar("Delete account failed")
                viewModel.resetDeleteAccountState()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = userState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Success -> {
                SettingsContent(
                    user = state.data,
                    onEditProfile = onEditProfile,
                    onUpdateLocation = onUpdateLocation,
                    onSignOut = { showSignOutDialog = true },
                    onDeleteAccount = { showDeleteAccountDialog = true },
                )
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Error loading settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            else -> Unit
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showSignOutDialog) {
        ConfirmDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            isLoading = signOutState is UiState.Loading,
            confirmText = "Sign Out",
            onConfirm = {
                viewModel.signOut()
                showSignOutDialog = false
            },
            onDismiss = { showSignOutDialog = false },
        )
    }

    if (showDeleteAccountDialog) {
        ConfirmDialog(
            title = "Delete Account",
            message = "This action cannot be undone. All your data will be permanently deleted.",
            isLoading = deleteAccountState is UiState.Loading,
            confirmText = "Delete Forever",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAccount()
                showDeleteAccountDialog = false
            },
            onDismiss = { showDeleteAccountDialog = false },
        )
    }
}

@Composable
private fun SettingsContent(
    user: User?,
    onEditProfile: () -> Unit,
    onUpdateLocation: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
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
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Manage your account and preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(24.dp))

            ProfileCard(user = user, onEditProfile = onEditProfile)

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(
                title = "Actions",
                items =
                    listOf(
                        SettingsItem(
                            icon = Icons.Default.LocationOn,
                            title = "Update Location",
                            subtitle = "Refresh your current location",
                            onClick = onUpdateLocation,
                        ),
                    ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(
                title = "Account",
                items =
                    listOf(
                        SettingsItem(
                            icon = Icons.Default.ExitToApp,
                            title = "Sign Out",
                            subtitle = "Log out from this device",
                            onClick = onSignOut,
                            isDestructive = true,
                        ),
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Account",
                            subtitle = "Permanently remove your account",
                            onClick = onDeleteAccount,
                            isDestructive = true,
                        ),
                    ),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "THRIFT IT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    user: User?,
    onEditProfile: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (user?.profileImageUrl != null) {
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier =
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = user?.initials ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = user?.displayName ?: "User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = user?.phoneNumber ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            IconButton(onClick = onEditProfile) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingsItem>,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Card(
            elevation = CardDefaults.cardElevation(1.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = item.onClick)
                                .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint =
                                if (item.isDestructive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }

                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.6.dp,
                        )
                    }
                }
            }
        }
    }
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
)

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    isLoading: Boolean,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(message)
            }
        },
        confirmButton = {
            if (!isLoading) {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = confirmText,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}
