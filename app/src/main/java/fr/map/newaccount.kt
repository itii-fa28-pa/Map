package fr.map

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import android.content.Intent
import android.util.Log
import android.widget.EditText
import com.google.firebase.auth.auth
import android.widget.Toast

class newaccount : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = Firebase.auth
        setContentView(R.layout.activity_newaccount)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun onClick_inscription(view: View) {
        val email = findViewById<EditText>(R.id.id_NewEmail).text.toString()
        val password = findViewById<EditText>(R.id.id_NewPassword).text.toString()
        val passwordConfirm = findViewById<EditText>(R.id.id_confirmer_mdp_new).text.toString()

        // Vérifications AVANT d'appeler Firebase
        when {
            email.isEmpty() ->
                Toast.makeText(this, "Veuillez entrer un email", Toast.LENGTH_LONG).show()

            password.isEmpty() ->
                Toast.makeText(this, "Veuillez entrer un mot de passe", Toast.LENGTH_LONG).show()

            passwordConfirm.isEmpty() ->
                Toast.makeText(this, "Veuillez confirmer votre mot de passe", Toast.LENGTH_LONG)
                    .show()

            password != passwordConfirm ->
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_LONG)
                    .show()

            password.length < 6 ->
                Toast.makeText(
                    this,
                    "Le mot de passe doit contenir au moins 6 caractères",
                    Toast.LENGTH_LONG
                ).show()

            else -> {
                // Tout est OK, on appelle Firebase
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d("Auth", "Compte créé !")
                            Toast.makeText(this, "Compte créé avec succès !", Toast.LENGTH_LONG)
                                .show()
                            startActivity(Intent(this, map::class.java))
                            finish()
                        } else {
                            when {
                                task.exception?.message?.contains("email address is already in use") == true ->
                                    Toast.makeText(
                                        this,
                                        "Cet email est déjà utilisé",
                                        Toast.LENGTH_LONG
                                    ).show()

                                task.exception?.message?.contains("badly formatted") == true ->
                                    Toast.makeText(this, "Email invalide", Toast.LENGTH_LONG).show()

                                task.exception?.message?.contains("password is invalid") == true ->
                                    Toast.makeText(
                                        this,
                                        "Mot de passe trop faible",
                                        Toast.LENGTH_LONG
                                    ).show()

                                else ->
                                    Toast.makeText(
                                        this,
                                        "Erreur : ${task.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                            }
                        }
                    }
            }
        }
    }

    fun onClick_annuler(view: View) {
        val intent = Intent(this, Authentification::class.java)
        startActivity(intent)
    }
}