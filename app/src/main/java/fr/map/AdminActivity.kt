package fr.map

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

data class Marker(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val geohash: String = "",
    val image: String = ""
)

class AdminActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView


    private lateinit var fireStore: FirebaseFirestore

    private val markerList = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin)

        fireStore = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerViewMarkers)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            startActivity(Intent(this, Authentification::class.java))
            finish()
        }

        loadMarkers()
    }

    private fun loadMarkers() {
        fireStore.collection("markersToValidate")
            .get()
            .addOnSuccessListener { result ->

                // Filtrer les documents "default" avant de les ajouter à la liste
                val markers = result.documents.mapNotNull { doc ->
                    if (doc.id == "default") return@mapNotNull null // ignore le doc par défaut

                    val title = doc.getString("title") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: return@mapNotNull null

                    val latitude = doc.getString("latitude") ?: ""
                    val longitude = doc.getString("longitude") ?: ""
                    val geohash = doc.getString("geohash") ?: ""
                    val image = doc.getString("image") ?: ""

                    Marker(doc.id, title, description, latitude, longitude, geohash, image)
                }

                markerList.clear()
                markerList.addAll(markers)

                recyclerView.adapter = MarkerAdapter(
                    markerList,
                    onValidate = { validateMarker(it) },
                    onReject = { rejectMarker(it) },
                    onMap = { openMap(it) }
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur chargement", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateMarker(marker: Marker) {
        val data = hashMapOf(
            "title" to marker.title,
            "description" to marker.description,
            "latitude" to marker.latitude,
            "longitude" to marker.longitude,
            "geohash" to marker.geohash,
            "image" to marker.image
        )

        fireStore.collection("markers")
            .document(marker.id)
            .set(data)
            .addOnSuccessListener {
                fireStore.collection("markersToValidate")
                    .document(marker.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Marker validé", Toast.LENGTH_SHORT).show()
                        loadMarkers() // Rafraîchit la liste
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur lors de la validation", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectMarker(marker: Marker) {
        fireStore.collection("markersToValidate")
            .document(marker.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Marker refusé", Toast.LENGTH_SHORT).show()
                loadMarkers() // Rafraîchit la liste
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur lors du refus", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openMap(marker: Marker) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("lat", marker.latitude)
        intent.putExtra("long", marker.longitude)
        startActivity(intent)
    }
}