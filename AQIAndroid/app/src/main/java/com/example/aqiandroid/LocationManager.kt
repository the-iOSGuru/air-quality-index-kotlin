// LocationManager.kt
package com.example.aqiandroid
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat

class LocationManager(private val context: Context) {
    private val locationManager: android.location.LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager

    fun requestLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            locationManager.requestSingleUpdate(
                android.location.LocationManager.GPS_PROVIDER,
                object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        callback(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                },
                Looper.getMainLooper()
            )
        } else {
            callback(null)
        }
    }
}