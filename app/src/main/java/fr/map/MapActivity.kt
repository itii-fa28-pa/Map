package fr.map

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import ch.hsr.geohash.GeoHash

import util.LocationExtensions.getCurrentUserLocation
import util.LocationExtensions.hasLocationPermission
import util.LocationExtensions.requestLocationPermission

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    private var lastKnownLocation: Location? = null
    private var marker: Marker? = null
    private var firstLocation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid_settings", MODE_PRIVATE)
        )

        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            startActivity(Intent(this, CreateMarkerActivity::class.java))
        }

        val btnCenter = findViewById<FloatingActionButton>(R.id.btnCenter)
        btnCenter.setOnClickListener {
            lastKnownLocation?.let {
                centerUser(it)
            }
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            startActivity(Intent(this, Authentification::class.java))
            finish()
        }

        mapView = findViewById(R.id.mapViewLive)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        if (!hasLocationPermission()) {
            requestLocationPermission()
        } else {
            startUserLocation()
        }
    }

    private fun startUserLocation() {
        getCurrentUserLocation { location ->
            lastKnownLocation = location
            updateUserLocation(location)
        }
    }

    private fun centerUser(location: Location) {
        val centerPoint = GeoPoint(location.latitude, location.longitude)
        mapView.controller.setZoom(15.0)
        mapView.controller.animateTo(centerPoint)
    }

    private fun updateUserLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude

        val geohash = GeoHash.withCharacterPrecision(
            latitude,
            longitude,
            8
        ).toBase32()

        val currentLoc = GeoPoint(latitude, longitude)

        if (firstLocation) {
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(currentLoc)
            firstLocation = false
        }

        if (marker == null) {
            marker = Marker(mapView).apply {
                title = "Your Location"
                subDescription = geohash
            }
            mapView.overlays.add(marker)
        }

        marker?.position = currentLoc
        mapView.invalidate()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        if (hasLocationPermission()) {
            startUserLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100) {
            if (
                grantResults.isNotEmpty() &&
                grantResults.all {
                    it == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            ) {
                startUserLocation()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}