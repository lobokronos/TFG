package com.example.proyectofinal

import android.view.View
import android.widget.TextView
import com.kizitonwose.calendar.view.ViewContainer

/**Esta clase recoge el TextView generado en el layout "calendar_day_layout" para poder mostrarlo posteriormente en la clase
 * donde generamos el calendario. Este textview contiene el numero del día.
 */

class DayViewContainer(view: View) : ViewContainer(view) {

    val textView:TextView=view.findViewById(R.id.calendarDayText)

}