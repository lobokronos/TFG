package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.databinding.ActivityCalendarBinding
import com.example.proyectofinal.databinding.ActivityHomeBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.MonthDayBinder
import java.time.Year
import java.time.YearMonth
import java.time.YearMonth.now


class CalendarActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var binding: ActivityCalendarBinding
    private var firstItem = true
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actions()
        buildCalendar()
    }

    private fun actions() {
        binding.spinnerMenu.onItemSelectedListener = this
    }

/**A traves de esta función generamos o cargamos el calendario completo con sus propiedades en la pantalla*/
    private fun buildCalendar() {

        val currentMonth = now()                                                // esta variable guarda el valor del mes actual
        val startMonth = currentMonth.minusMonths(100)           //Retrocede X meses para establecer el mes de inicio del calendario
        val endMonth = currentMonth.plusMonths(100)                 //Avanza X meses para establecer el mes final del calendario
        val firstDayOfWeek = firstDayOfWeekFromLocale()                         // Obtiene el primer día de la semana segun la configuracion horaria del dispositivo (Lunes)

        binding.calendarView.scrollPaged = true                                 // Permite hacer scroll entre los meses

        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)        // Configura el calendario con el rango de meses y el primer dia de la semana)
        binding.calendarView.scrollToMonth(currentMonth)                        // Cuando se abre la pantalla, muestra el mes actual por defecto

        /**El metodo "dayBinder" define el diseño de cada día dentro del calendario a traves de la interfáz "MonthDayBinder" en la cual accedemos a la clase
         * DayViewContainer que será la encargada de recuperar el layout donde guardamos el texto de los numeros de cada día.*/
        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {

        /**Esta función crea el diseño de cada día*/
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)                                   //Asigna la vista de día a un contenedor
            }
        /**Este método decide que mostrar en cada día del calendario utilizando el contenedor del dia actual y la fecha actual*/
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                var isActualMonth = false                                       // Variable en FALSE del mes actual para utilizar posteriormente

                if (data.date.monthValue == currentMonth.monthValue) {          //Si el numero del mes recogido en el sistema es igual al del mes actual actualiza la variable a TRUE
                    isActualMonth = true
                }
                if (isActualMonth) {                                            // Si la variable esta en true mostramos el dia y su contenido (el dia en String)
                    container.textView.isEnabled = true
                    container.textView.text = data.date.dayOfMonth.toString()
                } else {                                                        //Si no, deshabilitamos el día y su contenido
                    container.textView.isEnabled = false
                    container.textView.text = ""
                }
            }


        }
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

