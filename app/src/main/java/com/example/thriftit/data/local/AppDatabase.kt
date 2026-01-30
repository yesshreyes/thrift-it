package com.example.thriftit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.thriftit.data.local.Converters
import com.example.thriftit.data.local.dao.ItemDao
import com.example.thriftit.data.local.dao.UserDao
import com.example.thriftit.data.local.entities.ItemEntity
import com.example.thriftit.data.local.entities.UserEntity

@Database(
    entities = [ItemEntity::class, UserEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE = "thrift_it_database"

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            DATABASE,
                        ).fallbackToDestructiveMigration()
                        .build()

                INSTANCE = instance
                instance
            }
    }
}
