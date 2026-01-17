package com.example.thriftit.di

import com.example.thriftit.core.network.NetworkObserver
import com.example.thriftit.core.network.NetworkObserverImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindNetworkObserver(impl: NetworkObserverImpl): NetworkObserver
}
