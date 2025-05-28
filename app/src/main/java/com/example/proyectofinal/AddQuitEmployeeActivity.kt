package com.example.proyectofinal


/**
 * No completada
 *
 * Se ha descubierto un fallo importante: al crear un nuevo empleado, al autenticarlo en el codigo, si nos ibamos al Perfil
 * en vez de aparecer el perfil del Superusuario (el que debería estar) aparecía el del perfil nuevo creado y se apoderaba de la sesion
 * de superusuario teniendo acceso a toda la app hasta que no se cerrase sesion. Se ha arreglado forzando al superusuario que
 * ingrese su password para completar el proceso de añadir usuario y asi poder recuperar su contraseña para volverle a iniciar sesion
 * despues de dar de alta al usuario en la base de datos. OK 3H
 *
 * Se ha descubierto un fallo importante: cuando se creaba un usuario nuevo, se comprobaba el último Nº de empleado en la coleccion users.
 * Esto ocasionaba que si se borraban usuarios, se volverían a reutilizar Nº de empleados de usuarios ya inexistentes. Se ha solucionado
 * haciendo dicha consulta a la colección "usuarios registrados" cuya finalidad es mantener el registro de todos los numeros de empleado
 * sigan existiendo o no. 2H
 *
 * Falta introducir un alertDialog para el borrado de usuario por precaución. OK
 * Falta dar estilo a los radioButton OK
 * Falta controlar un Snackbar por si la contraseña del superjefe se mete mal.
 */
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.example.proyectofinal.databinding.ActivityAddQuitEmployeeBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AddQuitEmployeeActivity : BaseActivity(), RadioGroup.OnCheckedChangeListener,
    View.OnClickListener, AdapterView.OnItemSelectedListener {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var positionSection: Int =
        0    //variable para guardar la posicion del Spiner de secciones.
    private var positionJob: Int =
        0        //variable para guardar la posicion del spinner de puesto.
    private var job: String = ""            //variable para guargar el puesto.
    private var section: String = ""           //variable para guardar la sección.
    private lateinit var binding: ActivityAddQuitEmployeeBinding
    private lateinit var builder: AlertDialog.Builder
    private var firstLoadSpinnerSections: Boolean = false
    private var firstLoadSpinnerJob: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddQuitEmployeeBinding.inflate(layoutInflater)

        val navDrawer = findViewById<FrameLayout>(R.id.content_frame)
        navDrawer.addView(binding.root)


        variables()
        actions()


    }

    private fun actions() {
        binding.radioGroup.setOnCheckedChangeListener(this)
        binding.includeAddEmployee.btnAdd.setOnClickListener(this)
        binding.includeDeleteEmployee.btnFind.setOnClickListener(this)
        binding.includeDeleteEmployee.btnDelete.setOnClickListener(this)
        binding.includeAddEmployee.spinnerSections.onItemSelectedListener = this
        binding.includeAddEmployee.spinnerJob.onItemSelectedListener = this
        binding.includeDeleteEmployee.btnDelete.visibility = View.GONE
    }

    private fun variables() {
        builder = AlertDialog.Builder(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    /**
     * OnCheckedChange de los radioButtons para mostrar una vista u otra
     *
     * Sobreescribimos el método onCheckedChange para mostrar las Cardview "addCardView" o "deleteCardView" segun la seleccion del
     * RadioGroup mediante los métodos view.VISIBLE y view.GONE
     */
    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            /**
             * Si se pulsa Añadir empleado, se oculta Borrar empleado
             */
            binding.AddEmployee.id -> {
                binding.addCardView.visibility = View.VISIBLE
                binding.deleteCardView.visibility = View.GONE
            }
            /**
             * Lo mismo pero al revés
             */
            binding.deleteEmployee.id -> {
                binding.addCardView.visibility = View.GONE
                binding.deleteCardView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Botones de AñadirEmpleado, BorrarEmpleado y Buscar
     */
    override fun onClick(v: View?) {
        val findNum = binding.includeDeleteEmployee.editNumber.text.toString()
        when (v?.id) {

            /**
             * Si pulsamos este botón, llama a la función validateData() para guardar un empleado
             */
            binding.includeAddEmployee.btnAdd.id -> {
                Log.d("DEBUG", "Botón Añadir pulsado")
                validateData()
            }

            /**
             * Si pulsamos este botón, llama a la función findUser() para buscar un empleado
             */
            binding.includeDeleteEmployee.btnFind.id -> {
                findUser(findNum)
            }

            /**
             * Si pulsamos este botón, llama a la función deleteUser() para borrar un empleado
             */
            binding.includeDeleteEmployee.btnDelete.id -> {
                deleteUser(findNum) //Función para borrar usuario
            }
        }
    }

    /**
     * Función para añadir un empleado
     *
     * Esta función recoge el nombre, apellidos y email a partir de los editText, así como la sección a la que
     * pertenece y el rol de empleado de los spinners correspondientes para pasarselos como parámetro a la función
     * saveData(). Tambén recoge los posibles errores de dejarse los campos vacíos, o de que los spinner deben tener
     * alguna opcion seleccionada.
     */
    private fun validateData() {
        val name = binding.includeAddEmployee.editName.text.toString() //Recogemos el nombre
        val surname =
            binding.includeAddEmployee.editSurname.text.toString() // Recogemos el apellido
        val email = binding.includeAddEmployee.editEmail.text.toString() //recogemos el email
        val superPassword =
            binding.includeAddEmployee.editSuperPass.text.toString() // Recogemos la contraseña
        if (name.isEmpty()) { // Si el campo del nombre esta vacío mostramos error
            snackBar(binding.root, "Debe ingresar un nombre")
        } else if (surname.isEmpty()) { // Si el campo apellido esta vacío mostramos error
            snackBar(binding.root, "Debe ingresar apellidos")
        } else if (email.isEmpty()) { // Si el campo email está vacío mostramos error
            snackBar(binding.root, "Debe ingresar un email")
        } else if (positionSection == 0) { // Si no se ha seleccionado un elemento de los spinner mostramos error
            snackBar(binding.root, "Debe seleccionar una sección")
        } else if (positionJob == 0) {
            snackBar(binding.root, "Debe seleccionar un puesto")
        } else if (superPassword.isEmpty()) { // Si no se ha ingresado la contraseña mostramos error
            snackBar(binding.root, "Debes introducir tu contraseña para continuar")
        } else {
            val superEmail =
                auth.currentUser?.email!! // Recogemos el mail actual de authenticator del usuario Jefe
            saveData(
                name,
                surname,
                email,
                section,
                job,
                superEmail,
                superPassword
            ) //Llamamos al método para guardar los datos en la base de datos
        }
    }

    /**
     * Lógica de los Spinner de Secciones y de Rol
     */
    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p0?.id) {

            /**
             * Spinner de secciones
             *
             * Este spinner recoge la sección y su posición para tratarlas mas adelante. Si no, salta un mensaje
             * de error.
             */
            binding.includeAddEmployee.spinnerSections.id -> {
                section = p0.getItemAtPosition(p2).toString()
                positionSection = p2
                if (firstLoadSpinnerSections) {
                    if (positionSection == 0) {
                        snackBar(binding.root, "Debes seleccionar una sección")
                    } else {
                        snackBar(binding.root, "$section selecccionado")
                    }
                } else {
                    firstLoadSpinnerSections = true
                }
            }

            /**
             * Spinner de Rol
             *
             * Hace lo mismo que el anterior pero con los roles.
             */
            binding.includeAddEmployee.spinnerJob.id -> {
                job = p0.getItemAtPosition(p2).toString()
                positionJob = p2
                if (firstLoadSpinnerJob) {
                    if (positionJob == 0) {
                        snackBar(binding.root, "Debes seleccionar una ocupación")

                    } else {
                        snackBar(binding.root, "$job selecccionado")
                    }
                } else {
                    firstLoadSpinnerJob = true
                }
            }
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    /**
     * Función para guardar un empleado.
     *
     * Esta función es la encargada de realizar la conexión a la base de datos y de guardar sus datos en las colecciones correspondientes
     * o en los documentos correspondientes. Además, se genera de forma automática un número de empleado de tipo Int y una contraseña, la cual
     * será la misma que el número de empleado. Para el número de empleado, se hace una consulta a la coleccion "users" mediante un ORDER BY
     * descendente para averiguar el último numero de empleado que existe. Una vez averiguado, se crea uno nuevo sumándole +32 al último
     * numero de empleado existente. (Cuando el usuario creado haga su primer login, se le recomendará cambiarla).
     */
    private fun saveData(
        name: String,
        surname: String,
        email: String,
        section: String,
        job: String,
        superEmail: String,
        superPassword: String
    ) {
        db.collection("usuarios registrados").orderBy("numEmple", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { documents ->// Se hace una consulta a la coleeción usuarios registrados para averiguar cual es el ultimo Nº de empleado asignado.
                var newEmpNum = 1  //Inicializamos la variable
                if (!documents.isEmpty) { // Se genera el numero de empleado y se guarda en una variable.
                    val lastEmpNum = documents.documents[0].getLong("numEmple") ?: 0
                    newEmpNum = (lastEmpNum + 32).toInt()
                }
                val pass =
                    newEmpNum.toString() //Se genera la password igualandola al numero de empleado y se castea a String.

                /**
                 * Esta consulta comprueba que el correo que se esta introduciendo no exista ya, si existe, dará error y cortará la creación
                 */
                db.collection("users").whereEqualTo("email", email).get()
                    .addOnSuccessListener { query ->
                        if (!query.isEmpty) {
                            snackBar(
                                binding.root,
                                "Este correo ya existe, por favor introduce otro"
                            )
// Si el correo no existe, dará comienzo la inserción del usuario dependiendo de su rol
                        } else {
                            if (job == "Jefe de sección") { // Si es Jefe de seccion llamamos al método correspondiente para guardarlo
                                insertSectionBoss(
                                    email,
                                    pass,
                                    name,
                                    surname,
                                    newEmpNum,
                                    job,
                                    section,
                                    superEmail,
                                    superPassword
                                )
                            } else { // Si no, lo guardamos como empleado con su función correspondiente
                                insertEmployee(
                                    email,
                                    pass,
                                    name,
                                    surname,
                                    newEmpNum,
                                    job,
                                    section,
                                    superEmail,
                                    superPassword
                                )
                            }

                        }
                    }.addOnFailureListener {
                    snackBar(binding.root, "error al verificar el correo")
                }
            }
    }

    private fun insertSectionBoss(
        email: String,
        pass: String,
        name: String,
        surname: String,
        newEmpNum: Int,
        job: String,
        section: String,
        superEmail: String,
        superPassword: String
    ) {
        val user = auth.currentUser
        val token = EmailAuthProvider.getCredential(superEmail, superPassword)
        if (user != null) {
            user.reauthenticate(token).addOnSuccessListener {
                db.collection("secciones").document(positionSection.toString())
                    .collection("Jefe de sección").get()
                    .addOnSuccessListener { bossQuery -> //Consultamos en la colección secciones si ya existe un jefe de sección para la sección seleccionada
                        if (!bossQuery.isEmpty) { //Si existe...mostramos mensaje informativo
                            snackBar(
                                binding.root,
                                "Ya existe un jefe de sección en esta sección"
                            )
                        } else { // Si no lo creamos
                            auth.createUserWithEmailAndPassword(email, pass)
                                .addOnCompleteListener { task ->//Aqui metemos el user en authentication
                                    if (task.isSuccessful) { // Y lo creamos con sus campos correspondientes en las colecciones pertinentes
                                        val uid =
                                            auth.currentUser!!.uid //Introducimos su uid en el documento
                                        val logged =
                                            false // False para que salte la advertencia de cambiar la contraseña en el primer login
                                        val employee =
                                            hashMapOf( //Contruimos el hashMap para guardar los datos despues
                                                "uid" to uid,
                                                "nombre" to name,
                                                "apellidos" to surname,
                                                "numEmple" to newEmpNum,
                                                "email" to email,
                                                "rol" to job,
                                                "seccion" to section,
                                                "login" to logged
                                            )
                                        val employeeNumber =
                                            hashMapOf("numEmple" to newEmpNum) //Hashmap para guardar el número de empleado en el documento "empleados_sala."
                                        val sectionData = hashMapOf(
                                            "numEmple" to newEmpNum,
                                            "nombre" to name,
                                            "apellidos" to surname
                                        ) //hashMap para guardar el empleado en la sección

                                        val state =
                                            "activo" //Esta variable contempla que el usuario está dado de alta o de baja en la empresa. Cuando se da de baja, desaparece de la BBDD pero se conserva su numEmple.
                                        val registeredUser =
                                            hashMapOf(
                                                "estado" to state,
                                                "numEmple" to newEmpNum
                                            )
                                        //Estas variables guardan las inserciones de datos en los documentos o colecciones correspondientes utilizando los Hashmaps anteriores.
                                        val insertUser =
                                            db.collection("users")
                                                .document(newEmpNum.toString())
                                                .set(employee) //Insertamos el hasMap con los datos del usuario
                                        val insertRegistered =
                                            db.collection("usuarios registrados")
                                                .document(newEmpNum.toString())
                                                .set(registeredUser) // Insertamos el hashMap del estado.set(registeredUser)

                                        //si no hay jefe de sección lo inserta
                                        val insertBoss = db.collection("secciones")
                                            .document(positionSection.toString())
                                            .collection("Jefe de sección")
                                            .document(newEmpNum.toString())
                                            .set(sectionData)

                                        /**
                                         * Task.whenAllComplete
                                         * Utilizamos el método "Tasks.whenAllComplete" para indicar que cuando se terminen de realizar las tres acciones
                                         * de inserciones en la BBDD anteriores (las pasamos mediante parametro a traves de las variables) se pasará a la
                                         * pantalla "SuccessActivity donde se mostrarán los datos nuevos del usuario creado. Si no se realizan alguna de
                                         * las tres acciones de inserciones se mostrará un error mediante el Listener addOnFailureListener
                                         */

                                        Tasks.whenAllComplete(
                                            insertUser,
                                            insertBoss,
                                            insertRegistered
                                        )
                                            .addOnSuccessListener { //Si se completan todas las tareas pasamos a la pantalla de Verificación.
                                                val intent =
                                                    Intent(
                                                        this,
                                                        SuccessActivity::class.java
                                                    )
                                                val bundle = Bundle()
                                                //Mirar si se puede pasar con una lista o algo mas cómodo
                                                bundle.putString("name", name)
                                                bundle.putString("surname", surname)
                                                bundle.putString(
                                                    "numEmp",
                                                    newEmpNum.toString()
                                                )
                                                bundle.putString("password", pass)
                                                intent.putExtra("data", bundle)
                                                builder.setMessage("Se va a crear el usuario $name $surname . ¿Quieres continuar?")
                                                    .setTitle("¿Quieres continuar?")
                                                    .setPositiveButton("Si") { dialog, wich -> //Se genera un diálogo de advertencia
                                                        startActivity(intent)
                                                        /**
                                                         * Aquí es necesario volver a iniciarle sesión al ususario actual ya que, de no hacerlo, la sesión pasaría a ser
                                                         * del usuario creado recientemente. Descubierto a traves de fallos y pruebas. Si creabas un nuevo empleado e
                                                         * ibas a la pagina de mi perfil, aparecían los datos del usuario creado, no del Jefe...
                                                         */
                                                        auth.signInWithEmailAndPassword(
                                                            superEmail,
                                                            superPassword
                                                        )
                                                            .addOnFailureListener { e -> //Si la operación falla, mostramos error
                                                                snackBar(
                                                                    binding.root,
                                                                    "Error: ${e.message}"
                                                                )
                                                            }
                                                    }
                                                    .setNegativeButton("No") { dialog, _ -> // Si pulsamos No en el diálogo, la operación se abortará
                                                        dialog.dismiss()
                                                    }.show()

                                            }.addOnFailureListener { e ->
                                                snackBar(
                                                    binding.root,
                                                    "Error al guardar el jefe: ${e.message}"
                                                )

                                            }
                                    } else { // Si existe el documento en la base de datos de JEfe de Seccion...saltara un mensaje informativo y no dejará crear otro.
                                        snackBar(
                                            binding.root,
                                            "Ya existe un jefe de sección para esta sección"
                                        )
                                    }

                                }
                        }
                    }.addOnFailureListener {
                        snackBar(
                            binding.root,
                            "Contraseña incorrecta. No se ha creado ningún usuario"
                        )
                    }
            }
        } else {
            snackBar(
                binding.root,
                "Error: No se pudo verificar al superjefe. Intenta reiniciar sesión."
            )
        }
    }


    private fun insertEmployee(
        email: String,
        pass: String,
        name: String,
        surname: String,
        newEmpNum: Int,
        job: String,
        section: String,
        superEmail: String,
        superPassword: String
    ) {
        val user = auth.currentUser
        val token = EmailAuthProvider.getCredential(superEmail, superPassword)
        if (user != null) {
            user?.reauthenticate(token)
                ?.addOnCompleteListener { task ->//Aqui metemos el user en authentication
                    if (task.isSuccessful) {
                        val uid =
                            auth.currentUser!!.uid //Introducimos su uid en el documento
                        val logged =
                            false // False para que salte la advertencia de cambiar la contraseña en el primer login
                        val employee =
                            hashMapOf( //Contruimos el hashMap para guardar los datos despues
                                "uid" to uid,
                                "nombre" to name,
                                "apellidos" to surname,
                                "numEmple" to newEmpNum,
                                "email" to email,
                                "rol" to job,
                                "seccion" to section,
                                "login" to logged
                            )
                        val employeeNumber =
                            hashMapOf("numEmple" to newEmpNum) //Hashmap para guardar el número de empleado en el documento "empleados_sala."
                        val sectionData = hashMapOf(
                            "numEmple" to newEmpNum,
                            "nombre" to name,
                            "apellidos" to surname
                        ) //hashMap para guardar el empleado en la sección
                        val state =
                            "activo" //Esta variable contempla que el usuario está dado de alta o de baja en la empresa. Cuando se da de baja, desaparece de la BBDD pero se conserva su numEmple.
                        val registeredUser =
                            hashMapOf("estado" to state, "numEmple" to newEmpNum)
                        //Estas variables guardan las inserciones de datos en los documentos o colecciones correspondientes utilizando los Hashmaps anteriores.

                        val insertUser =
                            db.collection("users").document(newEmpNum.toString())
                                .set(employee) //Insertamos el hasMap con los dtos del usuario
                        val insertRegistered = db.collection("usuarios registrados")
                            .document(newEmpNum.toString()) // Insertamos el hashMap del estado
                            .set(registeredUser)

                        //si no hay jefe de sección lo inserta
                        val insertEmployee =
                            db.collection("secciones")
                                .document(positionSection.toString())
                                .collection("Empleados").document(newEmpNum.toString())
                                .set(sectionData)

                        /**
                         * Task.whenAllComplete
                         * Utilizamos el método "Tasks.whenAllComplete" para indicar que cuando se terminen de realizar las tres acciones
                         * de inserciones en la BBDD anteriores (las pasamos mediante parametro a traves de las variables) se pasará a la
                         * pantalla "SuccessActivity donde se mostrarán los datos nuevos del usuario creado. Si no se realizan alguna de
                         * las tres acciones de inserciones se mostrará un error mediante el Listener addOnFailureListener
                         */
                        Tasks.whenAllComplete(
                            insertUser,
                            insertEmployee,
                            insertRegistered
                        )
                            .addOnSuccessListener { //Si se completan todas las tareas pasamos a la pantalla de Verificación.
                                val intent = Intent(this, SuccessActivity::class.java)
                                val bundle = Bundle()
                                //Mirar si se puede pasar con una lista o algo mas cómodo
                                bundle.putString("name", name)
                                bundle.putString("surname", surname)
                                bundle.putString("numEmp", newEmpNum.toString())
                                bundle.putString("password", pass)
                                intent.putExtra("data", bundle)
                                builder.setMessage("Se va a crear el usuario $name $surname . ¿Quieres continuar?")
                                    .setTitle("¿Quieres continuar?")
                                    .setPositiveButton("Si") { dialog, wich ->// Mismo dialogo para frenar al usuario antes de insertar
                                        startActivity(intent)
                                        clearData() // función para limpiar los campso de escritura
                                        auth.signInWithEmailAndPassword(
                                            superEmail,
                                            superPassword
                                        )
                                            .addOnFailureListener { e ->
                                                snackBar(
                                                    binding.root,
                                                    "Error: Error al iniciar sesión de Superjefe"
                                                )
                                            }
                                    }.setNegativeButton("No") { dialog, _ ->
                                        dialog.dismiss()
                                    }.show()

                            }.addOnFailureListener { e ->
                                snackBar(
                                    binding.root,
                                    "Error al guardar el jefe: ${e.message}"
                                )

                            }
                    }


                }?.addOnFailureListener {
                    snackBar(
                        binding.root,
                        "Contraseña incorrecta. No se ha creado ningún usuario"
                    )
                }
        } else {
            snackBar(
                binding.root,
                "Error: No se pudo verificar al superjefe. Intenta reiniciar sesión."
            )
        }
    }


    /**
     * Función para buscár un empleado
     *
     * Esta función permite la búsqueda de un empleado en la base de datos a traves de su número de empleado.
     */
    private fun findUser(numEmple: String) {
        db.collection("users").document(numEmple).get()
            .addOnSuccessListener { document ->
                if (document.exists()) { //Si existe un documento con ese número, se cogen sus datos.
                    val name = document.getString("nombre")
                    val surname = document.getString("apellidos")
                    val rol = document.getString("rol")
                    val section = document.getString("seccion")

                    val resultText =
                        "Nombre completo: ${name} ${surname}\nSección: ${rol} en ${section}." //Y se muestra el resultado
                    binding.includeDeleteEmployee.textResult.text = resultText
                    binding.includeDeleteEmployee.btnDelete.visibility = View.VISIBLE
                } else {
                    snackBar(
                        binding.root,
                        "No se ha encontrado ningún empleado con ese número."
                    )

                    binding.includeDeleteEmployee.textResult.setText("Sin resultados")
                    binding.includeDeleteEmployee.btnDelete.visibility = View.GONE
                }
            }
    }

    /**
     * Función para borrar un empleado
     *
     * Esta función permite borrar un empleado a través de su número de empleado
     */
    private fun deleteUser(numEmple: String) {
        db.collection("users").document(numEmple).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val findSection =
                        document.getString("seccion") //Buscamos la sección a la que pertenece
                    val rol = document.getString("rol").toString() //También su rol
                    val sectionNumber =
                        when (findSection) { //Volvemos a darle valor a sectionNumber, ya que en esta cardView no hay un Spinner que proporcione el valor necesario
                            "Sala" -> "1"
                            "Charcutería" -> "2"
                            "Pescadería" -> "3"
                            "Frutería" -> "4"
                            "Carnicería" -> "5"
                            "Panadería" -> "6"
                            else -> "0"
                        }
                    if (rol == "Jefe de tienda") {
                        snackBar(
                            binding.root,
                            "No puedes borrar el jefe de tienda. Para borrarlo contacta con Central."
                        )
                    }else{
                        builder.setTitle("!Atención¡")
                            .setMessage("Estás seguro de querer dar de baja al empleado?")
                            .setPositiveButton("SI") { dialog, _ ->

                                val deleteUser = db.collection("users").document(numEmple)
                                    .delete() //Borramos al usuario de la colección usuarios.
                                val setState =
                                    db.collection("usuarios registrados").document(numEmple)
                                        .update(
                                            "estado",
                                            "inactivo"
                                        ) //Actualizamos su estado a "inactivo" para que, cuando generemos otro nuevo empleado, el sistema no reutilice números de empleado ya dados de baja.

                                val locateSection =
                                    db.collection("secciones").document(sectionNumber)
                                when (rol) {
                                    "Jefe de sección" -> { //Si es jefe de sección lo borramos de su colección.
                                        val deleteBoss =
                                            locateSection.collection("Jefe de sección")
                                                .document(numEmple)
                                                .delete()

                                        /**
                                         * task.whenAllComplete
                                         *
                                         * Cuando se completen las tareas de borrar usuario, actualizar el estado, borrar su rol y borrar el jefe
                                         * Se muestra el mensaje de exito o de error dependiendo del caso.
                                         */
                                        Tasks.whenAllComplete(
                                            deleteUser,
                                            setState,
                                            deleteBoss
                                        )
                                            .addOnSuccessListener {
                                                snackBar(
                                                    binding.root,
                                                    "Se ha borrado el jefe de sección correctamente"
                                                )
                                                clearData() // Función pàra borrar los datos en los campso de escritura
                                                binding.includeDeleteEmployee.btnDelete.visibility =
                                                    View.GONE
                                            }.addOnFailureListener { e ->
                                                snackBar(
                                                    binding.root,
                                                    "Error al borrar el jefe de sección ${e.message}"
                                                )

                                            }
                                    }
                                    /**
                                     * Si es empleado, se sigue el mismo procedimiento de antes pero sin pasar por la condicional de los jefes
                                     */
                                    else -> { //Si es empleado, se sigue el mismo procedimiento
                                        val deleteEmployee =
                                            locateSection.collection("Empleados")
                                                .document(numEmple)
                                                .delete()
                                        Tasks.whenAllComplete(
                                            deleteUser,
                                            setState,
                                            deleteEmployee
                                        )
                                            .addOnSuccessListener {
                                                snackBar(
                                                    binding.root,
                                                    "Se ha borrado el empleado correctamente"
                                                )
                                                clearData()
                                                binding.includeDeleteEmployee.btnDelete.visibility =
                                                    View.GONE
                                            }.addOnFailureListener { e ->
                                                snackBar(
                                                    binding.root,
                                                    "Error al borrar el empleado ${e.message}"
                                                )
                                            }
                                    }
                                }
                            }.setNegativeButton("NO") { dialog, _ ->
                                dialog.dismiss()
                            }
                        builder.show()
                    }
                }
            }
    }

    /**
     * Función para borrar los campos donde se escriben o se muestran datos.
     */
    private fun clearData() {
        binding.includeDeleteEmployee.editNumber.text.clear()
        binding.includeDeleteEmployee.textResult.text = ""
        binding.includeAddEmployee.editName.text.clear()
        binding.includeAddEmployee.editSurname.text.clear()
        binding.includeAddEmployee.editEmail.text.clear()
        binding.includeAddEmployee.editSuperPass.text.clear()
    }


}

private fun snackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()

}


