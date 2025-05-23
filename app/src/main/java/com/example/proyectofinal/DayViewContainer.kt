package com.example.proyectofinal

/**
 *
 * No completada
 *
 * Falta el comentario
 */


import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.ViewContainer


/**Esta clase recoge el TextView generado en el layout "calendar_day_layout" para poder mostrarlo posteriormente en la clase
 * donde generamos el calendario. Este textview contiene el numero del día.
 */

class DayViewContainer(
    view: View, //Este parámetro recoge la función de carga del dialog (CalendarActivity) y la de los container de notas del EmployeeCalendarActivity (Containers)
) : ViewContainer(view) {

    val dayNumber: TextView = view.findViewById(R.id.calendarDayText) //Busca el textView donde se muestra el número del día
    val icon=view.findViewById<ImageView>(R.id.infoIcon) // Busca el icono de anotaciones
    lateinit var day: CalendarDay


}