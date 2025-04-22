package com.example.proyectofinal

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import com.example.proyectofinal.databinding.ActivityCalendarBinding
import com.example.proyectofinal.databinding.DialogScheduleNotesBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthScrollListener
import java.time.LocalDate
import java.time.YearMonth.now
import java.time.format.TextStyle
import java.util.Locale


class CalendarActivity : BaseActivity(), AdapterView.OnItemSelectedListener,
    View.OnClickListener {
    private lateinit var binding: ActivityCalendarBinding
    private var selectedDate: LocalDate? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var pickedDate: String
    private lateinit var name: String
    private lateinit var numEmple: String
    private lateinit var turn: String
    private var positionSection: Int = 0
    private var employeeTurns: MutableMap<LocalDate, String> = mutableMapOf()
    private var statusPublicEmp: MutableMap<LocalDate, String> = mutableMapOf()
    private lateinit var selectedSection: String
    private lateinit var selectedEmployee: String
    private var status: String = ""
    private lateinit var builder: AlertDialog.Builder
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)

        val frameContent = findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)

        variables()
        actions()
        buildCalendar()
        binding.allContainer.visibility=View.GONE
    }

    private fun actions() {
        binding.spinnerSections.onItemSelectedListener = this
        binding.spinnerEmployeeSelected.onItemSelectedListener = this
        binding.btnSave.setOnClickListener(this)
        binding.btnAccept.setOnClickListener(this)
        binding.btnReject.setOnClickListener(this)
        binding.spinnerTurns.onItemSelectedListener = this
    }

    private fun variables() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        builder = AlertDialog.Builder(this)
    }

    /**A traves de esta función generamos o cargamos el calendario completo con sus propiedades en la pantalla*/
    private fun buildCalendar() {

        val currentMonth = now()                                                // esta variable guarda el valor del mes actual
        val startMonth = currentMonth.minusMonths(100)           //Retrocede X meses para establecer el mes de inicio del calendario
        val endMonth = currentMonth.plusMonths(100)                 //Avanza X meses para establecer el mes final del calendario
        val firstDayOfWeek =
            firstDayOfWeekFromLocale()                         // Obtiene el primer día de la semana segun la configuracion horaria del dispositivo (Lunes)

        val titlesContainer = findViewById<ViewGroup>(R.id.titlesContainer) //Captura el contenedor titlesContainer(Titulos de los dias) en una variable
        titlesContainer.children // recoge una secuencia de todas las vistas hijas de los titulos (son 7).
            .map { it as TextView } //recorre la secuencia y cada elemento lo pasa a textView
            .forEachIndexed { index, textView -> //forEachIndexed recorre la lista y nos da el TextView y el Indice de cada elemento.
                val dayOfWeek = daysOfWeek()[index] //daysOfWeek asigna el dia correspondiente con el valor del índice.
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) // Asignamos el titulo del día a cada título.
            textView.text = title
            if (textView.text == "dom") { // Si el textView correspondiente en el bucle es "dom":
                textView.setTextColor(Color.WHITE) //Aparecerá con letras blancas y fondo rojo.
                textView.setBackgroundColor(Color.RED)
            } else {
                textView.setTextColor(Color.WHITE) // Si no, el color de fonjdo será azúl
                textView.setBackgroundColor(Color.BLUE)
            }
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
        binding.calendarView.monthScrollListener = object : MonthScrollListener {
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
            override fun create(view: View): DayViewContainer = DayViewContainer(view, binding.selectedDateText,
                binding.calendarView, selectedDate, ::showScheduleNotesItems) //Asigna la vista de día a un contenedor

            /**
             * Este método que muestra el contenido de un "adapter". Configura la vista de cada día. Container se encarga de recoger las vistas
             * de los días y data posee los datos de una fecha seleccionada en el calendario.
             * */
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data //Esta variable recoge toda la información obtenida en el DayViewContainer al seleccionar un día.
                container.dayNumber.text = data.date.dayOfMonth.toString() //Aqui asignamos el numero a la vista de cada zelda de día.

                if (data.date.monthValue == currentMonth.monthValue) {
                    container.dayNumber.setTextColor(Color.BLACK)
                } else {
                    container.dayNumber.setTextColor(Color.GRAY)
                }
                //Utilizamos la lista calve-valor que contiene la fecha/turno para ir pintando cada dia de un color cuando el método bind los construya
                val recoveredTurn = employeeTurns[data.date]
                val showPublicDate = statusPublicEmp[data.date]

                when (recoveredTurn) {
                    "Mañana" -> { container.dayNumber.setBackgroundColor(Color.YELLOW) }
                    "Tarde" -> { container.dayNumber.setBackgroundColor(Color.BLUE) }
                    "Festivo" -> { container.dayNumber.setBackgroundColor(Color.RED) }
                    "Libre" -> { container.dayNumber.setBackgroundColor(Color.GREEN) }
                    "Borrar turno" -> {
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("turno").delete()
                        container.dayNumber.setBackgroundColor(Color.TRANSPARENT)
                    }
                    else -> { container.dayNumber.setBackgroundColor(Color.TRANSPARENT) }
                }
                when (showPublicDate) {
                    "pendiente" -> { container.dayNumber.setTextColor(Color.MAGENTA) }
                    "aceptado" -> {
                        container.dayNumber.setTextColor(Color.GREEN)
                        binding.btnAccept.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE
                    }
                    "rechazado" -> {
                        container.dayNumber.setTextColor(Color.RED)
                        binding.btnAccept.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE
                    }
                }

            }
        }

    }

    /**
     * Esta función se activa cuando se pulsa un día a traves del DayViewContainer. Carga las sugerencias de los empleados
     * seleccionados en caso de que las haya, muestra los contenedores correspondientes si las hay, y llama a la función
     * loadPickedSectionEmployees() para mostrar la lista turnos de los empleados de la sección.
     */
    private fun showScheduleNotesItems(context: Context, date: LocalDate) {
        binding.allContainer.visibility = View.VISIBLE //
        pickedDate = "${date.dayOfMonth}-${date.monthValue}-${date.year}" //String para mostrar en TextViews la fecha (día, mes, año)
        val dbNotes = db.collection("turnos").document(pickedDate).collection(numEmple).document("notas")
        dbNotes.collection("public").document("notaPublica").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val publicText = document.getString("nota")
                    status = document.getString("estado").toString()
                    binding.publicNotesContainer.visibility = View.VISIBLE
                    val finalPublicText = "${publicText}"
                    binding.publicNoteText.text = finalPublicText
                } else {
                    binding.publicNotesContainer.visibility = View.GONE
                }
                loadPickedSectionEmployees()
            }
    }

    /**
     * Esta función se encarga de recoger los empleados existentes y sus turnos, y recogerlos en un TextView cuando se
     * seleccione una seccion en su Spinner correspondiente
     */

    private fun loadPickedSectionEmployees() {
        var finalSheduleList = "" //Variable donde vamos a guardar la lista de empleados y turnos.
        var counter = 0  //Contador para manejar la introducción de datos a la lista.

        db.collection("users").whereEqualTo("seccion", selectedSection).get()
            .addOnSuccessListener { userQuery -> //Consulta para saber cuantos empleados hay en la seccion (X) de la coleccion usuarios.
                if (userQuery.size() == 0) {  //Si el tamaño de la consulta o resultado = 0, ponemos un texto por defecto.
                    binding.otherScheduleTitle.text = "Resto de turnos de ${selectedSection}"
                    binding.showOtherSchedule.text = "No hay empleados en esta sección"
                } else { //Si no comenzamos con la extracción de datos y la introducción de estos en la lista.
                    for (doc in userQuery) { //Bucle para actuar sobre cada uno de los resultados de la consulta.
                        val nameList = doc.getString("nombre")
                        val surnameList = doc.getString("apellidos")
                        val empNumLong = doc.getLong("numEmple")
                        val empNumList = empNumLong.toString()
                        Log.d("loadPickedSectionEmployees", "Consultando turno para empleado: $empNumList en la fecha $pickedDate") //Logcat para comprobar los datos extraidos.
                        db.collection("turnos").document(pickedDate)
                            .collection(empNumList)
                            .document("turno").get()
                            .addOnSuccessListener { documentSchedule -> //Ahora, por cada resultado, extraemos su turno usando su numero de empleado.
                                val scheduleDoc = documentSchedule.getString("turno") ?: "Sin turno"
                                Log.d("loadPickedSectionEmployees", "Turno encontrado: $scheduleDoc para el empleado $empNumList")  //Logcat para comprobar que el turno se ha encontrado.
                                finalSheduleList += "\n ${empNumList} - ${nameList} ${surnameList} : ${scheduleDoc}" //Metemos los datos del usuario y turno al String.
                                counter++ //Sumamos +1 al contador para que vaya controlando los usuarios de la consulta que ya se han terminado de tratar.
                                if (counter == userQuery.size()) { //Cuando el contador iguale al tamaño de la consulta, mostramos la lista ya completa.
                                    binding.otherScheduleTitle.text = "Resto de turnos de ${selectedSection}"
                                    binding.showOtherSchedule.text = finalSheduleList
                                }

                            }
                    }
                }
            }
    }

    /**
     * Esta función carga los turnos del empleado pasado como parámetro en el hashmap "employeeTurns", asignando a cada turno
     * la fecha seleccionada.
     */

    private fun loadEmployeeTurns(numEmple: String) {
        employeeTurns.clear()       //Primero limpiamos la lista por si contiene datos anteriores
        db.collection("turnos").get()
            .addOnSuccessListener { document -> //Iniciamos la recogida de datos de los turnos
                for (doc in document) {   //Creamos un bucle for para ir extrayendo y guardando datos
                    val idData = doc.id
                    val idSplitDate = idData.split("-")//Debemos recoger al fecha contenida en el id del documento y separarla en partes por los "-", para poder construir el LocalDate en orden.
                    val rebuildLocalDate = LocalDate.of(idSplitDate[2].toInt(), idSplitDate[1].toInt(), idSplitDate[0].toInt()) //Volvemos a construir el LocalDate con la fecha recogida en orden.

                    //Ahora toca sacar la palabra que define el turno:

                    db.collection("turnos").document(idData).collection(numEmple).document("turno")
                        .get().addOnSuccessListener { doc ->
                            val idTurn =
                                doc.getString("turno") //Hacemos una entrada a la base de datos para recoger el String del turno y guardarlo en una variable
                            if (idTurn != null) {       //Necesitamos contener la excepción de que idTurn no sea nulo, si no no dejará construir la siguiente linea
                                employeeTurns[rebuildLocalDate] = idTurn //Asignamos el valor a la fecha en la Lista mutable (Clave->rebuildLocaldate(11-0-25) / valor-> idTurn("Mañana"))
                                binding.calendarView.notifyDateChanged(rebuildLocalDate) //Refrescamos el calendario para que se posicione sobre nuestra fecha, para posteriormente incluirla colores
                            }
                        }
                    //Aqui cargamos las sugerencias del día
                    db.collection("turnos").document(idData).collection(numEmple).document("notas")
                        .collection("public").document("notaPublica").get()
                        .addOnSuccessListener { document ->
                            val status = document.getString("estado") //Recogemos el campo "estado" de la consulta.
                            if (status != null) { //Si la variable status no está vacía
                                statusPublicEmp[rebuildLocalDate] = status  //Asignamos el status a la fecha seleccionada en el Hashmap statusPublicEmp
                                binding.calendarView.notifyDateChanged(rebuildLocalDate)  //Refrescamos el día del calendario con la fecha seleccionada.
                            }
                        }.addOnFailureListener { e ->
                            snackBar(binding.root, "Error al cargar sugerencias del día ${rebuildLocalDate}: ${e.message}")
                        }
                }
            }
        binding.calendarView.notifyDateChanged(LocalDate.now())
    }

    private fun loadSpinnerEmployee(section: String) {
        db = FirebaseFirestore.getInstance()
        db.collection("users").whereEqualTo("seccion", section).get()
            .addOnSuccessListener { document ->
                val employees = mutableListOf<String>()
                for (doc in document) {
                    name = doc.getString("nombre").toString()
                    var numEmpleLong = doc.getLong("numEmple")
                    var rol = doc.getString("rol")
                    numEmple = numEmpleLong.toString()
                    if (rol == "Jefe de sección") {
                        employees.add(" $numEmple: $name (Jefe)")
                    } else {
                        employees.add("$numEmple: $name")
                    }

                }

                val adapter = ArrayAdapter(this, R.layout.spinner_edited_item, employees)
                adapter.setDropDownViewResource(R.layout.spinner_edited_item)
                binding.spinnerEmployeeSelected.adapter = adapter
            }.addOnFailureListener {
                Snackbar.make(binding.root, "Error al cargar empleados", Snackbar.LENGTH_SHORT)
                    .show()
            }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p0?.id) {
            binding.spinnerSections.id -> {
                selectedSection = p0.getItemAtPosition(p2).toString()
                binding.otherScheduleTitle.text = ""
                binding.showOtherSchedule.text = ""
                positionSection = p2
                loadSpinnerEmployee(selectedSection)
                val sectionIcon = when (selectedSection) {
                    "Sala" -> R.drawable.salaicon
                    "Charcutería" -> R.drawable.charcuicon
                    "Pescadería" -> R.drawable.pescadoicon
                    "Frutería" -> R.drawable.frutaicon
                    "Carnicería" -> R.drawable.carneicon
                    "Panadería" -> R.drawable.panicon
                    else -> null
                }
                if (sectionIcon != null) {
                    binding.iconEmployeeSelected.setImageResource(sectionIcon)
                    binding.iconEmployeeSelected.visibility = View.VISIBLE
                } else {
                    binding.iconEmployeeSelected.visibility = View.GONE
                }
            }

            binding.spinnerEmployeeSelected.id -> {
                selectedEmployee = p0.getItemAtPosition(p2).toString()
                numEmple = selectedEmployee.split(":")[0].trim()
                loadEmployeeTurns(numEmple) //Cargamos los turnos del empleado seleccionado a traves de esta función.
                val employeeName = selectedEmployee.split(":")[1].trim()
                val isBoss = selectedEmployee.trim().endsWith("(Jefe)")
                binding.employeeSelectedText.text = employeeName

                if (isBoss) {
                    binding.crownImage.visibility = View.VISIBLE
                } else {
                    binding.crownImage.visibility = View.GONE
                }
                binding.calendarView.notifyCalendarChanged()
            }

            binding.spinnerTurns.id -> {
                turn = p0.getItemAtPosition(p2).toString()
                when (p2) {
                    0 -> binding.btnSave.isEnabled = false
                    else -> binding.btnSave.isEnabled = true
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }


    override fun onClick(v: View?) {
        val dateParts = pickedDate.split("-") //Variable para sacar los numeros de la fecha a String y poder poner posteriormente sus partes en orden (aquí estan (dia-mes-año)
        val dateForThisFunction = LocalDate.of(dateParts[2].toInt(), dateParts[1].toInt(), dateParts[0].toInt()) /*Construimos la fecha en
        el orden correcto (año,mes,día) a partir de los trozos extraidos de pickedDate para utilizarla en las consultas de los botones a la base de datos*/

        when (v?.id) {
            binding.btnSave.id -> { //Botón de guardar turnos
                val createDoc = hashMapOf("created" to true) //Variable para crear un campo en un documento vacío, ya que si no dará problemas( no se pueden crear subcolecciones en documentos con campos vacíos).
                db.collection("turnos").document(pickedDate).set(createDoc) //Creamos el documento con el campo anterior.
                val schedule = hashMapOf("turno" to turn) // Creamos una variable para guardar el turno en la futura colección
                db.collection("turnos").document(pickedDate).collection(numEmple).document("turno")
                    .set(schedule)  //Guardamos el turno  en su correspondiente documento.

                employeeTurns[dateForThisFunction] = turn //Asignamos el turno a su fecha correspondiente del hashmap "EmployeeTurns" para usarlo en el bind.
                binding.calendarView.notifyDateChanged(dateForThisFunction) //refrescamos el día del calendario con la fecha creada anteriormente.
            }

            binding.btnAccept.id -> {
                builder.setMessage("Esta acción no será reversible a menos que el empleado borre la sugerencia.¿Deseas ACEPTAR la sugeréncia?")
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog, wich ->
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("public").document("notaPublica")
                            .update("estado", "aceptado").addOnSuccessListener {
                                snackBar(binding.root, "Sugerencia aceptada.")
                                statusPublicEmp[dateForThisFunction] = "aceptado"
                                binding.calendarView.notifyDateChanged(dateForThisFunction)
                                binding.btnAccept.visibility = View.GONE
                                binding.btnReject.visibility = View.GONE
                            }.addOnFailureListener { e ->
                                snackBar(binding.root, "Error: ${e.message}")
                            }

                    }.setNegativeButton("No") { dialog, wich ->
                        dialog.dismiss()
                    }
                    .show()


            }

            binding.btnReject.id -> {
                builder.setMessage("Esta acción no será reversible a menos que el empleado borre la sugerencia.¿Deseas RECHAZAR la sugeréncia?")
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog, wich ->
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("public").document("notaPublica")
                            .update("estado", "aceptado").addOnSuccessListener {
                                snackBar(binding.root, "Sugerencia rechazada.")
                                statusPublicEmp[dateForThisFunction] = "rechazado"
                                binding.calendarView.notifyDateChanged(dateForThisFunction)

                            }.addOnFailureListener { e ->
                                snackBar(binding.root, "Error: ${e.message}")
                            }
                    }.setNegativeButton("No") { dialog, wich ->
                        dialog.dismiss()
                    }
                    .show()

            }
        }
    }
}


private fun snackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
}

/** Arreglos:
 *Si se toca un día del calendario sin seleccionar ninguna sección y empleado peta (Hay que contener u ocultar el calendario cuando no esté ninguna seccion seleccionada).
 * Solo se muestran las notas publicas de danida (ni idea de por que...)
 */