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
import util.LocationExtensions.getCurrentUserLocation
import util.LocationExtensions.hasLocationPermission
import util.LocationExtensions.requestLocationPermission

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var lastKnownLocation: Location? = null
    private var lastKnownGeoHash: String? = null
    private var userMarker: Marker? = null
    private var firstLocation = true
    private lateinit var fireStore: FirebaseFirestore

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

        // --- Lancer l'écoute des marqueurs
        listenerMarkers()

        val btnAdd = findViewById<FloatingActionButton>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            startActivity(Intent(this, CreateMarkerActivity::class.java))
        }

        val btnCenter = findViewById<FloatingActionButton>(R.id.btnCenter)
        btnCenter.setOnClickListener {
            lastKnownLocation?.let { centerUser(it) }
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
        val geohash = GeoHash.withCharacterPrecision(latitude, longitude, 8).toBase32()
        lastKnownGeoHash = geohash.take(5)

        val currentLoc = GeoPoint(latitude, longitude)

        if (firstLocation) {
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(currentLoc)
            firstLocation = false
        }

        if (userMarker == null) {
            userMarker = Marker(mapView).apply {
                title = "Your Location"
                subDescription = geohash
            }
            mapView.overlays.add(userMarker)
        }

        userMarker?.position = currentLoc
        mapView.invalidate()
    }

    private fun listenerMarkers() {
        fireStore
            .collection("markers")
            .limit(100)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                // On vide tout et on remet seulement le marqueur utilisateur s'il existe
                mapView.overlays.clear()
                userMarker?.let { mapView.overlays.add(it) }

                snapshots?.documents?.forEach { doc ->
                    val lat = doc.get("latitude")?.toString()?.toDoubleOrNull()
                    val long = doc.get("longitude")?.toString()?.toDoubleOrNull()
                    val titre = doc.getString("title")
                    val description = doc.getString("description")
                    val base64Image = doc.getString("imageBase64")

                    if (lat != null && long != null && titre != null && description != null) {
                        val geoPoint = GeoPoint(lat, long)
                        addMarkersToMap(geoPoint, titre, description, base64Image)
                    }
                }
                mapView.invalidate()
            }
    }

    private fun addMarkersToMap(geoPoint: GeoPoint, title: String, description: String, base64Image: String?) {
        val marker = Marker(mapView).apply {
            position = geoPoint
            this.title = title
            subDescription = description

            if (!base64Image.isNullOrEmpty()) {
                try {
                    val byteArrays = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = 2 // Moins agressif que 4 pour garder un peu de qualité
                    }
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(byteArrays, 0, byteArrays.size, options)
                    image = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                startUserLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
