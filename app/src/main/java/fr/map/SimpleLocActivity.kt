package fr.map

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class SimpleLocActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var btnToggle: Button? = null
    private var isSatellite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid_settings", MODE_PRIVATE))
        setContentView(R.layout.activity_simple_loc)

        mapView = findViewById(R.id.mapViewSimpleLoc)
        btnToggle = findViewById(R.id.btnToggle)

        createMap()

        btnToggle!!.setOnClickListener {
            if (isSatellite) {
                mapView!!.setTileSource(TileSourceFactory.MAPNIK)
                btnToggle!!.setText("Satellite View")
            }
            else {
                mapView!!.setTileSource(TileSourceFactory.USGS_SAT)
                btnToggle!!.setText("Normal View")
            }
            isSatellite = !isSatellite
        }
    }

    private fun createMap() {
        mapView!!.setTileSource(TileSourceFactory.MAPNIK)
        mapView!!.setMultiTouchControls(true)
        mapView!!.controller.setZoom(15.0)

        //we need to specify a point of market

        val startPoint = GeoPoint(49.449399, 2.100129)
        mapView!!.controller.setCenter(startPoint)

        val startMarker = Marker(mapView)
        startMarker.position = startPoint
        startMarker.title = "ITII"
        startMarker.subDescription = "Fuyez pauvre fous"

        mapView!!.overlays.add(startMarker)
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }
}