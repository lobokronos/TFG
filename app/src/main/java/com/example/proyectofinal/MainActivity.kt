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
    private lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth=FirebaseAuth.getInstance()
        acciones()
    }

    private fun acciones(){
        binding.btnLogin.setOnClickListener(this)
    }


    override fun onClick(v: View?) {
        when (v?.id){
            binding.btnLogin.id ->{
                val email=binding.editEmployeeMail.text.toString()
                val password=binding.editPass.text.toString()

                if (email.isEmpty()){
                    Snackbar.make(binding.root,"Ingrese un email",Snackbar.LENGTH_SHORT).show()
                }
                if (password.isEmpty())
                    Snackbar.make(binding.root,"Ingrese la contraseña",Snackbar.LENGTH_SHORT).show()
                else{
                    loginUser(email,password)
                }
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task->
            if(task.isSuccessful){
                val intent = Intent(applicationContext,HomeActivity::class.java)
                startActivity(intent)
                finish()
            }else{
                Snackbar.make(binding.root,"Ha ocurrido un error",Snackbar.LENGTH_SHORT).show()
            }
        }.addOnFailureListener{ e->
            Snackbar.make(binding.root,"{${e.message}}",Snackbar.LENGTH_SHORT).show()

        }
    }
}
