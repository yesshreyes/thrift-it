package com.example.thriftit.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkObserverImpl
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : NetworkObserver {
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        override val networkStatus =
            callbackFlow {
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            trySend(NetworkStatus.AVAILABLE)
                        }

                        override fun onLost(network: Network) {
                            trySend(NetworkStatus.LOST)
                        }

                        override fun onUnavailable() {
                            trySend(NetworkStatus.UNAVAILABLE)
                        }

                        override fun onLosing(
                            network: Network,
                            maxMsToLive: Int,
                        ) {
                            trySend(NetworkStatus.LOSING)
                        }
                    }

                // 🔥 EMIT CURRENT STATE FIRST
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)

                val isConnected =
                    caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                trySend(
                    if (isConnected) {
                        NetworkStatus.AVAILABLE
                    } else {
                        NetworkStatus.UNAVAILABLE
                    },
                )

                val request =
                    NetworkRequest
                        .Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()

                connectivityManager.registerNetworkCallback(request, callback)

                awaitClose {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }
    }
