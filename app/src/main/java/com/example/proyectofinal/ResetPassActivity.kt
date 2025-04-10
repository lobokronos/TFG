package com.example.proyectofinal

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
    private lateinit var editOldPass: String
    private lateinit var editNewPass: String
    private lateinit var repeatNewPass: String
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var recoveredData: Bundle? = null
    private lateinit var numEmple: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        variables()
        binding.btnResetPass.setOnClickListener(this)


    }

    private fun variables() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recoveredData = intent.getBundleExtra("data")
        numEmple = recoveredData?.getString("numEmple") ?: ""
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnResetPass.id -> {
                resetPass()
            }
        }
    }

    private fun resetPass() {
        val intent = Intent(this, MainActivity::class.java)
        editOldPass = binding.editOldPass.text.toString()
        editNewPass = binding.editNewPass.text.toString()
        repeatNewPass = binding.editRepeatPass.text.toString()
        var userOldPass: String
        if (editOldPass.isEmpty() || editNewPass.isEmpty() || repeatNewPass.isEmpty()) {
            Snackbar.make(
                binding.root,
                "Todos los campos deben estar completos",
                Snackbar.LENGTH_SHORT
            ).show()
        } else {
            db.collection("users").document(numEmple).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    println(editOldPass)
                    println(editNewPass)
                    println(repeatNewPass)
                    userOldPass = document.getString("password").toString()
                    if (userOldPass == editOldPass) {
                        if (editNewPass == repeatNewPass) {
                            val user=auth.currentUser
                                if(user!=null) {
                                    user.updatePassword(repeatNewPass).addOnSuccessListener {
                                        db.collection("users").document(numEmple)
                                            .update("password", repeatNewPass)
                                            .addOnSuccessListener {
                                                db.collection("users").document(numEmple)
                                                    .update("login", true).addOnSuccessListener {
                                                        Snackbar.make(
                                                            binding.root,
                                                            "La contraseña ha sido actualizada",
                                                            Snackbar.LENGTH_SHORT
                                                        ).show()
                                                        startActivity(intent)
                                                        finish()
                                                    }.addOnFailureListener {
                                                        Snackbar.make(
                                                            binding.root,
                                                            "Error al actualizar la contraseña",
                                                            Snackbar.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }

                                    }.addOnFailureListener {
                                        Snackbar.make(
                                            binding.root,
                                            "Error al actualizar la contraseña en Authentication",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }else{
                                    Snackbar.make(
                                        binding.root,
                                        "El usuario no esta autenticado o no ha iniciado sesion",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } else {
                        Snackbar.make(
                            binding.root,
                            "La contraseña no es correcta",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}