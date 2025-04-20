package com.example.proyectofinal

import android.content.Context
import android.graphics.Color
import android.media.Image
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate

/**Esta clase recoge el TextView generado en el layout "calendar_day_layout" para poder mostrarlo posteriormente en la clase
 * donde generamos el calendario. Este textview contiene el numero del día.
 */

class DayViewContainer(
    view: View,
    private val selectedDateText: TextView,
    private val calendarView: CalendarView,
    private var selectedDate: LocalDate?,
    private val functionShowElements:(Context, LocalDate) ->Unit //Este parámetro recoge la función de carga del dialog (CalendarActivity) y la de los container de notas del EmployeeCalendarActivity (Containers)
) : ViewContainer(view) {

    val dayNumber: TextView = view.findViewById(R.id.calendarDayText)
    val icon=view.findViewById<ImageView>(R.id.infoIcon)
    lateinit var day: CalendarDay

    init {
        view.setOnClickListener {
            val date = day.date
            val text = "${date.dayOfMonth}/${date.monthValue}/${date.year}"
            selectedDateText.text = text

            if (day.position == DayPosition.MonthDate) {  //Si la fecha pulsada está dentro del mes actual mostrado...
                val currentSelectedDate = selectedDate

                if (currentSelectedDate == day.date) {  //Si el usuario vuelve a pulsar la misma fecha...deselecciona
                    selectedDate = null
                    dayNumber.setBackgroundColor(Color.TRANSPARENT)
                    calendarView.notifyDateChanged(day.date)
                } else {                              //Si no es la misma fecha...selecciona
                    selectedDate = day.date
                    calendarView.notifyDateChanged(day.date)
                    dayNumber.setTextColor(Color.WHITE)
                    dayNumber.setBackgroundColor(Color.CYAN)
                    if (currentSelectedDate != null) {
                        calendarView.notifyDateChanged(currentSelectedDate)
                    }

                    functionShowElements(view.context,date)  //Llamada a la funcion de la clase Clanedar para mostrar el dialogo o
                                                             //de la clase EmployeeCalendar para mostrar los container según que activity acceda.

                }
            }
        }
    }
}