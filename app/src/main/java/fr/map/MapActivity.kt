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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import util.LocationExtensions.getCurrentUserLocation
import util.LocationExtensions.hasLocationPermission
import util.LocationExtensions.requestLocationPermission

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var lastKnownLocation: Location? = null
    private var marker: Marker? = null
    private var firstLocation = true

    private lateinit var fireStore: FirebaseFirestore

    private var activeGeoHash: String? = null
    private var markerListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid_settings", MODE_PRIVATE)
        )

        fireStore = FirebaseFirestore.getInstance()

        mapView = findViewById(R.id.mapViewLive)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

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

        val newGeoHash = geohash.take(5)

        if (activeGeoHash != newGeoHash) {
            activeGeoHash = newGeoHash
            listenerMarkers()
        }

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

    private fun listenerMarkers() {
        if (activeGeoHash == null) return

        markerListener?.remove()

        markerListener = fireStore
            .collection("markers")
            .whereGreaterThanOrEqualTo("geohash", activeGeoHash!!)
            .whereLessThanOrEqualTo("geohash", activeGeoHash!! + "\uf8ff")
            .addSnapshotListener { snapshots, error ->

                if (error != null) return@addSnapshotListener

                val userMarker = marker
                mapView.overlays.clear()
                userMarker?.let {
                    mapView.overlays.add(it)
                }

                snapshots?.documents?.forEach { doc ->
                    val lat = doc.getString("latitude")?.toDoubleOrNull()
                    val long = doc.getString("longitude")?.toDoubleOrNull()
                    val titre = doc.getString("title")
                    val description = doc.getString("description")

                    if (lat != null && long != null && titre != null && description != null) {
                        val geoPoint = GeoPoint(lat, long)
                        addMarkers(geoPoint, titre, description)
                    }
                }

                mapView.invalidate()
            }
    }

    private fun addMarkers(
        geoPoint: GeoPoint,
        title: String,
        description: String
    ) {
        val marker = Marker(mapView).apply {
            position = geoPoint
            this.title = title
            subDescription = description
        }

        mapView.overlays.add(marker)
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

    override fun onDestroy() {
        super.onDestroy()
        markerListener?.remove()
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