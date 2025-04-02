package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityHomeBinding

class HomeActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)

        /**Esta variable carga el contenido del FrameLayout situado en el activity_base.xml, el cual
        se encarga de mostrar el contenido de la propia página(binding.root), el cual inflará
        dicho contenido a traves de la variable binding(en este caso carga ActivityHomeBinding).
        En cada activity, funcionará del mismo modo, cargando el contenido personalizado de cada
        página a traves del FrameLayout del BaseActivity).**/

        val frameContent=findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)



        binding.btnCalendario.setOnClickListener(this)
    }





    override fun onClick(v: View?) {
        when(v?.id){
            binding.btnCalendario.id ->{
                startActivity(Intent(this, CalendarActivity::class.java))
            }
        }
    }
}
