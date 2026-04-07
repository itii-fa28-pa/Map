package fr.map

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import ch.hsr.geohash.GeoHash
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import util.LocationExtensions.getSingleCurrentLocation

class CreateMarkerActivity : AppCompatActivity() {

    private lateinit var fireStore: FirebaseFirestore
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                findViewById<ImageView>(R.id.ivSelectedImage).apply {
                    setImageURI(uri)
                    visibility = View.VISIBLE
                }
                findViewById<ImageView>(R.id.ivPlaceholderIcon).visibility = View.GONE
                findViewById<TextView>(R.id.tvImagePlaceholder).visibility = View.GONE
            }
        }

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

        findViewById<MaterialCardView>(R.id.flImagePicker).setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
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

        val imageUri = selectedImageUri
        if (imageUri != null) {
            try {
                // 1. Ouvrir le flux de l'image sélectionnée
                val inputStream = contentResolver.openInputStream(imageUri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // 2. Redimensionner l'image (max 800px de large ou haut pour rester léger)
                    val maxSize = 800
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val ratio = width.toFloat() / height.toFloat()

                    val newWidth: Int
                    val newHeight: Int
                    if (width > height) {
                        newWidth = maxSize
                        newHeight = (maxSize / ratio).toInt()
                    } else {
                        newHeight = maxSize
                        newWidth = (maxSize * ratio).toInt()
                    }

                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

                    // 3. Compresser en JPEG (qualité 70% pour un bon compromis poids/visuel)
                    val outputStream = java.io.ByteArrayOutputStream()
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val bytes = outputStream.toByteArray()

                    // 4. Convertir en Base64 pour Firestore
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    newMarker["imageBase64"] = base64
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Erreur lors de la compression de l'image", Toast.LENGTH_SHORT).show()
            }
        }

        fireStore
            .collection("markersToValidate")
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
        val changePage = Intent(this, MapActivity::class.java)
        startActivity(changePage)
    }
}