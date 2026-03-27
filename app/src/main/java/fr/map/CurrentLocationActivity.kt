package fr.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class CurrentLocationActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var fusedLocationProvider: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid_settings", MODE_PRIVATE))
        setContentView(R.layout.activity_current_location)
        mapView = findViewById(R.id.mapViewCurrentLocation)

        mapView!!.setTileSource(TileSourceFactory.MAPNIK)
        mapView!!.setMultiTouchControls(true)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        val task = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        } else {
            val task = fusedLocationProvider!!.lastLocation

            task.addOnSuccessListener(OnSuccessListener<Location> { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    val currentLocation = GeoPoint(latitude, longitude)

                    mapView!!.controller.setZoom(15.0)
                    mapView!!.controller.setCenter(currentLocation)

                    val marker = Marker(mapView)
                    marker.position = currentLocation
                    marker.title = "Your position"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView!!.overlays.add(marker)

                    val debug = "$latitude     $longitude"

                    Toast.makeText(
                        this@CurrentLocationActivity,
                        debug,
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                    Toast.makeText(
                        this@CurrentLocationActivity,
                        "Could not fetch location",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }
}