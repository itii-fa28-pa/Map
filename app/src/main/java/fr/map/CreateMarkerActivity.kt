package fr.map

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import ch.hsr.geohash.GeoHash
import com.google.firebase.firestore.FirebaseFirestore
import util.LocationExtensions.getSingleCurrentLocation

class CreateMarkerActivity : AppCompatActivity() {

    private lateinit var fireStore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activer l'edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.create_marker_activity)

        // Application des insets (pour la gestion des barres système)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        fireStore = FirebaseFirestore.getInstance()

        // --- Récupération des composants de l'interface
        val checkbox: CheckBox = findViewById(R.id.cbLocalisation)
        val etLongitude: EditText = findViewById(R.id.etLongitude)
        val etLatitude: EditText = findViewById(R.id.etLatitude)
        val etTitre: EditText = findViewById(R.id.etTitre)
        val etDescription: EditText = findViewById(R.id.etDescription)

        checkbox.setOnClickListener {
            val enable = !checkbox.isChecked
            etLongitude.isEnabled = enable
            etLatitude.isEnabled = enable

            if (enable) {
                etLongitude.setText("")
                etLatitude.setText("")
            } else {
                getSingleCurrentLocation { location ->
                    etLatitude.setText(location.latitude.toString())
                    etLongitude.setText(location.longitude.toString())
                }
            }
        }

        val btnConfirmer = findViewById<Button>(R.id.btnConfirmer)
        btnConfirmer.setOnClickListener {
            confirmer(etLatitude, etLongitude, etDescription, etTitre)
        }

        val btnAnnuler = findViewById<Button>(R.id.btnAnnuler)
        btnAnnuler.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    fun confirmer(
        etLatitude: EditText,
        etLongitude: EditText,
        etDescription: EditText,
        etTitre: EditText
    ) {
        val latString: String = etLatitude.text.toString()
        val longString: String = etLongitude.text.toString()
        val description: String = etDescription.text.toString()
        val titre: String = etTitre.text.toString()

        when {
            titre.isEmpty() ->
                Toast.makeText(this, "Veuillez entrer un titre", Toast.LENGTH_LONG).show()

            description.isEmpty() ->
                Toast.makeText(this, "Veuillez entrer une description", Toast.LENGTH_LONG).show()

            latString.isEmpty() ->
                Toast.makeText(this, "Veuillez entrer une latitude", Toast.LENGTH_LONG).show()

            longString.isEmpty() ->
                Toast.makeText(this, "Veuillez entrer une longitude", Toast.LENGTH_LONG).show()

            latString.toDoubleOrNull() == null ->
                Toast.makeText(this, "Latitude invalide", Toast.LENGTH_LONG).show()

            longString.toDoubleOrNull() == null ->
                Toast.makeText(this, "Longitude invalide", Toast.LENGTH_LONG).show()

            else -> {

                verifierTitreDisponible(titre) { disposable ->
                    if (disposable) {

                        val lat: Double = latString.toDouble()
                        val long: Double = longString.toDouble()
                        val geohash = GeoHash.withCharacterPrecision(lat, long, 8).toBase32()

                        addMarker(titre, description, latString, longString, geohash)

                        // Changement de page vers MapActivity
                        val changePage = Intent(this, MapActivity::class.java)
                        startActivity(changePage)

                    } else {
                        Toast.makeText(this, "Titre déjà utilisé", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun verifierTitreDisponible(titre: String, callback: (Boolean) -> Unit) {
        fireStore
            .collection("markers")
            .whereEqualTo("title", titre)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun addMarker(titre: String, description: String, lat: String, long: String, geoHash: String) {

        val newMarker = HashMap<String, String>()
        newMarker["title"] = titre
        newMarker["longitude"] = long
        newMarker["latitude"] = lat
        newMarker["description"] = description
        newMarker["geohash"] = geoHash

        fireStore
            .collection("markers")
            .document(titre)
            .set(newMarker)
            .addOnSuccessListener {
                Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failure: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun annuler() {
        // Changer de page vers TestActivity
        val changePage = Intent(this, TestActivity::class.java)
        startActivity(changePage)
    }
}