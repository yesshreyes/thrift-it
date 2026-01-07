package com.example.thriftit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.thriftit.data.local.dao.ItemDao
import com.example.thriftit.data.local.dao.UserDao
import com.example.thriftit.data.local.entities.ItemEntity
import com.example.thriftit.data.local.entities.UserEntity

@Database(
    entities = [ItemEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false,
)
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
                        ).fallbackToDestructiveMigration() // Remove in production
                        // .addMigrations(MIGRATION_1_2) // Add migrations as needed
                        .build()

                INSTANCE = instance
                instance
            }

        // Example migration for future use
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // Example: Adding a new column
                    // database.execSQL("ALTER TABLE items ADD COLUMN views INTEGER NOT NULL DEFAULT 0")
                }
            }
    }
}
