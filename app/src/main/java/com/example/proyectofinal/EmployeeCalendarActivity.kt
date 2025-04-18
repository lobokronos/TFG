package com.example.proyectofinal

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.FieldValue
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
    private lateinit var employeeName: String
    private lateinit var numEmple: String
    private var selectedPosition: Int = 0
    private lateinit var builder: AlertDialog.Builder
    private var employeeTurns: MutableMap<LocalDate, String> = mutableMapOf()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeCalendarBinding.inflate(layoutInflater)

        val frameContent = findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)

        variables()
        buildCalendar()
        actions()
        loadUserData()
        loadEmployeeTurns()


    }

    private fun actions() {
        binding.btnSave.setOnClickListener(this)
        binding.spinnerNoteType.onItemSelectedListener = this
        binding.deletePrivate.setOnClickListener(this)
        binding.deletePublic.setOnClickListener(this)

    }

    private fun variables() {
        binding.notesContainer.visibility = View.INVISIBLE
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

        val titlesContainer = findViewById<ViewGroup>(R.id.titlesContainer)
        val children = titlesContainer.children.toList()

        for (i in children.indices) {
            val textView = children[i] as TextView
            val dayOfWeek = daysOfWeek()[i]
            val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            textView.text = title
            if (textView.text == "dom") {
                textView.setTextColor(Color.WHITE)
                textView.setBackgroundColor(Color.RED)
            } else {
                textView.setTextColor(Color.WHITE)
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
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(
                    view,
                    binding.selectedDateText,
                    binding.calendarView,
                    selectedDate,
                    ::showNotesElements
                )                                   //Asigna la vista de día a un contenedor
            }


            /**Este método decide que mostrar en cada día del calendario utilizando el contenedor del dia actual y la fecha actual*/
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.dayNumber.text = data.date.dayOfMonth.toString()

                if (data.date.monthValue == currentMonth.monthValue) {

                    container.dayNumber.setTextColor(Color.BLACK)
                } else {
                    container.dayNumber.setTextColor(Color.GRAY)
                }

                binding.notesContainer.visibility=View.INVISIBLE
                //Utilizamos la lista calve-valor que contiene la fecha/turno para ir pintando cada dia de un color cuando el método bind los construya
                val recoveredTurn = employeeTurns[data.date]

                when (recoveredTurn) {
                    "Mañana" -> {
                        container.dayNumber.setBackgroundColor(Color.YELLOW)
                    }

                    "Tarde" -> {
                        container.dayNumber.setBackgroundColor(Color.BLUE)
                    }

                    "Fiesta" -> {
                        container.dayNumber.setBackgroundColor(Color.RED)
                    }

                    "Libre" -> {
                        container.dayNumber.setBackgroundColor(Color.GREEN)
                    }


                    else -> {
                        container.dayNumber.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
            }
        }
    }


    private fun showNotesElements(context: Context, date: LocalDate) {
        binding.notesContainer.visibility = View.VISIBLE
        pickedDate = "${date.dayOfMonth}-${date.monthValue}-${date.year}"
        val dbNotes =
            db.collection("turnos").document(pickedDate).collection(numEmple).document("notas")
        dbNotes.collection("private").document("notaPrivada").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val privateText = document.getString("nota")
                    binding.containerShowExistPrivateNotes.visibility = View.VISIBLE
                    binding.textShowPrivate.text = privateText
                } else {
                    binding.containerShowExistPrivateNotes.visibility = View.GONE
                }
            }
        dbNotes.collection("public").document("notaPublica").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val privateText = document.getString("nota")
                    val status = document.getString("estado")
                    binding.containerShowExistPublicNotes.visibility = View.VISIBLE
                    val finalPublicText = "${privateText} (${status})"
                    binding.textShowPublic.text = finalPublicText
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
                    val idSplitDate =
                        idData.split("-")//Debemos recoger al fecha contenida en el id del documento y separarla en partes por los "-".
                    val idDay = idSplitDate[0].toInt()
                    val idMonth =
                        idSplitDate[1].toInt() //Guardamos cada parte en una variable diferente
                    val idYear = idSplitDate[2].toInt()
                    val rebuildLocalDate = LocalDate.of(
                        idYear,
                        idMonth,
                        idDay
                    ) //Volvemos a construir el LocalDate con la fecha recogida

                    //Ahora toca sacar la palabra que define el turno:

                    db.collection("turnos").document(idData).collection(numEmple).document("turno")
                        .get().addOnSuccessListener { doc ->
                            val idTurn =
                                doc.getString("turno") //Hacemos una entrada a la base de datos para recoger el String del turno y guardarlo en una variable
                            if (idTurn != null) {       //Necesitamos contener la excepción de que idTurn no sea nulo, si no no dejará construir la siguiente linea
                                employeeTurns[rebuildLocalDate] =
                                    idTurn //Asignamos el valor a la fecha en la Lista mutable (Clave->rebuildLocaldate(11-0-25) / valor-> idTurn("Mañana"))
                                binding.calendarView.notifyDateChanged(rebuildLocalDate) //Refrescamos el calendario para que se posicione sobre nuestra fecha, para posteriormente incluirla colores
                            }
                        }
                }

            }
        binding.calendarView.notifyDateChanged(LocalDate.now())
    }

    private fun loadUserData() {
        db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val document = result.documents[0]
                val numEmpleLong = document.getLong("numEmple")
                numEmple = numEmpleLong.toString()
            }
        }
    }

    override fun onClick(v: View?) {
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
                    Snackbar.make(
                        binding.root,
                        "No puedes guardar una nota vacía",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {

                    //Primero debemos comprobar qeu tanto la colección de la fecha seleccionada, como la de notas en su interior existen ya que
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
                                                Snackbar.make(
                                                    binding.root,
                                                    "Ya tienes una anotación guardada en este día",
                                                    Snackbar.LENGTH_SHORT
                                                ).show()
                                            } else { //Si no, la crea
                                                existNote.collection("private")
                                                    .document("notaPrivada")
                                                    .set(insertPrivate)
                                                    .addOnSuccessListener {
                                                        binding.containerShowExistPrivateNotes.visibility =
                                                            View.VISIBLE //Hacemos visible el container donde mostrará la nota
                                                        binding.textShowPrivate.text =
                                                            text //Ponemos el texto de la nota en el textView del Container
                                                        binding.editNotes.text.clear()  //Limpiamos el editText de caracteres
                                                        Snackbar.make(
                                                            binding.root,
                                                            "Nota privada guardada",
                                                            Snackbar.LENGTH_SHORT
                                                        ).show()
                                                    }.addOnFailureListener { e ->
                                                        Snackbar.make(
                                                            binding.root,
                                                            "Error al guardar la nota: ${e.message}",
                                                            Snackbar.LENGTH_SHORT
                                                        ).show()
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
                                                Snackbar.make(
                                                    binding.root,
                                                    "Ya tienes una nota guardada en este día",
                                                    Snackbar.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                existNote.collection("public")
                                                    .document("notaPublica")
                                                    .set(insertPublic).addOnSuccessListener {
                                                        binding.containerShowExistPublicNotes.visibility =
                                                            View.VISIBLE //Hacemos visible el contenedor para mostrar las notas
                                                        val newPublicText =
                                                            "${text} (${noteStatus})" //Juntamos la nota y su estado en una variable
                                                        binding.textShowPublic.text =
                                                            newPublicText  //Asignamos la variable anterior al textView para que se vea la nota
                                                        binding.editNotes.text.clear()  //Borramos el editText de caracteres
                                                        Snackbar.make(
                                                            binding.root,
                                                            "Nota guardada",
                                                            Snackbar.LENGTH_SHORT
                                                        ).show()
                                                    }.addOnFailureListener { e ->
                                                        Snackbar.make(
                                                            binding.root,
                                                            "Error al guardar la nota: ${e.message}",
                                                            Snackbar.LENGTH_SHORT
                                                        ).show()
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
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog, wich ->
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("private").document("notaPrivada").delete()
                            .addOnSuccessListener {
                                Snackbar.make(
                                    binding.root,
                                    "Anotación borrada con éxito",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener { e ->
                                Snackbar.make(
                                    binding.root,
                                    "Error: ${e.message}",
                                    Snackbar.LENGTH_SHORT
                                )
                                    .show()
                            }
                        binding.containerShowExistPrivateNotes.visibility = View.GONE
                    }.setNegativeButton("No") { dialog, wich ->
                        dialog.dismiss()
                    }
                    .show()
            }

            binding.deletePublic.id -> {
                builder.setMessage("¿Estas seguro de querer borrar esta nota?")
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog, wich ->
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("public").document("notaPublica").delete()
                            .addOnSuccessListener {
                                Snackbar.make(
                                    binding.root,
                                    "Anotación borrada con éxito",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener { e ->
                                Snackbar.make(
                                    binding.root,
                                    "Error: ${e.message}",
                                    Snackbar.LENGTH_SHORT
                                )
                                    .show()
                            }
                        binding.containerShowExistPublicNotes.visibility = View.GONE
                    }.setNegativeButton("No") { dialog, wich ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        selectedPosition = p2
        when (selectedPosition) {
            0 -> {
                binding.noteContainerSelected.visibility = View.INVISIBLE
            }

            1 -> {
                binding.noteContainerSelected.visibility = View.VISIBLE
                binding.typeTitleText.text = getString(R.string.privateNote)
                binding.noteInstructionsText.text = getString(R.string.instructionsPrivate)
            }

            2 -> {
                binding.noteContainerSelected.visibility = View.VISIBLE
                binding.typeTitleText.text = getString(R.string.publicNote)
                binding.noteInstructionsText.text = getString(R.string.instructionsPublic)
            }
        }
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {}
}