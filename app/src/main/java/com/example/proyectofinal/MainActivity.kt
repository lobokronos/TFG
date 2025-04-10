package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        variables()
        actions()
    }

    private fun variables() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun actions() {
        binding.btnLogin.setOnClickListener(this)
        binding.warningPass.visibility=View.GONE
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnLogin.id -> {
                val email = binding.editEmployeeMail.text.toString()
                val password = binding.editPass.text.toString()

                if (email.isEmpty()) {
                    Snackbar.make(binding.root, "Ingrese un email", Snackbar.LENGTH_SHORT).show()
                }
                if (password.isEmpty())
                    Snackbar.make(binding.root, "Ingrese la contraseña", Snackbar.LENGTH_SHORT)
                        .show()
                else {
                    loginUser(email, password)
                }
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.setLanguageCode("es")  // Establece el idioma en español para el envío del correo de restablecimiento de contraseña
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid.toString()
                db.collection("users").whereEqualTo("uid",uid).get().addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        var login=document.getBoolean("login")
                        var numEmple=document.getLong("numEmple")!!.toString()
                        if (login==false) {
                            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    db.collection("users").document(numEmple).update("login", true)
                                    binding.warningPass.visibility=View.VISIBLE
                                    Snackbar.make(binding.root, "Email enviado, revisa tu correo.", Snackbar.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener{e ->
                                Snackbar.make(binding.root, "Error al enviar el mail: ${e.message}.", Snackbar.LENGTH_SHORT).show()
                            }
                        } else {
                            binding.warningPass.visibility=View.GONE
                            val intent = Intent(applicationContext, HomeActivity::class.java)
                            val bundle = Bundle()
                            bundle.putString("numEmple", numEmple)
                            intent.putExtra("data", bundle)
                            startActivity(intent)
                        }
                    } else {
                        Snackbar.make(binding.root, "No existe ningun usuario con ese email.", Snackbar.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Snackbar.make(binding.root, "${e.message}.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener{e ->
            Snackbar.make(binding.root, "El usuario o contraseña no coinciden: ${e.message}.", Snackbar.LENGTH_SHORT).show()
        }
    }
}
