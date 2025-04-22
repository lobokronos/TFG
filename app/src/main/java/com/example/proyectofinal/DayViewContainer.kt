package com.example.proyectofinal

import android.content.Context
import android.graphics.Color
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
            val date = day.date // Variable que recoge la fecha entera de un día (Ej:2025-04-18)
            val text = "${date.dayOfMonth}/${date.monthValue}/${date.year}" //Construimos la fecha para mostrarla extrayendo las valores de date.
            selectedDateText.text = text /*Variable construida a partir del elemento selectedDayText de las activitys del calendario,
            la cual recoge el texto resultante de la fecha construida en "text". Se pone aquí y no en cada clase porque necesitamos que
            se cliquee un dia para poder asignarle su valor en String y mostarlo.*/

            if (day.position == DayPosition.MonthDate) {  //Si la fecha pulsada está dentro del mes actual mostrado...
                val currentSelectedDate = selectedDate
                if (currentSelectedDate == day.date) {  //Si el usuario vuelve a pulsar la misma fecha...deselecciona
                    selectedDate = null
                    calendarView.notifyDateChanged(day.date)
                } else {                              //Si no es la misma fecha...selecciona
                    selectedDate = day.date
                    functionShowElements(view.context,date)  /*Llamada a la funcion de la clase Clanedar para mostrar el dialogo o
                                                             de la clase EmployeeCalendar para mostrar los container según que activity acceda.*/

                    dayNumber.setTextColor(Color.WHITE)
                    dayNumber.setBackgroundColor(Color.CYAN)

                    calendarView.notifyDateChanged(day.date)
                    if (currentSelectedDate != null) {
                        calendarView.notifyDateChanged(currentSelectedDate)
                    }



                }
            }
        }
    }
}