package com.example.thriftit.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.thriftit.core.network.NetworkObserverEntryPoint
import com.example.thriftit.core.network.NetworkStatus
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThriftItTopBar() {
    val context = LocalContext.current.applicationContext

    val networkObserver =
        EntryPointAccessors
            .fromApplication(
                context,
                NetworkObserverEntryPoint::class.java,
            ).networkObserver()

    val networkStatus by networkObserver.networkStatus.collectAsState(
        initial = NetworkStatus.AVAILABLE,
    )

    TopAppBar(
        title = {
            Text(
                text = "THRIFT IT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        actions = {
            if (networkStatus != NetworkStatus.AVAILABLE) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
    )
}
