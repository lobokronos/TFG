package com.example.proyectofinal

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.databinding.ActivityAddQuitEmployeeBinding

class AddQuitEmployeeActivity : BaseActivity() {
    private lateinit var binding:ActivityAddQuitEmployeeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAddQuitEmployeeBinding.inflate(layoutInflater)

        val frameContent=findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)

    }
}