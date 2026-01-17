package com.example.thriftit.core.network

import kotlinx.coroutines.flow.Flow

interface NetworkObserver {
    val networkStatus: Flow<NetworkStatus>
}
