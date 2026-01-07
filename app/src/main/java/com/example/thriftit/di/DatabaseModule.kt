package com.example.thriftit.di

import android.content.Context
import com.example.thriftit.data.local.AppDatabase
import com.example.thriftit.data.local.dao.ItemDao
import com.example.thriftit.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = AppDatabase.getDatabase(context)

    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
}
