package util

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

object LocationExtensions {

    fun AppCompatActivity.hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun AppCompatActivity.requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            100
        )
    }

    fun AppCompatActivity.getCurrentUserLocation(
        onLocationReceived: (Location) -> Unit
    ) {
        val fusedLocClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000
        )
            .setMinUpdateIntervalMillis(2000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)

                result.lastLocation?.let {
                    onLocationReceived(it)
                }
            }
        }

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        val settingsClient = LocationServices.getSettingsClient(this)

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                if (
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        null
                    )
                }
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(this, 1001)
                    } catch (ex: IntentSender.SendIntentException) {
                        ex.printStackTrace()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "GPS is required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    fun AppCompatActivity.getSingleCurrentLocation(
        onLocationReceived: (Location) -> Unit
    ) {
        val fusedLocClient = LocationServices.getFusedLocationProviderClient(this)

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    onLocationReceived(it)
                }
            }
        }
    }
}