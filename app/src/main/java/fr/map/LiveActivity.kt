package fr.map

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class LiveActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var fusedLocClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallBack: LocationCallback? = null
    private var marker: Marker? = null
    private var gpsDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)
        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences("osmdroid_settings", MODE_PRIVATE))
        mapView = findViewById(R.id.mapViewLive)
        mapView!!.setTileSource(TileSourceFactory.MAPNIK)
        mapView!!.setMultiTouchControls(true)

        requestLocationUpdates()
        checkGPSAndRequestForLocation()
    }

    private fun checkGPSAndRequestForLocation() {
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)
            .setAlwaysShow(true)
            .build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                startLocationUpdates()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        gpsDialogShown = true
                        exception.startResolutionForResult(this, 1001)
                    } catch(ex: IntentSender.SendIntentException) {
                        ex.printStackTrace()
                    }
                }
                else {
                    Toast.makeText(this, "GPS is required to track location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationRequest?.let {locationCallBack?.let { it1 ->
                fusedLocClient!!.requestLocationUpdates(locationRequest!!, locationCallBack!!, null)

            }}
        }
    }

    private fun requestLocationUpdates() {
        fusedLocClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(2000).build()

        locationCallBack = object : LocationCallback()
        {
            override fun onLocationResult(locationRequest: LocationResult) {
                super.onLocationResult(locationRequest)
                for (location in locationRequest.locations) {
                    updateUserLocation(location)
                }
            }
        }
    }

    private fun updateUserLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude

        val debug = "$latitude     $longitude"

        Toast.makeText(
            this@LiveActivity,
            debug,
            Toast.LENGTH_LONG
        ).show()

        val currentLoc = GeoPoint(latitude, longitude)

        mapView!!.controller.setZoom(15.0)
        mapView!!.controller.setCenter(currentLoc)
        mapView!!.overlays.remove(marker)

        val currentMarker = Marker(mapView)
        currentMarker.position = currentLoc
        currentMarker.title = "Your Location"
        currentMarker.subDescription = "You are there"

        mapView!!.overlays.add(currentMarker)

    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            gpsDialogShown = false
            if (resultCode == RESULT_OK) {
                checkGPSAndRequestForLocation()
            }
            else {
                Toast.makeText(this, "Location disabled, Please enable it", Toast.LENGTH_SHORT).show()
            }
        }
    }
}