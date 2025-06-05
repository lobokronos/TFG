package com.example.proyectofinal

/**
 * Completada
 *
 */

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.children
import com.example.proyectofinal.databinding.ActivityCalendarBinding
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
    private lateinit var binding: ActivityCalendarBinding // Variable para el binding
    private var selectedDate: LocalDate? = null
    private lateinit var db: FirebaseFirestore // Variable para realizar la conexion con Firebase
    private lateinit var auth: FirebaseAuth  // Variable para realizar la conexion con Authentication
    private lateinit var pickedDate: String //Guardará la fecha seleccionada en String (dia-mes-año) para utilizarla en funciones
    private lateinit var name: String // Variable para guardar el nombre de usuario
    private lateinit var numEmple: String // Variable para guardar el número de empleado del usuario
    private lateinit var turn: String // Variable para guardar el turno seleccionado del spinner de turnos (deberia ser schedule, está mal escrita)
    private var positionSection: Int = 0 //Variable para guardar la POSICION del spinner de secciones
    private var employeeTurns: MutableMap<LocalDate, String> = mutableMapOf() // Variable tipo lista o clave valor para guardar los turnos de los empleados en dias concretos
    private var statusPublicEmp: MutableMap<LocalDate, String> = mutableMapOf() // Variable tipo clave-valor para guardar el estado de las sugerencias de los dias en concreto
    private lateinit var selectedSection: String // Variable para guardar el NOMBRE de la sección seleccionada desde el spinner de secciones
    private lateinit var selectedEmployee: String //Variable para guardar el empleado seleccionado desde el spinner de empleados
    private var status: String = "" // Variable para recoger el estado de las sugerencias creadas (valor vacío por defecto)
    private lateinit var builder: AlertDialog.Builder // Variable para construir los alertDialogs
    private var firstSection =
        false //Variable para recoger si se ha pulsado ya una primera seccion (esconder Calendario).
    private var firstEmployee =
        false //Variable para recoger si se ha pulsado ya un primer empleado (esconder Calendario).

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)

        val navDrawer =
            findViewById<FrameLayout>(R.id.content_frame) // buscamos la vista del frameLayout donde vamos a cargar esta activity
        navDrawer.addView(binding.root)// Añadimos esta vista de la activity a la del menú

        binding.calendarView.visibility =
            View.GONE // Nada mas empezar, mostramos el calendario oculto


        variables()
        actions()
        buildCalendar() // Iniciamos nada mas empezar la construcción del calendario
        binding.allContainer.visibility =
            View.GONE //Ocultamos el Layout donde se encuentran el selector de turnos y las sugerencias
        binding.legendLayout.visibility = View.GONE //Ocultamos la leyenda
        binding.btnLegend.visibility = View.GONE //Ocultamos el botón de la leyenda
    }

    /**
     * Función que recoge las acciones de los elementos
     */
    private fun actions() {
        binding.spinnerSections.onItemSelectedListener = this
        binding.spinnerEmployeeSelected.onItemSelectedListener = this
        binding.btnSave.setOnClickListener(this)
        binding.btnAccept.setOnClickListener(this)
        binding.btnReject.setOnClickListener(this)
        binding.spinnerTurns.onItemSelectedListener = this
        binding.btnLegend.setOnClickListener(this)
        binding.btnExitLegend.setOnClickListener(this)

    }

    /**
     * Función que recoge las inicializaciones de las variables
     */
    private fun variables() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        builder = AlertDialog.Builder(this)

    }

    /**
     * Función para construir el calendario.
     *
     * A traves de esta función generamos o cargamos el calendario completo con sus propiedades en la pantalla*/
    private fun buildCalendar() {

        val currentMonth =
            now()                                                // esta variable guarda el valor del mes actual
        val startMonth =
            currentMonth.minusMonths(100)           //Retrocede X meses para establecer el mes de inicio del calendario
        val endMonth =
            currentMonth.plusMonths(100)                 //Avanza X meses para establecer el mes final del calendario
        val firstDayOfWeek =
            firstDayOfWeekFromLocale()                         // Obtiene el primer día de la semana segun la configuracion horaria del dispositivo (Lunes)

        val titlesContainer =
            findViewById<ViewGroup>(R.id.titlesContainer) //Captura el contenedor titlesContainer(Titulos de los dias) en una variable
        titlesContainer.children // recoge una secuencia de todas las vistas hijas de los titulos (son 7).
            .map { it as TextView } //recorre la secuencia y cada elemento lo pasa a textView
            .forEachIndexed { index, textView -> //forEachIndexed recorre la lista y nos da el TextView y el Indice de cada elemento.
                val dayOfWeek =
                    daysOfWeek()[index] //daysOfWeek asigna el dia correspondiente con el valor del índice.
                val title = dayOfWeek.getDisplayName(
                    TextStyle.SHORT,
                    Locale.getDefault()
                ) // Asignamos el titulo del día a cada título.
                textView.text = title
                if (textView.text == "dom") { // Si el textView correspondiente en el bucle es "dom":
                    textView.setTextColor(Color.WHITE) //Aparecerá con letras blancas y fondo rojo.
                    textView.setBackgroundColor(Color.RED)
                } else {
                    textView.setTextColor(Color.WHITE) // Si no, el color de fonjdo será azúl
                    textView.setBackgroundColor(Color.BLUE)
                }
            }

        binding.calendarView.scrollPaged =
            true                                 // Permite hacer scroll entre los meses
        binding.calendarView.setup(
            startMonth,
            endMonth,
            firstDayOfWeek
        )        // Configura el calendario con el rango de meses y el primer dia de la semana)
        binding.calendarView.scrollToMonth(currentMonth)                        // Cuando se abre la pantalla, muestra el mes actual por defecto

        val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
        binding.monthTitle.text = monthName
        /**
         * Método monthScrollListener y override invoke
         *
         * Con la llamada al metodo monthScrollListener vamos a conseguir que escuche el mes en el que esta cuando hacemos scroll. Le decimos que es un objeto de tipo
         * MonthScrollListener. Al hacer esto, nos pide implementar los miembros "invoke" para sobreescribir esta funcion. En la funcion invoke recogemos el texto de
         * el mes correspondiente actual y se lo pasamos al textView donde lo mostrará. Es importante usar getDisplayName(TextStyle.FULL, Locale.getDefault()) ya que
         * este metodo nos devuelve el nombre del mes en Castellano. Podriamos utilizar el de java.time que sería "monthName=month.yearMonth.month.name.lowercase()
         * .replaceFirstChar { it.uppercase() }" pero nos lo devolvería en Inglés.
         */
        binding.calendarView.monthScrollListener = object : MonthScrollListener {
            override fun invoke(month: CalendarMonth) {
                val monthName =
                    month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .uppercase()
                binding.monthTitle.text = monthName
            }
        }
        /**
         * Método dayBinder
         *
         * El metodo "dayBinder" define el diseño de cada día dentro del calendario a traves de la interfáz
         * "MonthDayBinder" en la cual accedemos a la clase DayViewContainer que será la encargada de recuperar
         * el layout donde guardamos el texto de los numeros de cada día. Al crear el "object" nos pide
         * implementar los miembros que vamos a sobreescribir "el metodo bind"*/
        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            /**
             * Función create
             * Esta función crea el diseño de cada día*/
            override fun create(view: View): DayViewContainer =
                DayViewContainer(view) //Asigna la vista de día a un contenedor

            /**
             * Función bind
             *
             * Este método que muestra el contenido de un "adapter". Configura la vista de cada día. Container se encarga de recoger las vistas
             * de los días y data posee los datos de una fecha seleccionada en el calendario.
             * */
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day =
                    data //Esta variable recoge toda la información obtenida en el DayViewContainer al seleccionar un día.
                container.dayNumber.text =
                    data.date.dayOfMonth.toString() //Aqui asignamos el numero a la vista de cada zelda de día.

                if (data.date.monthValue == currentMonth.monthValue) { // Si el calendario muestra el mes actual, pintamos los numeros en negro
                    container.dayNumber.setTextColor(Color.BLACK)
                } else { // Si no, los pintamos en gris
                    container.dayNumber.setTextColor(Color.GRAY)
                }

                container.dayNumber.setOnClickListener { //Este "Listener" se activa cuando el usuario pulsa una celda del calendario
                    val text =
                        "${data.date.dayOfMonth}/${data.date.monthValue}/${data.date.year}" //Recogemos la fecha seleccionada y la guardamos en tipo String en una variable
                    binding.selectedDateText.text =
                        text // Pasasmos la variable que contiene la fecha al TextView donde la mostrará
                    selectedDate =
                        data.date // Guardamos en la variable local la fecha recogida en la pulsación para utilizarla despues
                    pickedDate =
                        "${data.date.dayOfMonth}-${data.date.monthValue}-${data.date.year}" //String para utilizar la fecha en futuras operaciones con la base de datos
                    showScheduleNotesItems(data.date) // Activamos el método que activa las vistas de los Layout de notas y turnos pasándole la fecha pulsada
                }
                //Utilizamos la lista calve-valor que contiene la fecha/turno para ir pintando cada dia de un color cuando el método bind los construya
                val recoveredTurn =
                    employeeTurns[data.date] // Comprobará si en la colección de los turnos se encuentra algún turno en la fecha pasada como parámetro
                val showPublicDate =
                    statusPublicEmp[data.date]  //Comprobará si en la colección de las sugerencias se encuentra algún turno en la fecha pasada como parámetro

                /**
                 * dependiendo de lso turnos recogidos en la lista recoveredTurn se mostrará la imagen correspondiente al turno
                 */
                when (recoveredTurn) {
                    "Mañana" -> {
                        container.dayNumber.setBackgroundResource(R.drawable.turnomananaopacity)
                    }

                    "Tarde" -> {
                        container.dayNumber.setBackgroundResource(R.drawable.afternoonopacity)
                    }

                    "Vacaciones" -> {
                        container.dayNumber.setBackgroundResource(R.drawable.turnovacacionesopacity)
                    }

                    "Libre" -> {
                        container.dayNumber.setBackgroundResource(R.drawable.turnolibreopacity)
                    }

                    "Borrar turno" -> {
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("turno").delete()
                        container.dayNumber.setBackgroundColor(Color.TRANSPARENT)
                    }

                    else -> {
                        container.dayNumber.setBackgroundColor(Color.TRANSPARENT)
                    }
                }

                /**
                 * Dependiendo de los estados de las sugerencias recogidas en la lista showPublicDate
                 * se mostraran unos u otros colores.
                 */
                when (showPublicDate) {
                    "pendiente" -> {
                        container.dayNumber.setTextColor(Color.MAGENTA)
                        binding.btnAccept.visibility = View.VISIBLE
                        binding.btnReject.visibility = View.VISIBLE
                    }

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
     * Función showScheduleNotesItems que carga elementos segun se pulse un día.
     *
     * Esta función se activa cuando se pulsa un día a traves del DayViewContainer. Carga las sugerencias de los empleados
     * seleccionados en caso de que las haya, muestra los contenedores correspondientes si las hay, y llama a la función
     * loadPickedSectionEmployees() para mostrar la lista turnos de los empleados de la sección.
     */
    private fun showScheduleNotesItems(date: LocalDate) {
        binding.btnLegend.visibility = View.VISIBLE
        binding.allContainer.visibility = View.VISIBLE //
        pickedDate =
            "${date.dayOfMonth}-${date.monthValue}-${date.year}" //String para mostrar en TextViews la fecha (día, mes, año)
        val dbNotes =
            db.collection("turnos").document(pickedDate).collection(numEmple).document("notas")
        dbNotes.collection("public").document("notaPublica")
            .get() //HAcemos una consulta en la BBDD para saber el estado de la nota Publica.
            .addOnSuccessListener { document ->
                if (document.exists()) { // Si el documento existe
                    val publicText = document.getString("nota")
                    status = document.getString("estado")
                        .toString() //Se recoge el estado de la sugerencia
                    binding.publicNotesContainer.visibility =
                        View.VISIBLE //Y se muestra el contenedor de las  notas
                    when (status) { // dependiendo del valor del estado de las sugerencias...
                        "pendiente" -> { // Si esta en "pendiente", se muestra el contenido, los botones de aceptar o rechazar, el estado de la sugerencia y la imagen correspondiente
                            val finalPublicText = "${publicText} "
                            binding.btnAccept.visibility = View.VISIBLE
                            binding.btnReject.visibility = View.VISIBLE
                            binding.publicNoteText.text = finalPublicText
                            binding.imageResult.setImageResource(R.drawable.questionpublic)
                            binding.textResult.text = status
                        }

                        "aceptado" -> { // Si está aceptada, se muestra el contenido de la sugerencia, el estado y la imagen correspondiente. También se ocultan los botones de aceptar o rechazar
                            val finalPublicText = "${publicText}"
                            binding.btnAccept.visibility = View.GONE
                            binding.btnReject.visibility = View.GONE
                            binding.publicNoteText.text = finalPublicText
                            binding.imageResult.setImageResource(R.drawable.likepublic)
                            binding.textResult.text = status

                        }

                        "rechazado" -> { // Pasa lo mismo que cuando la sugerencia está aceptada, solo que aquí se muestran los elementos correspondientes a cuando el estado es rechazado
                            val finalPublicText = "${publicText}"
                            binding.publicNoteText.text = finalPublicText
                            binding.imageResult.setImageResource(R.drawable.dislikepublic)
                            binding.textResult.text = status
                            binding.btnAccept.visibility = View.GONE
                            binding.btnReject.visibility = View.GONE
                        }
                    }

                } else { // Si no hay notas, se oculta el contenedor de s sugerencias
                    binding.publicNotesContainer.visibility = View.GONE
                }
                loadPickedSectionEmployees() //y llamamos a la función que muestra los turnos del resto de la sección
            }
    }

    /**
     * Función loadPickedSectionEmployees para cargar los turnos de los empleados de una sección y mostrarlos
     *
     * Esta función se encarga de recoger los empleados existentes y sus turnos, y recogerlos en un TextView cuando se
     * seleccione una seccion en su Spinner correspondiente
     */
    private fun loadPickedSectionEmployees() {
        var finalSheduleList = "" //Variable donde vamos a guardar la lista de empleados y turnos.
        var counter = 0  //Contador para manejar la introducción de datos a la lista.

        db.collection("users").whereEqualTo("seccion", selectedSection).get()
            .addOnSuccessListener { userQuery -> //Consulta para saber cuantos empleados hay en la seccion (X) de la coleccion usuarios.
                if (userQuery.size() == 0) {  //Si el tamaño de la consulta o resultado = 0, ponemos un texto por defecto.
                    binding.otherScheduleTitle.text =
                        "Resto de turnos de ${selectedSection}" // Título de esta sección
                    binding.showOtherSchedule.text =
                        "No hay empleados en esta sección" // Si no hay empleados en esa sección, se muesrta este mensaje
                } else { //Si no comenzamos con la extracción de datos y la introducción de estos en la lista.
                    for (doc in userQuery) { //Bucle para ir guardando cada uno de los valores mostrados a continuación en cada uno de los resultados encontrados
                        val nameList = doc.getString("nombre")
                        val surnameList = doc.getString("apellidos")
                        val rolList = doc.getString("rol")
                        val empNumLong = doc.getLong("numEmple")
                        val empNumList = empNumLong.toString()
                        db.collection("turnos").document(pickedDate)
                            .collection(empNumList)
                            .document("turno").get()
                            .addOnSuccessListener { documentSchedule -> //Ahora, por cada resultado, extraemos su turno usando su numero de empleado. Si el empleado es jefe de sección, tambien lo recogemos
                                val scheduleDoc = documentSchedule.getString("turno") ?: "Sin turno"
                                val sectionBossText =
                                    if (rolList == "Jefe de sección") "(Jefe)" else ""

                                finalSheduleList += "\n ${empNumList} - ${nameList} ${surnameList} ${sectionBossText} : ${scheduleDoc}" //Metemos los datos del usuario y turno al String.
                                counter++ //Sumamos +1 al contador para que vaya controlando los usuarios de la consulta que ya se han terminado de tratar.
                                if (counter == userQuery.size()) { //Cuando el contador iguale al tamaño de la consulta, mostramos la lista ya completa.
                                    binding.otherScheduleTitle.text =
                                        "Turnos de ${selectedSection}" // Titulo de la sección
                                    binding.showOtherSchedule.text =
                                        finalSheduleList //Mostramos el String construido con todos los resultados de los empleados encontrados
                                }

                            }
                    }
                }
            }
    }

    /**
     * Función loadEmployeeTurns la cual carga los turnos del empleado seleccionado
     *
     * Esta función carga los turnos del empleado pasado como parámetro en el muatbleMap "employeeTurns", asignando a cada turno
     * la fecha seleccionada. Es importante recalcar que ha de limpiarse los datos de los hashMap primero, ya que si no
     * mostrará siempre los mismos turnos y notas.
     */
    private fun loadEmployeeTurns(numEmple: String) {
        employeeTurns.clear()       //Primero limpiamos la lista por si contiene datos anteriores.
        statusPublicEmp.clear()     //Tambien limpiamos la lista de anotaciones (he tardado un muy buen rato en darme cuenta de que faltaba esta linea...).
        db.collection("turnos").get()
            .addOnSuccessListener { document -> //Iniciamos la recogida de datos de los turnos
                for (doc in document) {   //Creamos un bucle for para ir extrayendo y guardando datos
                    val idData = doc.id
                    val idSplitDate =
                        idData.split("-")//Debemos recoger al fecha contenida en el id del documento y separarla en partes por los "-", para poder construir el LocalDate en orden.
                    val rebuildLocalDate = LocalDate.of(
                        idSplitDate[2].toInt(),
                        idSplitDate[1].toInt(),
                        idSplitDate[0].toInt()
                    ) //Volvemos a construir el LocalDate con la fecha recogida en orden.

                    //Ahora toca sacar la palabra que define el turno:

                    db.collection("turnos").document(idData).collection(numEmple).document("turno")
                        .get().addOnSuccessListener { doc ->
                            val idTurn =
                                doc.getString("turno") //Hacemos una entrada a la base de datos para recoger el String del turno y guardarlo en una variable
                            if (idTurn != null) {       //Necesitamos contener la excepción de que idTurn no sea nulo, si no no dejará construir la siguiente linea
                                employeeTurns[rebuildLocalDate] =
                                    idTurn //Asignamos el valor a la fecha en la Lista mutable (Clave->rebuildLocaldate(11-0-25) / valor-> idTurn("Mañana"))
                                binding.calendarView.notifyDateChanged(rebuildLocalDate) //Refrescamos el calendario para que se posicione sobre nuestra fecha, para posteriormente asignarle imágenes
                            }
                        }
                    //Aqui cargamos las sugerencias del día
                    db.collection("turnos").document(idData).collection(numEmple).document("notas")
                        .collection("public").document("notaPublica").get()
                        .addOnSuccessListener { document ->
                            val status =
                                document.getString("estado") //Recogemos el campo "estado" de la consulta.
                            if (status != null) { //Si la variable status no está vacía
                                statusPublicEmp[rebuildLocalDate] =
                                    status  //Asignamos el status a la fecha seleccionada en el Hashmap statusPublicEmp
                                binding.calendarView.notifyDateChanged(rebuildLocalDate)  //Refrescamos el día del calendario con la fecha seleccionada.
                            }
                        }.addOnFailureListener { e ->
                            snackBar(
                                binding.root,
                                "Error al cargar sugerencias del día ${rebuildLocalDate}: ${e.message}"
                            )
                        }
                }
            }
        //binding.calendarView.notifyDateChanged(LocalDate.now()) // recarga la fecha actual para evitar errores de carga mientras se ejecutan las llamadas asíncronas anteriores
    }

    /**
     * Funcion loadSpinnerEmployee que rellena de empleados el spinner dependiendo de la sección seleccionada. Esta se activa cada vez
     * que un item del spinner es selecionado
     */
    private fun loadSpinnerEmployee(section: String) {
        Log.d(
            "parametroSección",
            "Parámetro section: '$section'"
        ) //Log realizado para comprobar que se pasaba como parámetro ya que me daba error los espacios de (-----)
        db.collection("users").whereEqualTo("seccion", section).get()
            .addOnSuccessListener { document -> //Hacemos una consulta a la coleccion "users" y sacaremos su numEmple, nombre y rol
                val employees = mutableListOf<String>() //Creamos una lista donde recogeremos todos los datos obtenidos para luego mostrarla en el adapter (como en clase)
                if (document.isEmpty) { //Si el documento está vacío puede sedr por dos motivos:
                    if (section == "-----") { // Porque se seleccione la primera posición del spinner (-----)...Evidentemente aquí no hay resultados que mostrar
                        binding.employeeSelectedText.text =
                            "" //Ponemos el titulo del texto vacío para que al iniciar la activity no muestre nada

                    } else {
                        binding.employeeSelectedText.text = "${selectedSection} (vacío)" /*Si el documento está vacío ( la sección) pero se ha seleccionado una sección
                        mostrará el nombre de la sección y vacío.*/
                        binding.calendarView.visibility = View.GONE //Se ocultan los elementos correspondientes para que no de error
                        binding.allContainer.visibility = View.GONE
                        binding.publicNotesContainer.visibility = View.GONE
                        binding.selectedDateText.text = ""
                        binding.showOtherSchedule.text = ""
                        binding.otherScheduleTitle.text = ""
                    }
                }
                for (doc in document) { //Ahora recorremos el documento para sacar los datos requeridos y añadirlos a la Lista para el adapter.
                    name = doc.getString("nombre").toString()
                    var numEmpleLong = doc.getLong("numEmple")
                    var rol = doc.getString("rol")
                    numEmple = numEmpleLong.toString()
                    if (rol == "Jefe de sección") { //Si es jefe de sección lo indicamos en su string para que quede reflejado
                        employees.add(" $numEmple: $name (Jefe)")
                    } else { //Si no, solo numero y nombre
                        employees.add("$numEmple: $name")
                    }
                }

                // Ahora damos forma al spinner con un elemento editado, ya que, al ser "dinamico" no dejaba editarlo de manera simplificada desde el layout.
                val employeeAdapter = ArrayAdapter(this, R.layout.spinner_edited_item, employees)
                employeeAdapter.setDropDownViewResource(R.layout.spinner_edited_item)
                binding.spinnerEmployeeSelected.adapter = employeeAdapter
            }.addOnFailureListener {
                Snackbar.make(binding.root, "Error al cargar empleados", Snackbar.LENGTH_SHORT)
                    .show()
            }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p0?.id) {
            /**
             * Spinner de secciones
             *
             * En este Spinner se recogen los nombres de las secciones a traves de sus posiciones para pasarselas
             * a la variable "selectedSection" y poder utilizarla como parámetro en la función loadSpinnerEmployee()
             * para cargar los empleados de esa sección. También se obliga a los textos de la lista de turnos del resto
             * de empleados a mostrarse vacíos cada vez que se selecciona una nueva sección para que quede mas visual.
             * Además, se usa esta variable para cargar imágenes según la seccion seleccionada.
             */
            binding.spinnerSections.id -> {
                selectedSection = p0.getItemAtPosition(p2)
                    .toString() //Variable que guarda el String de la sección seleccionada
                Log.d("SECCION", "Sección seleccionada: '$selectedSection'")
                binding.otherScheduleTitle.text =
                    "" //Ocultamos los textos de la lista del resto de turnos
                binding.showOtherSchedule.text = ""
                positionSection = p2 //capturamos la posicion en Int del elemento seleccionado.
                firstSection =
                    true //Actualizamos la variable que esconde el calendario ya que ya hemos seleccionado la primera seccion.
                loadSpinnerEmployee(selectedSection) //Llamada a la función de carga del spinner de los empleados.
                val sectionIcon =
                    when (selectedSection) { //Condicional para cargar imágenes según la sección.
                        "Sala" -> R.drawable.salaicon
                        "Charcutería" -> R.drawable.charcuicon
                        "Pescadería" -> R.drawable.pescadoicon
                        "Frutería" -> R.drawable.frutaicon
                        "Carnicería" -> R.drawable.carneicon
                        "Panadería" -> R.drawable.panicon
                        else -> null
                    }
                if (sectionIcon != null) { //Si no es nulo, mostramos la imagen.
                    binding.iconEmployeeSelected.setImageResource(sectionIcon)
                    binding.iconEmployeeSelected.visibility = View.VISIBLE
                } else { //Si es nulo, no.
                    binding.iconEmployeeSelected.visibility = View.GONE
                }

            }

            /**
             * Spinner de empleados (dinámico)
             *
             * En este spinner se recoge el string del numero de empleado seleccionado  para llamar a la función
             * loadEmployeeTurns(numEmple) y cargar los turnos en el calendario. Se ha intentado que aparezca una
             * corona al lado del nombre del empleadosi es jefe. Falta revisarlo. Una vez terminada la selección del
             * spinner, se refresca el calendario para que muestre los turnos nuevos.
             */
            binding.spinnerEmployeeSelected.id -> {
                selectedEmployee = p0.getItemAtPosition(p2)
                    .toString() // Se recoge el string del empleado seleccionado.

                numEmple =
                    selectedEmployee.split(":")[0].trim() //Se separa el numero de empleado de toda la cadena.
                loadEmployeeTurns(numEmple) //Cargamos los turnos del empleado seleccionado a traves de esta función.
                val employeeName =
                    selectedEmployee.split(":")[1].trim() //Se separa el empleado de toda la cadena.
                firstEmployee = true
                binding.employeeSelectedText.text =
                    employeeName //Se muestra en el textView el nombre del empleado.


                binding.calendarView.notifyCalendarChanged() //Redibujamos el calendario completo ya que se ha cambiado de usuario seleccionado para que el método bind() vuelva a redibujar sus turnos y demas
            }

            /**
             * Spinner de los turnos
             *
             * Este es sencillo: Si no se ha seleccionado ningún turno, se deshabilita el botón de guardar para
             * evitar posibles errores.
             */
            binding.spinnerTurns.id -> {
                turn = p0.getItemAtPosition(p2).toString()
                when (p2) {
                    0 -> binding.btnSave.isEnabled = false
                    else -> binding.btnSave.isEnabled = true
                }
            }
        }
        if (firstSection && firstEmployee) { //Ahora, si ya hemos seleccionado una sección y un empleado...
            binding.calendarView.visibility = View.VISIBLE //...activamos el calendario.
        }
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    /**
     * Lógica de los botones
     */
    override fun onClick(v: View?) {
        val dateParts =
            pickedDate.split("-") //Variable para sacar los numeros de la fecha a String y poder poner posteriormente sus partes en orden (aquí estan (dia-mes-año)
        val dateForThisFunction =
            LocalDate.of(dateParts[2].toInt(), dateParts[1].toInt(), dateParts[0].toInt()) /*Construimos la fecha en
        el orden correcto (año,mes,día) a partir de los trozos extraidos de pickedDate para utilizarla en las consultas de los botones a la base de datos*/

        when (v?.id) {
            /**
             * Botón de guardar Turnos
             *
             * Este botón se encarga de guardar los turnos que el jefe elige por cada dia de un empleado. Hay
             * que crear un campo ("created" por ejemplo) en cada documento nuevo si este va a contener subcolecciones,
             * ya que de no ser así, este documento aparecerá en cursiva en Firestore, lo que indica que es un documento
             * inexistente y no tendra validez ni funcionalidad. Una vez hecho el documento, se procede a guardar el
             * turno en su coleccion correspondiente (en la fecha seleccionada y con el numero de empleado correspondiente.
             */
            binding.btnSave.id -> { //Botón de guardar turnos
                val createDoc =
                    hashMapOf("created" to true) //Variable para crear un campo en un documento vacío, ya que si no dará problemas( no se pueden crear subcolecciones en documentos con campos vacíos).
                db.collection("turnos").document(pickedDate)
                    .set(createDoc) //Creamos el documento con el campo anterior.
                val schedule =
                    hashMapOf("turno" to turn) // Creamos una variable para guardar el turno en la futura colección
                db.collection("turnos").document(pickedDate).collection(numEmple).document("turno")
                    .set(schedule)  //Guardamos el turno  en su correspondiente documento.

                employeeTurns[dateForThisFunction] =
                    turn //Asignamos el turno a su fecha correspondiente del hashmap "EmployeeTurns" para usarlo en el bind.
                binding.calendarView.notifyDateChanged(dateForThisFunction) //refrescamos el día del calendario con la fecha creada anteriormente.
                loadPickedSectionEmployees()
            }

            /**
             * Botón de aceptar sugerencia
             *
             * Aquí metemos un Dialogo de alerta para indicar que, una vez que se acepte la sugerencia, esta no será reversible,
             * es decir, no se podrá luego rechazar. Si el jefe pulsa en aceptar, actualiza el estado de la sugerencia en la base
             * de datos a "aceptado) y refresca el día del calendario para que muestre su color correspondiente. También se ocultan
             * los botones para que el jefe no pueda reescribir el estado de la nota.
             */
            binding.btnAccept.id -> {
                builder.setMessage("Esta acción no será reversible a menos que el empleado borre la sugerencia.¿Deseas ACEPTAR la sugeréncia?")
                    .setTitle("¡Atención!")
                    .setPositiveButton("Si") { dialog, wich -> //Dialogo de advertencia.
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("public").document("notaPublica")
                            .update("estado", "aceptado")
                            .addOnSuccessListener { //ruta de acceso a la BBDD para actualizar el estado.
                                snackBar(binding.root, "Sugerencia aceptada.")
                                statusPublicEmp[dateForThisFunction] =
                                    "aceptado"  //Lista donde asociamos el estado a la fecha seleccionada para luego poder mostrar el color.
                                binding.calendarView.notifyDateChanged(dateForThisFunction) //refrescamos el día del calendario utilizando la fecha construida a partir del string.
                                binding.btnAccept.visibility = View.GONE //Ocultamos los botones.
                                binding.btnReject.visibility = View.GONE
                                binding.imageResult.setImageResource(R.drawable.likepublic)
                                binding.textResult.text = "aceptado"
                            }.addOnFailureListener { e ->
                                snackBar(binding.root, "Error: ${e.message}")
                            }
                    }.setNegativeButton("No") { dialog, wich -> //Si no, se cierra el dialogo.
                        dialog.dismiss()
                    }
                    .show()
            }

            /**
             * Botón de rechazar
             *
             * Sigue la misma lógica del botón anterior, solo que este actualiza el estado a "rechazado" para que luego
             * se pinte el día de rojo.
             */
            binding.btnReject.id -> {
                builder.setMessage("Esta acción no será reversible a menos que el empleado borre la sugerencia.¿Deseas RECHAZAR la sugeréncia?")
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog, _ ->
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("public").document("notaPublica")
                            .update("estado", "rechazado").addOnSuccessListener {
                                snackBar(binding.root, "Sugerencia rechazada.")
                                statusPublicEmp[dateForThisFunction] = "rechazado"
                                binding.calendarView.notifyDateChanged(dateForThisFunction)
                                binding.imageResult.setImageResource(R.drawable.dislikepublic)
                                binding.textResult.text = "rechazado"

                            }.addOnFailureListener { e ->
                                snackBar(binding.root, "Error: ${e.message}")
                            }
                    }.setNegativeButton("No") { dialog, wich ->
                        dialog.dismiss()
                    }
                    .show()

            }

            /** Botón para mostrar la leyenda **/
            binding.btnLegend.id -> {
                binding.legendLayout.visibility = View.VISIBLE
            }

            /** Botón para ocultar la leyenda**/
            binding.btnExitLegend.id -> {
                binding.legendLayout.visibility = View.GONE
            }

        }
    }


    private fun snackBar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Utilizamos onResume (ciclo de vida de la pantalla) para recargar el spinner de los empleados
     * una vez se borren o agreguen estos a la base de datos cuando volvemos de la pantalla
     * AddQuitEmployeeActivity.kt
     */
    override fun onResume() {
        super.onResume()
        if (firstSection) { //Si ya estaba seleccionada una sección en el spinner...
            loadSpinnerEmployee(selectedSection) // Se le fuerza la recarga de sus empleados
        }
    }
}


