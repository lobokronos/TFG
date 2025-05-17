package com.example.proyectofinal

/**
 * Completada
 *
 * Falta el comentario OK
 * Falta arreglar las vistas de los container OK
 * Falta que seaparezca el texto (sin revisar) de la sugerencia cuando se acepta o rechaza (FALTA)
 */

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import com.example.proyectofinal.databinding.ActivityEmployeeCalendarBinding
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

class EmployeeCalendarActivity : BaseActivity(), View.OnClickListener,
    AdapterView.OnItemSelectedListener {
    private lateinit var binding: ActivityEmployeeCalendarBinding
    private var selectedDate: LocalDate? = null
    private lateinit var pickedDate: String
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private lateinit var numEmple: String
    private var selectedPosition: Int = 0
    private lateinit var builder: AlertDialog.Builder
    private var employeeTurns: MutableMap<LocalDate, String> = mutableMapOf()
    private var statusPublicEmp:MutableMap <LocalDate, String> = mutableMapOf()
    private var privateNoteList:MutableMap <LocalDate, Boolean> = mutableMapOf()
    private var status:String=""
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeCalendarBinding.inflate(layoutInflater)

        val navDrawer = findViewById<FrameLayout>(R.id.content_frame)
        navDrawer.addView(binding.root)

        variables()
        buildCalendar()
        actions()
        loadUserData()
        loadEmployeeTurns()


    }

    /**
     * Acciones de los botones y Spinners
     */
    private fun actions() {
        binding.btnSave.setOnClickListener(this)
        binding.spinnerNoteType.onItemSelectedListener = this
        binding.deletePrivate.setOnClickListener(this)
        binding.deletePublic.setOnClickListener(this)

    }

    /**
     * Variables iniciadas nada mas arrancar la pantalla
     */
    private fun variables() {
        binding.notesContainer.visibility = View.INVISIBLE // Mostramos el contenedor de notas invisible nada mas iniciar la pantalla
        binding.containerShowExistPrivateNotes.visibility=View.GONE // Mostramos el visor de notas y sugerencias invisible hasta que no se pulse un día
        binding.containerShowExistPublicNotes.visibility=View.GONE
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid ?: ""
        builder = AlertDialog.Builder(this)


    }

    /**A traves de esta función generamos o cargamos el calendario completo con sus propiedades en la pantalla*/
    private fun buildCalendar() {

        val currentMonth =
            now()                                                // esta variable guarda el valor del mes actual
        val startMonth =
            currentMonth.minusMonths(100)           //Retrocede X meses para establecer el mes de inicio del calendario
        val endMonth =
            currentMonth.plusMonths(100)                 //Avanza X meses para establecer el mes final del calendario
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

        binding.calendarView.scrollPaged =
            true                                 // Permite hacer scroll entre los meses

        binding.calendarView.setup(
            startMonth,
            endMonth,
            firstDayOfWeek
        )        // Configura el calendario con el rango de meses y el primer dia de la semana)
        binding.calendarView.scrollToMonth(currentMonth)                        // Cuando se abre la pantalla, muestra el mes actual por defecto

        /**Con la llamada al metodo monthScrollListener vamos a conseguir que escuche el mes en el que esta cuando Scrolleamos. Le decimos que es un objeto de tipo
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
        /**El metodo "dayBinder" define el diseño de cada día dentro del calendario a traves de la interfáz "MonthDayBinder" en la cual accedemos a la clase
         * DayViewContainer que será la encargada de recuperar el layout donde guardamos el texto de los numeros de cada día. Al crear el "object" nos pide
         * implementar los miembros que vamos a sobreescribir "el metodo bind"*/
        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {

            /**Esta función crea el diseño de cada día*/
            override fun create(view: View): DayViewContainer = DayViewContainer(view, binding.selectedDateText,
                binding.calendarView, selectedDate, ::showNotesElements) //Asigna la vista de día a un contenedor


            /**Este método decide que mostrar en cada día del calendario utilizando el contenedor del dia actual y la fecha actual*/
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.dayNumber.text = data.date.dayOfMonth.toString()

                if (data.date.monthValue == currentMonth.monthValue) {
                    container.dayNumber.setTextColor(Color.BLACK)

                } else {
                    container.dayNumber.setTextColor(Color.GRAY)
                }

                //binding.notesContainer.visibility=View.INVISIBLE
                //Utilizamos la lista calve-valor que contiene la fecha/turno para ir pintando cada dia de un color cuando el método bind los construya
                val recoveredTurn = employeeTurns[data.date]
                val showPublicDate=statusPublicEmp[data.date]
                val showPrivateIcons=privateNoteList[data.date]
                val icon=container.icon
                when (recoveredTurn) {
                    "Mañana" -> { container.dayNumber.setBackgroundResource(R.drawable.turnomanana)
                    container.dayNumber.background.alpha=100}
                    "Tarde" -> { container.dayNumber.setBackgroundResource(R.drawable.tarde)
                        container.dayNumber.background.alpha=100}
                    "Fiesta" -> { container.dayNumber.setBackgroundResource(R.drawable.turnovacaciones)
                        container.dayNumber.background.alpha=100}
                    "Libre" -> { container.dayNumber.setBackgroundResource(R.drawable.turnolibre)
                        container.dayNumber.background.alpha=100}
                    else -> {
                        container.dayNumber.setBackgroundColor(Color.TRANSPARENT)
                    }
                }

                // Iremos pintando los números de los días dependiendo del estado de la sugerencia que contenga
                when(showPublicDate){
                    "pendiente"->{ container.dayNumber.setTextColor(Color.MAGENTA) }
                    "aceptado" -> { container.dayNumber.setTextColor(Color.GREEN) }
                    "rechazado" ->{ container.dayNumber.setTextColor(Color.RED) }
                }
                // Si existe un día con una nota en la variable showPrivateIcons, vuelve visible el icono.
                if (showPrivateIcons==true){
                    icon.visibility=View.VISIBLE
                }else{
                    icon.visibility=View.GONE
                }
            }
        }
    }

    /**
     * Función que muestra las notas y sugerencias que contienen los días al ser pulsados en el calendario.
     *
     * Esta función comprueba en la base de datos si existen notas en un día pulsado. Si existen, las muestra en
     * el editText TextShowPrivate o TextShowPublic haciendo aparecer sus correspondientes contenedores. Si no
     * hay notas o sugerencias, directamente oculta estos contenedores para evitar futuros errores.
     */
    private fun showNotesElements( date: LocalDate) {
        binding.notesContainer.visibility = View.VISIBLE //Cuando se pulsa un día, automáticamente se vuelve visible el selector de notas o sugerencias
        pickedDate = "${date.dayOfMonth}-${date.monthValue}-${date.year}" //construimos un LocalDate para utilizarlo en este método.
        val dbNotes = //Consulta a la base de datos para las notas privadas
            db.collection("turnos").document(pickedDate).collection(numEmple).document("notas")
        dbNotes.collection("private").document("notaPrivada").get()
            .addOnSuccessListener { document ->
                if (document.exists()) { // Si hay notas privadas...
                    val privateText = document.getString("nota") //Cogemos la nota
                    binding.containerShowExistPrivateNotes.visibility = View.VISIBLE //Hacemos visible su contenedor para mostrarla
                    binding.textShowPrivate.text = privateText // Le pasamos el texto de la nota al EditText para que sea legible
                } else {
                    binding.containerShowExistPrivateNotes.visibility = View.GONE //Si no, directamente no mostramos dicho contenedor
                }
            }
        //Hacemos exáctamente lo mismo con las notas públicas
        dbNotes.collection("public").document("notaPublica").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val publicText = document.getString("nota")
                    status = document.getString("estado").toString()
                    binding.containerShowExistPublicNotes.visibility = View.VISIBLE
                    val finalPublicText = "${publicText}"
                    binding.textShowPublic.text = finalPublicText

                    when(status){
                        "pendiente"->{
                            binding.imageResult.setImageResource(R.drawable.questionpublic)
                            binding.textPublicResult.setTextColor(Color.MAGENTA)
                            binding.deletePublic.isEnabled=true
                            binding.textPublicResult.text="Pendiente"

                        }
                        "aceptado" -> {
                            binding.imageResult.setImageResource(R.drawable.likepublic)
                            binding.textPublicResult.setTextColor(Color.GREEN)
                            binding.deletePublic.isEnabled=false
                            binding.textPublicResult.text="Aceptado"
                        }
                        "rechazado" ->{
                            binding.imageResult.setImageResource(R.drawable.dislikepublic)
                            binding.textPublicResult.setTextColor(Color.RED)
                            binding.deletePublic.isEnabled=false
                            binding.textPublicResult.text="Rechazado"

                        }
                    }
                } else {
                    binding.containerShowExistPublicNotes.visibility = View.GONE

                }
            }
    }

    private fun loadEmployeeTurns() {
        employeeTurns.clear()       //Primero limpiamos la lista por si contiene datos anteriores
        db.collection("turnos").get()
            .addOnSuccessListener { document -> //Iniciamos la recogida de datos de los turnos
                for (doc in document) {   //Creamos un bucle for para ir extrayendo y guardando datos
                    val idData = doc.id
                    val idSplitDate = idData.split("-")//Debemos recoger al fecha contenida en el id del documento y separarla en partes por los "-".
                    val idDay = idSplitDate[0].toInt()
                    val idMonth = idSplitDate[1].toInt() //Guardamos cada parte en una variable diferente
                    val idYear = idSplitDate[2].toInt()
                    val rebuildLocalDate = LocalDate.of(idYear, idMonth, idDay) //Volvemos a construir el LocalDate con la fecha recogida

                    //Ahora toca sacar la palabra que define el turno:

                    db.collection("turnos").document(idData).collection(numEmple).document("turno")
                        .get().addOnSuccessListener { doc ->
                            val idTurn = doc.getString("turno") //Hacemos una entrada a la base de datos para recoger el String del turno y guardarlo en una variable
                            if (idTurn != null) {       //Necesitamos contener la excepción de que idTurn no sea nulo, si no no dejará construir la siguiente linea
                                employeeTurns[rebuildLocalDate] = idTurn //Asignamos el valor a la fecha en la Lista mutable (Clave->rebuildLocaldate(11-0-25) / valor-> idTurn("Mañana"))
                                binding.calendarView.notifyDateChanged(rebuildLocalDate) //Refrescamos el calendario para que se posicione sobre nuestra fecha, para posteriormente incluirla colores
                            }
                        }.addOnFailureListener{e->
                            snackBar(binding.root,"Error al cargar Turno del día ${rebuildLocalDate}: ${e.message}")
                            }
                    //Aquí cargamos las sugerencias
                    db.collection("turnos").document(idData).collection(numEmple).document("notas")
                        .collection("public").document("notaPublica").get().addOnSuccessListener { document->
                            val status=document.getString("estado")
                            if(status!=null){
                                statusPublicEmp[rebuildLocalDate]=status
                                binding.calendarView.notifyDateChanged(rebuildLocalDate)
                            }
                        }.addOnFailureListener{e->
                            snackBar(binding.root, "Error al cargar sugerencias del día ${rebuildLocalDate}: ${e.message}")
                        }

                    db.collection("turnos").document(idData).collection(numEmple).document("notas")
                        .collection("private").document("notaPrivada").get().addOnSuccessListener { document->
                            if(document.exists()){
                                privateNoteList[rebuildLocalDate]=true
                                binding.calendarView.notifyDateChanged(rebuildLocalDate)
                            }
                        }.addOnFailureListener{e->
                            snackBar(binding.root, "Error al cargar anotaciones del día ${rebuildLocalDate}: ${e.message}")
                        }
                }

            }
        binding.calendarView.notifyDateChanged(LocalDate.now())
    }

    /**
     * Esta función carga los datos del usuario, los cuales son necesarios para mostrar su calendario personalizado.
     */
    private fun loadUserData() {
        db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val document = result.documents[0]
                val numEmpleLong = document.getLong("numEmple")
                numEmple = numEmpleLong.toString()
            }
        }
    }

    /**
     * Es importante recalcar que en los botones, cuando se crea o destruye cualquier tipo de nota, hay que eliminarla de su
     * correspondiente hashmap y pedirle al calendario que vuelva a recargar el día, para que añada o quite el color. Este hashmap
     * se construye a partir del día cogido del PickedDate, separandolo en trozos  guardando cada parte (día, mes y año) dentro de el.
     */
    override fun onClick(v: View?) {
        val dateParts= pickedDate.split("-") //Deconstruimos el pickedDate para reconstruir la fecha en la siguiente variable con el orden de los elementos ordenado correctamente
        val dateForThisFunction=LocalDate.of(dateParts[2].toInt(),dateParts[1].toInt(),dateParts[0].toInt())
        when (v?.id) {
            binding.btnSave.id -> {
                val text =
                    binding.editNotes.text.toString() //variable que recoge el texto del editText
                val noteStatus = "pendiente"
                val createDoc =
                    hashMapOf("generado" to "true") //Variable que introduce la fecha de creación
                val existDate = db.collection("turnos")
                    .document(pickedDate) //Variable con la ruta que apunta a la coleccion "numEmple"(no se ve)
                val existNote = db.collection("turnos").document(pickedDate).collection(numEmple)
                    .document("notas") //Variable con la ruta que apunta a la coleccion "notas"(no se ve)
                if (text.isEmpty()) {
                    snackBar(binding.root, "No puedes guardar una nota vacía")
                } else {
                    //Primero debemos comprobar que tanto la colección de la fecha seleccionada, como la de notas en su interior existen ya que
                    //no se pueden guardar documentos vacíos (es decir, solo con subcolecciones). Cada documento debe tener al menos un campo
                    //con datos en su interior.
                    existDate.get().addOnSuccessListener { date ->
                        if (!date.exists()) {
                            existDate.set(createDoc)  //Si la colección con la fecha seleccionada no existe la creamos con un campo (la fecha de creacion)
                        }
                        existNote.get().addOnSuccessListener { note ->
                            if (!note.exists()) {
                                existNote.set(createDoc) //Si la colección notas no existe la creamos con un campo (la fecha de creacion)
                            }
                            when (selectedPosition) { // Dependiendo de la posición del spinner, el boton de guardar hara una cosa u otra
                                1 -> { //Guardará una nota privada
                                    val insertPrivate =
                                        hashMapOf("nota" to text)  //Variable para insertar la nota
                                    existNote.collection("private").document("notaPrivada").get()
                                        .addOnSuccessListener { privateNote ->
                                            if (privateNote.exists()) { //Si la nota ya existe no deja guardar otra
                                                snackBar(binding.root, "Ya tienes una anotación guardada en este día")

                                            } else { //Si no, la crea
                                                existNote.collection("private").document("notaPrivada").set(insertPrivate)
                                                    .addOnSuccessListener {
                                                        binding.containerShowExistPrivateNotes.visibility = View.VISIBLE //Hacemos visible el container donde mostrará la nota
                                                        binding.textShowPrivate.text = text //Ponemos el texto de la nota en el textView del Container
                                                        binding.editNotes.text.clear()  //Limpiamos el editText de caracteres
                                                        snackBar(binding.root,"Nota privada guardada")
                                                        privateNoteList[dateForThisFunction]=true
                                                        binding.calendarView.notifyDateChanged(dateForThisFunction)
                                                    }.addOnFailureListener { e ->
                                                        snackBar(binding.root,"Error al guardar la nota: ${e.message}")

                                                    }
                                            }
                                        }
                                }

                                2 -> { // guardará una nota pública
                                    val insertPublic = hashMapOf(
                                        "nota" to text,
                                        "estado" to noteStatus
                                    ) //Variable para introducir la nota y el estado
                                    existNote.collection("public").document("notaPublica").get()
                                        .addOnSuccessListener { privateNote ->
                                            if (privateNote.exists()) { //Si la nota ya existe no deja guardar otra
                                                snackBar(binding.root,"Ya tienes una nota guardada en este día")
                                             } else {
                                                existNote.collection("public")
                                                    .document("notaPublica")
                                                    .set(insertPublic).addOnSuccessListener {
                                                        binding.containerShowExistPublicNotes.visibility = View.VISIBLE //Hacemos visible el contenedor para mostrar las notas
                                                        val newPublicText = "${text}" //Juntamos la nota en una variable
                                                        binding.textShowPublic.text = newPublicText  //Asignamos la variable anterior al textView para que se vea la nota
                                                        binding.editNotes.text.clear()  //Borramos el editText de caracteres
                                                        snackBar(binding.root,"Nota guardada")
                                                        statusPublicEmp[dateForThisFunction]="pendiente" //Con esto guardamos el valor pendiente en la fecha del hashmap
                                                        binding.calendarView.notifyDateChanged(dateForThisFunction) //actualizamos el calendario con la fecha que hemos seleccionado para que muestre el color

                                                    }.addOnFailureListener { e ->
                                                        snackBar(binding.root,"Error al guardar la nota: ${e.message}")
                                                    }
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }
            binding.deletePrivate.id -> {
                //Primero generamos un dialogo al pulsar el boton como medida de seguridad, para evitar que el usuario borre
                //por error una nota. Si en el dialogo se pulsa si, se borra, si no se cierra y no pasa nada.
                builder.setMessage("¿Estas seguro de querer borrar esta nota?")
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog,wich -> //Generamos la lógica que actuará cuando se pulse el SI
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("private").document("notaPrivada").delete()
                            .addOnSuccessListener { //Si está correcto
                                snackBar(binding.root,"Anotación borrada con éxito") //Informamos
                                privateNoteList.remove(dateForThisFunction) //Borramos del array de notas privadas la que contenga dicha fecha
                                binding.calendarView.notifyDateChanged(dateForThisFunction) //Refrescamos el día con la fecha seleccionada.
                            }.addOnFailureListener { e ->
                                snackBar(binding.root,"Error: ${e.message}")
                            }
                        //Y despues de borrarla ocultamos el contenedor de las notas privadas ya que ahora no existe ninguna nota
                        binding.containerShowExistPrivateNotes.visibility = View.GONE
                    }.setNegativeButton("No") { dialog, wich -> //Si pulsamos no, directamente cerramos el dialogo.
                        dialog.dismiss()
                    }
                    .show()
            }
// Aquí hacemos exáctamente lo mismo que en el botón anterior, solo que con las notas públicas.
            binding.deletePublic.id -> {
                builder.setMessage("¿Estas seguro de querer borrar esta nota?")
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog, wich ->
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("public").document("notaPublica").delete()
                            .addOnSuccessListener {
                                snackBar(binding.root,"Anotación borrada con éxito")
                                statusPublicEmp.remove(dateForThisFunction)
                                binding.calendarView.notifyDateChanged(dateForThisFunction)
                            }.addOnFailureListener { e ->
                                snackBar(binding.root,"Error: ${e.message}")
                            }
                        binding.containerShowExistPublicNotes.visibility = View.GONE
                    }.setNegativeButton("No") { dialog, wich ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    /**
     * En este onItemSelected se juega con el spinner de selección de notas o sugerencias para mostrar unos elementos
     * u otros dependiendo del item seleccionado
     */
    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedPosition = p2
        //Dependiendo de la posición del Spinner...
        when (selectedPosition) {
            0 -> { // Si es 0 dirrectamente no mostramos el contenedor entero
                binding.noteContainerSelected.visibility = View.INVISIBLE
            }
            // Si se selecciona la privada, se muestra el contenedor y se personalizan sus elementos para que muestren advertencias y mensajes referentes a las notas privadas
            1 -> {
                binding.noteContainerSelected.visibility = View.VISIBLE
                binding.typeTitleText.text = getString(R.string.privateNote)
                binding.noteInstructionsText.text = getString(R.string.instructionsPrivate)
            }
            // Si se selecciona la sugerencia, se muestra el contenedor y se personalizan sus elementos para que muestren advertencias y mensajes referentes a las sugerencias
            2 -> {
                binding.noteContainerSelected.visibility = View.VISIBLE
                binding.typeTitleText.text = getString(R.string.publicNote)
                binding.noteInstructionsText.text = getString(R.string.instructionsPublic)
            }
        }
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {}
}

/**
 * Método para los Snackbar
 */
private fun snackBar(view:View, message:String){
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
}