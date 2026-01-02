package com.example.thriftit

import android.app.Application
import com.cloudinary.android.MediaManager

class ThriftItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initCloudinary()
    }

    private fun initCloudinary() {
        val config =
            hashMapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET,
                "secure" to true,
            )

        try {
            MediaManager.init(this, config)
        } catch (e: IllegalStateException) {
            // MediaManager already initialized (safe to ignore)
        }
    }
}
