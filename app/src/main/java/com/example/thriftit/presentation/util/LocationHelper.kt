package com.example.thriftit.presentation.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.collections.firstOrNull

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null

        return withContext(Dispatchers.IO) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            try {
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    return@withContext Pair(lastLocation.latitude, lastLocation.longitude)
                }

                val location =
                    fusedLocationClient
                        .getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            CancellationTokenSource().token,
                        ).await()

                if (location != null) {
                    Pair(location.latitude, location.longitude)
                } else {
                    fusedLocationClient
                        .getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            CancellationTokenSource().token,
                        ).await()
                        ?.let {
                            Pair(it.latitude, it.longitude)
                        }
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationHelper", "Location error: ${e.message}")
                null
            }
        }
    }

    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): String =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                val addresses =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val resultList = mutableListOf<Address>()
                        geocoder.getFromLocation(latitude, longitude, 1) { resultList.addAll(it) }
                        resultList
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latitude, longitude, 1)?.toList() ?: emptyList()
                    }

                formatAddress(addresses.firstOrNull())
            } catch (e: Exception) {
                android.util.Log.e("LocationHelper", "Geocoder error: ${e.message}")
                "${latitude.toString().take(7)}, ${longitude.toString().take(7)}"
            }
        }

    private fun formatAddress(address: Address?): String =
        address?.let {
            buildString {
                it.thoroughfare?.takeIf { str -> str.isNotBlank() }?.let { append("$it, ") }
                it.subLocality?.takeIf { str -> str.isNotBlank() }?.let { append("$it, ") }
                it.locality?.takeIf { str -> str.isNotBlank() }?.let { append("$it, ") }
                it.adminArea?.takeIf { str -> str.isNotBlank() }?.let { append("$it, ") }
                it.countryName?.takeIf { str -> str.isNotBlank() }?.let { append(it) }
                trimEnd(',', ' ')
            }
        } ?: "Unknown Location"

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
}
