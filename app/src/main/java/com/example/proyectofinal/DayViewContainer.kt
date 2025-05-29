package com.example.proyectofinal

/**
 *Completada
 */


import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.ViewContainer


/**Esta clase recoge el TextView generado en el layout "calendar_day_layout" para poder mostrarlo posteriormente en la clase
 * donde generamos el calendario. Este textview contiene el numero del día. Esta clase crea una View (celda del calendario) como parámetro  y hereda
 * de ViewContainer, pasandole la vista del día creado.
 */

class DayViewContainer(
    view: View, //Este parámetro del constructor recoge la función de carga del dialog (CalendarActivity) y la de los container de notas del EmployeeCalendarActivity (Containers)
) : ViewContainer(view) {

    val dayNumber: TextView = view.findViewById(R.id.calendarDayText) //Busca el textView donde se muestra el número del día
    val icon=view.findViewById<ImageView>(R.id.infoIcon) // Busca el icono de anotaciones
    lateinit var day: CalendarDay // Esta variable se utiliza en el bind(), recogiendo a traves de ella los datos de la fecha dl día


}