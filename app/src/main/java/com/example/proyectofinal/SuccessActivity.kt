package com.example.proyectofinal

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.databinding.ActivityProfileBinding
import com.example.proyectofinal.databinding.ActivitySuccessBinding

class SuccessActivity : BaseActivity() {
    private lateinit var binding: ActivitySuccessBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySuccessBinding.inflate(layoutInflater)

        val frameContent = findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)

        val recoveredData = intent.getBundleExtra("data")

        val textName = recoveredData?.getString("name")
        val textSurname = recoveredData?.getString("surname")
        val textNumEmp = recoveredData?.getString("numEmp")
        val textPass = recoveredData?.getString("password")

        binding.textUser.text = "${textName} ${textSurname} registrado. Su nuevo número de" +
                " empleado es el ${textNumEmp} y " +
                    "su contraseña es la misma que el número de empleado (${textPass})."

    }
}