package com.example.proyectofinal

/**
 * Completada
 */

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityResetPassBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ResetPassActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityResetPassBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        variables()
        actions()
    }

    /**
     * Función que recoge las acciones de los elementos
     */
    private fun actions() {
        binding.btnResetPass.setOnClickListener(this)
    }

    /**
     * Función que recoge las inicializaciones de las variables
     */
    private fun variables() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

    }

    /**
     * On click de los botones
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnResetPass.id -> {
                sendEmailPass()
            }
        }
    }

    /**
     * Función para enviar un mail de reseteo de contraseña
     *
     * Esta función recoge el texto del editText donde el usuario ingresa el correo y envía un mail con dicho objetivo
     */
    private fun sendEmailPass() {
        val emailToSend = binding.emailToSend.text.toString()
        auth.setLanguageCode("es")
        if (emailToSend.isEmpty()) {
            snackBar(binding.root, "Por favor, ingresa tu correo electrónico")
        } else {
            db.collection("users").whereEqualTo("email", emailToSend).get()
                .addOnSuccessListener { query ->
                    if (!query.isEmpty) {


                        auth.sendPasswordResetEmail(emailToSend).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                snackBar(binding.root, "Revisa la bandeja de entrada de tu correo")

                            }
                        }.addOnFailureListener { e ->
                            snackBar(binding.root, "Error: ${e.message}")
                        }
                    } else {
                        snackBar(
                            binding.root,
                            "Este correo no existe en nuestro sistema o está mal escrito"
                        )
                    }
                }
                .addOnFailureListener { e ->
                    snackBar(binding.root, "Error al verificar el correo ${e.message}")
                }
        }
    }
}

/**
 * Función para reutilizar los Snackbar
 */
private fun snackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
}