package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.databinding.ActivitySubscribeBinding

class SubscribeActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var binding: ActivitySubscribeBinding
    private var firstItem = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscribeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actions()
    }

    private fun actions() {
        binding.spinnerMenu.onItemSelectedListener = this
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, id: Long) {
        if (firstItem) {
            firstItem = false
            return
        }
        when (p2) {
            0 -> startActivity(Intent(this.applicationContext, HomeActivity::class.java))
            1 -> startActivity(Intent(this.applicationContext, CalendarActivity::class.java))
            2 -> startActivity(Intent(this.applicationContext, SubscribeActivity::class.java))
            3 -> startActivity(Intent(this.applicationContext, UnsubscribeActivity::class.java))
        }
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {

    }
}
