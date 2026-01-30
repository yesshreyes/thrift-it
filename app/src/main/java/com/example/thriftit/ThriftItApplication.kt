package com.example.thriftit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.cloudinary.android.MediaManager
import com.example.thriftit.presentation.util.SyncManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ThriftItApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        setupFirestore()
        initCloudinary()
        createNotificationChannel()
        syncManager
    }

    private fun setupFirestore() {
        val settings =
            FirebaseFirestoreSettings
                .Builder()
                .setPersistenceEnabled(true)
                .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
            }.build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    "upload_channel",
                    "Item Uploads",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifications for item uploads"
                }

            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun initCloudinary() {
        android.util.Log.d("CLOUDINARY_INIT", "Initializing Cloudinary...")
        android.util.Log.d("CLOUDINARY_INIT", "Cloud name: ${BuildConfig.CLOUDINARY_CLOUD_NAME}")

        val config =
            hashMapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET,
                "secure" to true,
            )

        try {
            MediaManager.init(this, config)
            android.util.Log.d("CLOUDINARY_INIT", "Cloudinary initialized successfully!")
        } catch (e: IllegalStateException) {
            android.util.Log.w("CLOUDINARY_INIT", "Cloudinary already initialized")
        } catch (e: Exception) {
            android.util.Log.e("CLOUDINARY_INIT", "Failed to initialize Cloudinary: ${e.message}", e)
        }
    }
}
