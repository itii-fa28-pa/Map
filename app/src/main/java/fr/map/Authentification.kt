package fr.map

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import android.widget.Toast

class Authentification : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth  //authentification avec firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_authentification)
        auth = Firebase.auth // authentification a firebase
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
    }

    fun onClick_bt_connection(view: View) {
        val email = findViewById<EditText>(R.id.id_email).text.toString()
        val password = findViewById<EditText>(R.id.id_password).text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Log.e("Auth", "Champs vides !")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("Auth", "Connexion réussie !")
                    startActivity(Intent(this, map::class.java))
                    finish()
                } else {
                    // Récupère le type d'erreur Firebase
                    when {
                        task.exception?.message?.contains("no user record") == true ->
                            Toast.makeText(this, "Aucun compte trouvé avec cet email", Toast.LENGTH_LONG).show()

                        task.exception?.message?.contains("password is invalid") == true ->
                            Toast.makeText(this, "Mot de passe incorrect", Toast.LENGTH_LONG).show()

                        task.exception?.message?.contains("badly formatted") == true ->
                            Toast.makeText(this, "Email invalide", Toast.LENGTH_LONG).show()

                        email.isEmpty() ->
                            Toast.makeText(this, "Email non renseigner", Toast.LENGTH_LONG).show()

                        email.isEmpty() ->
                            Toast.makeText(this, "mot de passe non renseigner", Toast.LENGTH_LONG).show()

                        else ->
                            Toast.makeText(this, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

    }

    fun onClick_bt_CreateAccount(view: View) {
        val intent = Intent(this, newaccount::class.java)
        startActivity(intent)
    }

}
