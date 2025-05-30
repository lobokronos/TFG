package com.example.proyectofinal


/**
 *Completada
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AddQuitEmployeeActivity : BaseActivity(), RadioGroup.OnCheckedChangeListener,
    View.OnClickListener, AdapterView.OnItemSelectedListener {
    private lateinit var db: FirebaseFirestore //Variable para iniciar la conexion con Firebase posteriormente
    private lateinit var auth: FirebaseAuth //Variable para inicializar la instancia de Authentication
    private var positionSection: Int = 0    //variable para guardar la posicion del Spiner de secciones. Empieza en la psoicion 0
    private var positionJob: Int = 0        //variable para guardar la posicion del spinner de puesto. Empieza en 0
    private var job: String = ""            //variable para guargar el puesto. Empieza con valor vacío
    private var section: String = ""           //variable para guardar la sección. Empieza con valor vacío
    private lateinit var binding: ActivityAddQuitEmployeeBinding // variable para inicializar binding despues
    private lateinit var builder: AlertDialog.Builder // variable para inicializar los alertDialog despues
    private var firstLoadSpinnerSections: Boolean = false // Sirve para saber si el usuario ha interactuado con el spinner de secciones. Estando en false evita que salte el snackbar de seleccionar seccion nada mas abrir la pantalla
    private var firstLoadSpinnerJob: Boolean = false // Sirve para saber si el usuario ha interactuado con el spinner de puesto. Estando en false evita que salte el snackbar de seleccionar puesto nada mas abrir la pantalla
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
        builder = AlertDialog.Builder(this) // Variable para inicializar el AlertDialog
        auth =
            FirebaseAuth.getInstance() // Variable para inicializar la instancia de Authentication
        db =
            FirebaseFirestore.getInstance() // Variable inicializar la instancia de  Firestore y simplificar las futuras consultas a este
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
                if (firstLoadSpinnerSections) { // Solo se entra en este bucle si en valor de esta variable es true, es decir, hasta que el usuario no interactue con el spinner.Esto se usa para evitar que lance avisos nada mas iniciar la activity.
                    if (positionSection == 0) { // Si ya ha cargado antes, es decir, si el usuario ha interactuado con el antes, mostrará el mensaje
                        snackBar(binding.root, "Debes seleccionar una sección")
                    } else {
                        snackBar(
                            binding.root,
                            "$section selecccionado"
                        ) // Si ha elegido otra sección, mostrará el mensaje
                    }
                } else {
                    firstLoadSpinnerSections =
                        true // Si el usuario interactua con el spinner, cambia el valor a true para indicar que ya se puede mostrar el snackbar en caso de que no se elija una sección a drede
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
     * Función para guardar un usuario.
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
                db.collection("usuarios registrados").whereEqualTo("email", email).get()
                    .addOnSuccessListener { query ->
                        if (!query.isEmpty) {
                            snackBar(
                                binding.root,
                                "Este correo ya existe, por favor introduce otro"
                            )
// Si el correo no existe, dará comienzo la inserción del usuario dependiendo de su rol
                        } else {
                            if (job == "Jefe de sección") { // Si es Jefe de seccion llamamos al método correspondiente para guardarlo
                                db.collection("secciones").document(positionSection.toString())
                                    .collection("Jefe de sección").get()
                                    .addOnSuccessListener { bossQuery ->
                                        if (!bossQuery.isEmpty) {
                                            snackBar(
                                                binding.root,
                                                "Ya existe un jefe de sección en esta sección"
                                            )
                                        } else {
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
                                        }
                                    }.addOnFailureListener {
                                        snackBar(
                                            binding.root,
                                            "Error al comprobar si hay jefe de sección"
                                        )
                                    }

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
                        snackBar(binding.root, "Error al verificar el correo")
                    }
            }
    }

    /**
     * Función anidada para crear un jefe de sección en caso de que el usuario a crear tenga ese rol
     */
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
        val user =
            auth.currentUser //Aquí recogemos el usuario autenticado actualmente (El jefe de tienda)
        val token = EmailAuthProvider.getCredential(
            superEmail,
            superPassword
        ) //Construye las credenciales de este usuario para verificar su identidad
        if (user != null) { //Si el usuario no es nulo (existe o se encuentra)
            user.reauthenticate(token)
                .addOnSuccessListener { // Lo reautenticamos para verificar su identidad antes de la creación
                    // Si no lo creamos
                    builder.setMessage("Se va a crear el usuario $name $surname . ¿Quieres continuar?")
                        .setTitle("¿Quieres continuar?")
                        .setPositiveButton("Si") { dialog, wich -> //Se genera un diálogo de advertencia
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
                                                "numEmple" to newEmpNum,
                                                "email" to email
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
                                                val intent =
                                                    Intent(
                                                        this,
                                                        SuccessActivity::class.java
                                                    )
                                                val bundle =
                                                    Bundle() //Creamos un bundle para pasar los datos del usuario creado a la pantalla SuccessActivity.kt
                                                bundle.putString("name", name)
                                                bundle.putString("surname", surname)
                                                bundle.putString(
                                                    "numEmp",
                                                    newEmpNum.toString()
                                                )
                                                bundle.putString("password", pass)
                                                intent.putExtra("data", bundle)
                                                startActivity(intent) //Activamos el cambio de pantalla
                                                /**
                                                 * Aquí es necesario volver a iniciarle sesión al ususario actual ya que, de no hacerlo, la sesión pasaría a ser
                                                 * del usuario creado recientemente. Descubierto a traves de fallos y pruebas. Si creabas un nuevo empleado e
                                                 * ibas a la pagina de mi perfil, aparecían los datos del usuario creado, no del Jefe...
                                                 */


                                            }.addOnFailureListener { e ->
                                                snackBar(
                                                    binding.root,
                                                    "Error al guardar el jefe: ${e.message}"
                                                )
                                            }
                                    } else { // Si existe el documento en la base de datos de JEfe de Seccion...saltara un mensaje informativo y no dejará crear otro.
                                        snackBar(
                                            binding.root,
                                            "Error al crear el usuario"
                                        )
                                    }

                                }
                        }
                        .setNegativeButton("No") { dialog, _ -> // Si pulsamos No en el diálogo, la operación se abortará
                            dialog.dismiss()
                        }.show()

                }.addOnFailureListener {
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
        val user = auth.currentUser // Recogemos el usuario de la sesión actual (jefe de tienda)
        val token = EmailAuthProvider.getCredential(
            superEmail,
            superPassword
        ) // Preparamos sus credenciales para posteriormente reautenticarlo
        if (user != null) { // Si se encuentra el usuario
            user?.reauthenticate(token) // Lo revalidamos para poder continuar con la operación y asi reforzar la seguridad de estos procesos
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        builder.setMessage("Se va a crear el usuario $name $surname . ¿Quieres continuar?")
                            .setTitle("¿Quieres continuar?")
                            .setPositiveButton("Si") { dialog, wich ->// Mismo dialogo para frenar al usuario antes de insertar
                                auth.createUserWithEmailAndPassword(email, pass)
                                    .addOnSuccessListener {
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
                                                "numEmple" to newEmpNum,
                                                "email" to email
                                            )
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
                                                .collection("Empleados")
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
                                            insertEmployee,
                                            insertRegistered
                                        )
                                            .addOnSuccessListener { //Si se completan todas las tareas pasamos a la pantalla de Verificación.
                                                val intent =
                                                    Intent(this, SuccessActivity::class.java)
                                                val bundle =
                                                    Bundle() // guardamos en un bundle los datos del nuevo usuario para pasarlos a SuccesActivity.kt
                                                bundle.putString("name", name)
                                                bundle.putString("surname", surname)
                                                bundle.putString("numEmp", newEmpNum.toString())
                                                bundle.putString("password", pass)
                                                intent.putExtra("data", bundle)
                                                startActivity(intent)
                                                clearData() // función para limpiar los campos de escritura
                                                //Volvemos a iniciar sesión al jefe de tienda para que el nuevo usuario autenticado no nos robe la sesión
                                                auth.signInWithEmailAndPassword(
                                                    superEmail,
                                                    superPassword
                                                )
                                                    .addOnFailureListener {
                                                        snackBar(
                                                            binding.root,
                                                            "Error: Error al iniciar sesión de Superjefe"
                                                        )
                                                    }

                                            }.addOnFailureListener { e ->
                                                snackBar(
                                                    binding.root,
                                                    "Error al guardar el empleado"
                                                )

                                            }.addOnFailureListener {
                                                snackBar(binding.root, "Error al crear el usuario")
                                            }
                                    }
                            }
                            .setNegativeButton("No") { dialog, _ -> // Si se pulsa "NO", se cierra el diálogo sin completar la operación
                                dialog.dismiss()
                            }
                            .show()

                    } else {
                        snackBar(
                            binding.root,
                            "Contraseña incorrecta. Ingrese bien la contraseña"
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


    /**
     * Función para buscár un empleado
     *
     * Esta función permite la búsqueda de un empleado en la base de datos a traves de su número de empleado.
     */
    private fun findUser(numEmple: String) {
        db.collection("users").document(numEmple).get()
            .addOnSuccessListener { document -> //Consulta que busca el número de empleado de entre todos los que hay
                if (document.exists()) { //Si existe un documento con ese número, se cogen sus datos.
                    val name = document.getString("nombre")
                    val surname = document.getString("apellidos")
                    val rol = document.getString("rol")
                    val section = document.getString("seccion")
                    val resultText =
                        "Nombre completo: ${name} ${surname}\nSección: ${rol} en ${section}." //Y se muestra el resultado
                    binding.includeDeleteEmployee.textResult.text =
                        resultText // Se imprime el resultado por pantalla
                    binding.includeDeleteEmployee.btnDelete.visibility =
                        View.VISIBLE // Y se muestra el boton de borrar para que se pueda borrar
                } else {
                    snackBar(
                        binding.root,
                        "No se ha encontrado ningún empleado con ese número."
                    )

                    binding.includeDeleteEmployee.textResult.setText("Sin resultados") // Si no se encuentra ningún empleado con ese número, se muestra este mensaje por pantalla
                    binding.includeDeleteEmployee.btnDelete.visibility =
                        View.GONE // Y se oculta el botón delete para evitar posibles errores
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
                    if (rol == "Jefe de tienda") { // Si el usuario es jefe de tienda, impedimos que se siga con el proceso de borrado, ya que el jefe de tienda solo podrá ser borrado manualmente
                        snackBar(
                            binding.root,
                            "No puedes borrar el jefe de tienda. Para borrarlo contacta con Central."
                        )
                    } else {
                        builder.setTitle("!Atención¡") // Dialogo de advertencia para asegurarnos de que el usuario quiere seguir con la operación
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
                                } // cerramos el dialogo
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


