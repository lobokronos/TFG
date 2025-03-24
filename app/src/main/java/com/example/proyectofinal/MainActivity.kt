package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        acciones()
    }
    private fun acciones(){
        binding.botonLogin.setOnClickListener(this)
    }
    private fun iniciarSesion(numEmp: Long, pass: String) {
        db.collection("users").document("q7qwmqlrnuMYKvEQnU9P").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val numEmple = document.getLong("numEmple")
                    val passwd = document.getString("password")
                    val nombre = document.getString("nombre")

                    println("numEmple: $numEmple, passwd: $passwd, nombre: $nombre")

                    if (passwd == pass && numEmple == numEmp) {
                        val intent = Intent(this.applicationContext, HomeActivity::class.java)
                        val bundle = Bundle()
                        bundle.putString("nombre", nombre)
                        intent.putExtra("datos", bundle)
                        startActivity(intent)
                    } else {
                        Snackbar.make(
                            binding.root,
                            "El nº de empleado o la contraseña no coinciden",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }


            }
    }

    override fun onClick(v: View?) {
        when (v?.id){
            binding.botonLogin.id ->{
                val numEmpleado=binding.editWorkerNumber.text.toString().toLong()
                val contrasena=binding.editPass.text.toString()
                iniciarSesion(numEmpleado,contrasena)
            }
        }
    }
}
