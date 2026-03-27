package fr.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class CurrentLocationActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationProvider: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Charger la configuration osmdroid si tu veux stocker le cache local
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid_settings", MODE_PRIVATE)
        )
        setContentView(R.layout.activity_current_location)

        mapView = findViewById(R.id.mapViewCurrentLocation)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        // Vérification des permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Demander la permission à l'utilisateur
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        // Récupérer la dernière position connue
        val task = fusedLocationProvider.lastLocation

        task.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val currentLocation = GeoPoint(latitude, longitude)

                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(currentLocation)

                val marker = Marker(mapView)
                marker.position = currentLocation
                marker.title = "You are here"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(marker)

                Toast.makeText(
                    this@CurrentLocationActivity,
                    "Lat: $latitude, Lon: $longitude",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@CurrentLocationActivity,
                    "Impossible de récupérer la localisation",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Gérer le retour de la permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
    }
}