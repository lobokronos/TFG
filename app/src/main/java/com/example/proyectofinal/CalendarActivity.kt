package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.example.proyectofinal.databinding.ActivityCalendarBinding
import com.example.proyectofinal.databinding.ActivityHomeBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.MonthScrollListener
import java.time.Year
import java.time.YearMonth
import java.time.YearMonth.now
import java.time.format.TextStyle
import java.util.Locale


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

        val titlesContainer = findViewById<ViewGroup>(R.id.titlesContainer)
        val children = titlesContainer.children.toList()

        for (i in children.indices){
        val textView=children[i] as TextView
        val dayOfWeek= daysOfWeek()[i]
        val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        textView.text=title
    }

        binding.calendarView.scrollPaged = true                                 // Permite hacer scroll entre los meses

        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)        // Configura el calendario con el rango de meses y el primer dia de la semana)
        binding.calendarView.scrollToMonth(currentMonth)                        // Cuando se abre la pantalla, muestra el mes actual por defecto

    /**Con la llamada al metodo monthScrollListener vamos a conseguir que escuche el mes en el que esta cuando Scrolleamos. Le decimos que es un objeto de tipo
     * MonthScrollListener. Al hacer esto, nos pide implementar los miembros "invoke" para sobreescribir esta funcion. En la funcion invoke recogemos el texto de
     * el mes correspondiente actual y se lo pasamos al textView donde lo mostrará. Es importante usar getDisplayName(TextStyle.FULL, Locale.getDefault()) ya que
     * este metodo nos devuelve el nombre del mes en Castellano. Podriamos utilizar el de java.time que sería "monthName=month.yearMonth.month.name.lowercase()
     * .replaceFirstChar { it.uppercase() }" pero nos lo devolvería en Inglés.
     */
    binding.calendarView.monthScrollListener = object : MonthScrollListener{
        override fun invoke(month: CalendarMonth) {
            val monthName = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
            binding.monthTitle.text = monthName
        }
    }
        /**El metodo "dayBinder" define el diseño de cada día dentro del calendario a traves de la interfáz "MonthDayBinder" en la cual accedemos a la clase
         * DayViewContainer que será la encargada de recuperar el layout donde guardamos el texto de los numeros de cada día. Al crear el "object" nos pide
         * implementar los miembros que vamos a sobreescribir "el metodo bind"*/
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
                    container.dayNumber.isEnabled = true
                    container.dayNumber.text = data.date.dayOfMonth.toString()
                } else {                                                        //Si no, deshabilitamos el día y su contenido
                    container.dayNumber.isEnabled = false
                    container.dayNumber.text = ""
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

