package com.example.proyectofinal

/**
 * Completada
 */

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import com.example.proyectofinal.databinding.ActivitySuccessBinding

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

        val frameContent = findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)

        recoveredData = intent.getBundleExtra("data")

        val textName = recoveredData?.getString("name")
        val textSurname = recoveredData?.getString("surname")
        val textNumEmp = recoveredData?.getString("numEmp")
        val textPass = recoveredData?.getString("password")

        binding.textUser.text = "${textName} ${textSurname} registrado. Su nuevo número de" +
                " empleado es el ${textNumEmp} y " +
                    "su contraseña es la misma que el número de empleado (${textPass})."

    }
}