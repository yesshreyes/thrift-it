package com.example.thriftit.core.network

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkObserverEntryPoint {
    fun networkObserver(): NetworkObserver
}
