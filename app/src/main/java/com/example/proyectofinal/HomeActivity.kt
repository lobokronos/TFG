package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, View.OnClickListener {
    private lateinit var binding: ActivityHomeBinding
    private var firstItem = true
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actions()
        binding.btnCalendario.setOnClickListener(this)
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

    override fun onClick(v: View?) {
        when(v?.id){
            binding.btnCalendario.id ->{
                startActivity(Intent(this, CalendarActivity::class.java))
            }
        }
    }
}
