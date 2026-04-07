package fr.map

import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
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

    // --- Variable pour le rôle Admin
    private var isAdmin = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid_settings", MODE_PRIVATE)
        )

        fireStore = FirebaseFirestore.getInstance()

        // --- Vérifier si l'utilisateur est Admin
        checkUserRole()

        // --- ÉCOUTEUR INTERNET
        val cm = getSystemService(android.net.ConnectivityManager::class.java)
        var isFirstCall = true
        cm.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                if (!isFirstCall) {
                    runOnUiThread { android.widget.Toast.makeText(applicationContext, "Internet rétabli", android.widget.Toast.LENGTH_SHORT).show() }
                }
                isFirstCall = false
            }
            override fun onLost(network: android.net.Network) {
                runOnUiThread { android.widget.Toast.makeText(applicationContext, "Connexion perdue", android.widget.Toast.LENGTH_LONG).show() }
            }
        })
        // --- ÉCOUTEUR GPS
        val gpsReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val isGpsEnabled = (getSystemService(android.location.LocationManager::class.java))
                    .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                if (!isGpsEnabled) {
                    android.widget.Toast.makeText(applicationContext, "GPS coupé : position imprécise", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(applicationContext, "GPS activé", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        registerReceiver(gpsReceiver, android.content.IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION), RECEIVER_EXPORTED)

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

        // --- Initialisation barre de recherche
        val editSearch = findViewById<android.widget.EditText>(R.id.editSearch)
        editSearch.setOnEditorActionListener { v, actionId, event ->
            val query = editSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchMarker(query)
            }
            true
        }

        if (!hasLocationPermission()) {
            requestLocationPermission()
        } else {
            startUserLocation()
        }
    }

    private fun checkUserRole() {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            fireStore.collection("users").document(currentUserId).get()
                .addOnSuccessListener { document ->
                    isAdmin = document.getString("role") == "admin"
                    if (isAdmin) {
                        Toast.makeText(this, "Connecté en tant qu'ADMIN", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun searchMarker(query: String) {
        if (!::mapView.isInitialized) return

        // On parcourt les overlays de la carte pour trouver un Marker qui correspond
        val foundMarker = mapView.overlays.filterIsInstance<Marker>().find { marker ->
            marker.title?.contains(query, ignoreCase = true) == true
        }

        if (foundMarker != null) {
            mapView.controller.animateTo(foundMarker.position)
            mapView.controller.setZoom(18.0)
            foundMarker.showInfoWindow()
        } else {
            Toast.makeText(this, "Aucun marqueur trouvé pour '$query'", Toast.LENGTH_SHORT).show()
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

            // --- FEATURE ADMIN : Supprimer au clic
            setOnMarkerClickListener { m, _ ->
                if (isAdmin) {
                    androidx.appcompat.app.AlertDialog.Builder(this@MapActivity)
                        .setTitle("Supprimer le marqueur")
                        .setMessage("Voulez-vous vraiment supprimer '$title' ?")
                        .setPositiveButton("Oui") { _, _ ->
                            fireStore.collection("markers").document(title).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this@MapActivity, "Marqueur supprimé", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .setNegativeButton("Non", null)
                        .show()
                }
                m.showInfoWindow()
                true
            }

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
