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
    private var selectedDate: LocalDate?=null
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var pickedDate: String
    private lateinit var name:String
    private lateinit var numEmple:String
    private lateinit var turn:String
    private var employeeTurns:MutableMap<LocalDate,String> =mutableMapOf()
    private late
    private var statusPublicEmp:MutableMap <LocalDate, String> = mutableMapOf()
    private lateinit var selectedSection:String
    private lateinit var selectedEmployee:String
    private var status:String=""
    private lateinit var builder: AlertDialog.Builder
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)

        val frameContent=findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)

        variables()
        actions()
        buildCalendar()
    }

    private fun actions() {
        binding.spinnerSections.onItemSelectedListener=this
        binding.spinnerEmployeeSelected.onItemSelectedListener=this
        binding.btnSave.setOnClickListener(this)
        binding.btnAccept.setOnClickListener(this)
        binding.btnReject.setOnClickListener(this)
        binding.spinnerTurns.onItemSelectedListener=this
    }

    private fun variables(){
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        builder = AlertDialog.Builder(this)
    }

    /**A traves de esta función generamos o cargamos el calendario completo con sus propiedades en la pantalla*/
    private fun buildCalendar() {

        val currentMonth = now()                                                // esta variable guarda el valor del mes actual
        val startMonth = currentMonth.minusMonths(100)           //Retrocede X meses para establecer el mes de inicio del calendario
        val endMonth = currentMonth.plusMonths(100)                 //Avanza X meses para establecer el mes final del calendario
        val firstDayOfWeek = firstDayOfWeekFromLocale()                         // Obtiene el primer día de la semana segun la configuracion horaria del dispositivo (Lunes)

        val titlesContainer = findViewById<ViewGroup>(R.id.titlesContainer)
        val children = titlesContainer.children.toList()

        for (i in children.indices) {
            val textView = children[i] as TextView
            val dayOfWeek = daysOfWeek()[i]
            val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            textView.text = title
            if (textView.text=="dom"){
                textView.setTextColor(Color.WHITE)
                textView.setBackgroundColor(Color.RED)
            }else{
                textView.setTextColor(Color.WHITE)
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
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view,binding.selectedDateText,binding.calendarView,selectedDate,::showScheduleNotesItems)                                   //Asigna la vista de día a un contenedor
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
                //Utilizamos la lista calve-valor que contiene la fecha/turno para ir pintando cada dia de un color cuando el método bind los construya
                val recoveredTurn = employeeTurns[data.date]
                val showPublicDate=statusPublicEmp[data.date]

                when(recoveredTurn){
                    "Mañana"->{container.dayNumber.setBackgroundColor(Color.YELLOW)}
                    "Tarde"->{container.dayNumber.setBackgroundColor(Color.BLUE)}
                    "Fiesta"->{container.dayNumber.setBackgroundColor(Color.RED)}
                    "Libre"->{container.dayNumber.setBackgroundColor(Color.GREEN)}
                    "Borrar turno"->{
                        db.collection("turnos").document(pickedDate).collection(numEmple).document("turno").delete()
                        container.dayNumber.setBackgroundColor(Color.TRANSPARENT)}

                    else ->{container.dayNumber.setBackgroundColor(Color.TRANSPARENT)}
                }
                when(showPublicDate){
                    "pendiente"->{ container.dayNumber.setTextColor(Color.MAGENTA) }
                    "aceptado" -> {
                        container.dayNumber.setTextColor(Color.GREEN)
                        binding.btnAccept.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE}
                    "rechazado" ->{
                        container.dayNumber.setTextColor(Color.RED)
                        binding.btnAccept.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE}
                }

                }
            }

        }


    /** En esta función se implementa la lógica para guardar los turnos seleccionados en la base de datos**/



    private fun showScheduleNotesItems(context: Context, date:LocalDate) {
        binding.allContainer.visibility=View.VISIBLE
        pickedDate = "${date.dayOfMonth}-${date.monthValue}-${date.year}"
        loadsectionTurns(date)
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
            }
        val sectionNumber = when (selectedSection) { //Volvemos a darle valor a sectionNumber, ya que en esta cardView no hay un Spinner que proporcione el valor necesario
                "Sala" -> "1"
                "Charcutería" -> "2"
                "Pescadería" -> "3"
                "Frutería" -> "4"
                "Carnicería" -> "5"
                "Panadería" -> "6"
                else -> "0"
            }
        var finalSheduleList=""
        val numEmpleList= mutableListOf<String>()
        db.collection("secciones").document(sectionNumber).collection("Empleados").get().addOnSuccessListener { find->
            for(document in find){
                val numberDoc=document.id
                numEmpleList.add(numberDoc)
            }
        }
        db.collection("secciones").document(sectionNumber).collection("Jefe de seccion").get().addOnSuccessListener { find ->
            for (document in find) {
                val numberDoc = document.id
                numEmpleList.add(numberDoc)
            }
        }
        for(numEmpleVal in numEmpleList) {
            db.collection("users").document(numEmpleVal).get()
                .addOnSuccessListener { documentUser ->
                    val name = documentUser.getString("nombre")
                    val surname = documentUser.getString("apellidos")
                    val section = documentUser.getString("seccion")

                    db.collection("turnos").document(pickedDate).collection(numEmpleVal)
                        .document("turno").get().addOnSuccessListener { documentSchedule ->
                            val scheduleDoc = documentSchedule.getString("turno") ?: "Sin turno"
                            if (selectedSection == section) {
                                finalSheduleList += "\n ${name} ${surname} - TURNO: ${scheduleDoc}"
                                binding.showOtherSchedule.text=finalSheduleList
                            }


                        }
                }
        }
    }

    private fun loadsectionTurns(date: LocalDate){
        pickedDate = "${date.dayOfMonth}-${date.monthValue}-${date.year}"
        var finalSheduleList=""
        val numEmpleList= mutableListOf<String>()
        db.collection("secciones").document(selectedSection).collection("Empleados").get().addOnSuccessListener { find->
            for(document in find){
                val numberDoc=document.id
                numEmpleList.add(numberDoc)
            }
        }
        db.collection("secciones").document(selectedSection).collection("Jefe de seccion").get().addOnSuccessListener { find ->
                for (document in find) {
                    val numberDoc = document.id
                    numEmpleList.add(numberDoc)
                }
            }
        for(numEmpleVal in numEmpleList) {
            db.collection("users").document(numEmpleVal).get()
                .addOnSuccessListener { documentUser ->
                    val name = documentUser.getString("nombre")
                    val surname = documentUser.getString("apellidos")
                    val section = documentUser.getString("seccion")

                    db.collection("turnos").document(pickedDate).collection(numEmpleVal)
                        .document("turno").get().addOnSuccessListener { documentSchedule ->
                        val scheduleDoc = documentSchedule.getString("turno") ?: "Sin turno"
                        if (selectedSection == section) {
                            finalSheduleList += "\n ${name} ${surname} - TURNO: ${scheduleDoc}"
                            binding.showOtherSchedule.text=finalSheduleList
                        }


                    }
                }
        }




}
    private fun loadEmployeeTurns(numEmple:String){
        employeeTurns.clear()       //Primero limpiamos la lista por si contiene datos anteriores
        db.collection("turnos").get().addOnSuccessListener { document -> //Iniciamos la recogida de datos de los turnos
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
                //Aqui cargamos las sugerencias del día
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
                }

        }
        binding.calendarView.notifyDateChanged(LocalDate.now())
    }
    private fun loadSpinnerEmployee(section:String){
        db=FirebaseFirestore.getInstance()
        db.collection("users").whereEqualTo("seccion",section).get().addOnSuccessListener { document->
            val employees=mutableListOf<String>()
            for(doc in document){
                name= doc.getString("nombre").toString()
                var numEmpleLong=doc.getLong("numEmple")
                var rol=doc.getString("rol")
                numEmple=numEmpleLong.toString()
                if(rol=="Jefe de sección"){
                    employees.add(" $numEmple: $name (Jefe)" )
                }else{
                    employees.add("$numEmple: $name")
                }

            }

            val adapter=ArrayAdapter(this,R.layout.spinner_edited_item,employees)
            adapter.setDropDownViewResource(R.layout.spinner_edited_item)
            binding.spinnerEmployeeSelected.adapter=adapter
        }.addOnFailureListener{
            Snackbar.make(binding.root,"Error al cargar empleados",Snackbar.LENGTH_SHORT).show()
        }
    }
    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
    when(p0?.id) {
        binding.spinnerSections.id -> {
            selectedSection = p0.getItemAtPosition(p2).toString()
            positionSection=p2
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
            loadEmployeeTurns(numEmple)
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
        binding.spinnerTurns.id->{
                turn=p0.getItemAtPosition(p2).toString()
            when(p2){
                0->binding.btnSave.isEnabled=false
                else->binding.btnSave.isEnabled=true
            }
        }
    }
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {
    }


    override fun onClick(v: View?) {
        val dateParts= pickedDate.split("-")
        val dateForThisFunction=LocalDate.of(dateParts[2].toInt(),dateParts[1].toInt(),dateParts[0].toInt())

        when(v?.id) {
            binding.btnSave.id -> {
                val createDoc = hashMapOf("created" to true)
                db.collection("turnos").document(pickedDate).set(createDoc)
                val schedule = hashMapOf("turno" to turn)
                db.collection("turnos").document(pickedDate).collection(numEmple).document("turno")
                    .set(schedule)

                employeeTurns[dateForThisFunction] = turn
                binding.calendarView.notifyDateChanged(dateForThisFunction)
            }

            binding.btnAccept.id -> {
                builder.setMessage("Esta acción no será reversible a menos que el empleado borre la sugerencia.¿Deseas ACEPTAR la sugeréncia?")
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog, wich ->
                        db.collection("turnos").document(pickedDate).collection(numEmple)
                            .document("notas")
                            .collection("public").document("notaPublica")
                            .update("estado", "aceptado",).addOnSuccessListener {
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
            binding.btnReject.id->{
                builder.setMessage("Esta acción no será reversible a menos que el empleado borre la sugerencia.¿Deseas RECHAZAR la sugeréncia?")
                    .setTitle("¡Atención!").setPositiveButton("Si") { dialog, wich ->
                db.collection("turnos").document(pickedDate).collection(numEmple).document("notas")
                    .collection("public").document("notaPublica").update("estado","aceptado",).addOnSuccessListener {
                        snackBar(binding.root,"Sugerencia rechazada.")
                        statusPublicEmp[dateForThisFunction]="rechazado"
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



private fun snackBar(view:View, message:String) {
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
}

//Modificar los cardView de Notas: El empleado creara una anotacion en la que automáticamente se guardará en la base de datos
//con una señal de "pendiente" y un boolean de aprovado "true o false". En el perfil de Jefe, podrá listar las anotaciones de cada empleado
//pudiendo aceptar o rechazar las anotaciones de cada uno, cambiando la señal de "pendiente" a "visto" y aceptando o rechazando su notificación.