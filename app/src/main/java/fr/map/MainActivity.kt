package fr.map

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // --- Récupération de toutes les composants
        val checkbox: CheckBox = findViewById<CheckBox>(R.id.cbLocalisation)
        val long: EditText = findViewById<EditText>(R.id.etLongitude)
        val lat: EditText = findViewById<EditText>(R.id.etLatitude)
        val description: EditText = findViewById<EditText>(R.id.etDescription)

        // --- Déclaration des listener
        checkbox.setOnClickListener {
            val enable = !checkbox.isEnabled
            long.isEnabled = enable
            lat.isEnabled = enable
        }
        val btnConfirmer = findViewById<Button>(R.id.btnConfirmer)
        btnConfirmer.setOnClickListener {
            Toast.makeText(this, "Vous avez cliqué sur confirmer", Toast.LENGTH_LONG).show()
            confirmer(checkbox, lat, long, description)
        }
        val btnAnnuler = findViewById<Button>(R.id.btnAnnuler)
        btnAnnuler.setOnClickListener {
            Toast.makeText(this, "Vous avez cliqué sur annuler", Toast.LENGTH_LONG).show()
            annuler()
        }
    }

    fun confirmer(checkBox: CheckBox, etLatitude: EditText, etLongitude: EditText,
            etDescription: EditText) {
        var lat: Double
        var long: Double
        val description: String = etDescription.text.toString()

        // --- On traite ou non la long et lat fournie
        if (checkBox.isChecked) {
            // On récupère les coordoonées via la position
        } else {
            // On récupère les coordonnées via les champ lat / long
            lat = etLatitude.text.toString().toDouble()
            long = etLongitude.text.toString().toDouble()
        }

        // TODO : On créer notre objet ici

        // --- Changement de page
        val changePage = Intent(this, TestActivity::class.java)
        startActivity(changePage)
    }

    fun annuler() {
        // --- Changement de page
        val changePage = Intent(this, TestActivity::class.java)
        startActivity(changePage)
    }
}