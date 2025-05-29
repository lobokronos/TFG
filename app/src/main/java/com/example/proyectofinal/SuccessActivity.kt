package com.example.proyectofinal

/**
 * Completada
 */

import android.os.Bundle
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import com.example.proyectofinal.databinding.ActivitySuccessBinding
import android.view.animation.AnimationUtils

/**
 * Esta actividad se muestra cuando el dado de alta de un usuario ha sido satisfactorio. Muestra los datos introducidos y
 * la contraseña que tendrá.
 */

class SuccessActivity : BaseActivity() {
    private var recoveredData: Bundle? = null
    private lateinit var binding: ActivitySuccessBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Esta función carga el "estilo" o animación que se ha prediseñado en la carpeta anim, en esta activity o contexto (this)
        val animation : Animation = AnimationUtils.loadAnimation(this,R.anim.anim_appear)
        binding.imageView.startAnimation(animation) // Y se la aplica a la imagen de este layout


        recoveredData = intent.getBundleExtra("data") //recuperamos los datos del nuevo empleado creado

        val textName = recoveredData?.getString("name")
        val textSurname = recoveredData?.getString("surname")
        val textNumEmp = recoveredData?.getString("numEmp")
        val textPass = recoveredData?.getString("password")

        // Y los mostramos simplemente como una cadena de texto en un editText
        binding.textUser.text = "${textName} ${textSurname} registrado. Su nuevo número de" +
                " empleado es el ${textNumEmp} y " +
                    "su contraseña es la misma que el número de empleado (${textPass})."

    }
}