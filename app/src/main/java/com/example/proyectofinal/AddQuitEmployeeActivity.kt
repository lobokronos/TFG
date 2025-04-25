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
 * Falta introducir un alertDialog para el borrado de usuario por precaución.
 * Falta dar estilo a los radioButton
 * Falta controlar un Snackbar por si la contraseña del superjefe se mete mal.
 */
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.RadioGroup
import com.example.proyectofinal.databinding.ActivityAddQuitEmployeeBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddQuitEmployeeBinding.inflate(layoutInflater)

        val navDrawer = findViewById<FrameLayout>(R.id.content_frame)
        navDrawer.addView(binding.root)


        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        binding.radioGroup.setOnCheckedChangeListener(this)
        binding.includeAddEmployee.btnAdd.setOnClickListener(this)
        binding.includeDeleteEmployee.btnFind.setOnClickListener(this)
        binding.includeDeleteEmployee.btnDelete.setOnClickListener(this)
        binding.includeAddEmployee.spinnerSections.onItemSelectedListener = this
        binding.includeAddEmployee.spinnerJob.onItemSelectedListener = this

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
        val name = binding.includeAddEmployee.editName.text.toString()
        val surname = binding.includeAddEmployee.editSurname.text.toString()
        val email = binding.includeAddEmployee.editEmail.text.toString()
        val superPassword = binding.includeAddEmployee.editSuperPass.text.toString()
        if (name.isEmpty()) {
            snackBar(binding.root, "Debe ingresar un nombre")
        } else if (surname.isEmpty()) {
            snackBar(binding.root, "Debe ingresar apellidos")
        } else if (email.isEmpty()) {
            snackBar(binding.root, "Debe ingresar un email")
        } else if (positionSection == 0) {
            snackBar(binding.root, "Debe seleccionar una sección")
        } else if (positionJob == 0) {
            snackBar(binding.root, "Debe seleccionar un puesto")
        }else if(superPassword.isEmpty()){
            snackBar(binding.root,"Debes introducir tu contraseña para continuar")
        } else {
            val superEmail=auth.currentUser?.email!!
            saveData(name, surname, email, section, positionSection, job, superEmail,superPassword)
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
                if (positionSection == 0) {
                    snackBar(binding.root, "Debes seleccionar una sección")
                } else {
                    snackBar(binding.root, "$section selecccionado")

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
                if (positionJob == 0) {
                    snackBar(binding.root, "Debes seleccionar una ocupación")

                } else {
                    snackBar(binding.root, "$job selecccionado")
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
        positionSection: Int,
        job: String,
        superEmail:String,
        superPassword:String
    ) {

        db = FirebaseFirestore.getInstance()
        db.collection("users").orderBy("numEmple", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { documents ->
                var newEmpNum = 1
                if (!documents.isEmpty) { // Se genera el numero de empleado y se guarda en una variable.
                    val lastEmpNum = documents.documents[0].getLong("numEmple") ?: 0
                    newEmpNum = (lastEmpNum + 32).toInt()
                }
                val pass =
                    newEmpNum.toString() //Se genera la password igualandola al numero de empleado y se castea a String.
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->//Aqui metemos el user en authentication
                        if (task.isSuccessful) {
                            val uid = auth.currentUser!!.uid //Introducimos su uid en el documento
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
                            val registeredUser = hashMapOf("estado" to state)
                            //Estas variables guardan las inserciones de datos en los documentos o colecciones correspondientes utilizando los Hashmaps anteriores.

                            val insertRol = db.collection("rol").document(job)
                                .set(employeeNumber) //Insertamos el numero de empleado en su rol
                            val insertUser = db.collection("users").document(newEmpNum.toString())
                                .set(employee) //Insertamos el hasMap con los dtos del usuario
                            val insertRegistered = db.collection("usuarios registrados")
                                .document(newEmpNum.toString()) // Insertamos el hashMap del estado
                                .set(registeredUser)

                            /**
                             * Aquí recalco algo.
                             *
                             * Solo puede haber un jefe de sección por cada sección, por lo que la lógica desarrollada mas abajo
                             * permite que solo se pueda guardar uno.
                             */
                            val insertJob =
                                db.collection("secciones").document(positionSection.toString())
                            if (job == "Jefe de sección") {
                                insertJob.collection("Jefe de sección").get()
                                    .addOnSuccessListener { bossDocument ->
                                        if (bossDocument.isEmpty) {
                                            //si no hay jefe de sección lo inserta
                                            val insertBoss = insertJob.collection("Jefe de sección")
                                                .document(newEmpNum.toString()).set(sectionData)

                                            /**
                                             * Task.whenAllComplete
                                             * Utilizamos el método "Tasks.whenAllComplete" para indicar que cuando se terminen de realizar las tres acciones
                                             * de inserciones en la BBDD anteriores (las pasamos mediante parametro a traves de las variables) se pasará a la
                                             * pantalla "SuccessActivity donde se mostrarán los datos nuevos del usuario creado. Si no se realizan alguna de
                                             * las tres acciones de inserciones se mostrará un error mediante el Listener addOnFailureListener
                                             */
                                            Tasks.whenAllComplete(
                                                insertUser,
                                                insertRol,
                                                insertBoss,
                                                insertRegistered
                                            )
                                                .addOnSuccessListener { //Si se completan todas las tareas pasamos a la pantalla de Verificación.
                                                    val intent =
                                                        Intent(this, SuccessActivity::class.java)
                                                    val bundle = Bundle()
                                                    //Mirar si se puede pasar con una lista o algo mas cómodo
                                                    bundle.putString("name", name)
                                                    bundle.putString("surname", surname)
                                                    bundle.putString("numEmp", newEmpNum.toString())
                                                    bundle.putString("password", pass)
                                                    intent.putExtra("data", bundle)
                                                    startActivity(intent)
                                                    auth.signInWithEmailAndPassword(superEmail,superPassword).addOnFailureListener{e->
                                                        snackBar(binding.root,"Error: Error al iniciar sesión de Superjefe")
                                                    }
                                                }.addOnFailureListener { e ->
                                                    snackBar(
                                                        binding.root,
                                                        "Error al guardar el jefe: ${e.message}"
                                                    )

                                                }
                                        } else {

                                            snackBar(
                                                binding.root,
                                                "$name $surname ya es el jefe de esta sección"
                                            )

                                        }
                                    }
                            } else {
                                //Si es empleado normal se procede a su inserción siguiendo el mismo procedimiento de antes
                                val employeeInsert =
                                    insertJob.collection("Empleados").document(newEmpNum.toString())
                                        .set(sectionData)

                                /**
                                 * Task.whenAllComplete
                                 *
                                 * Mismo Tasks.whenAllComplete que antes, solo que este cambia la última variable por la de inserción de empleado, ya que en
                                 * esta casuística estamos insertando un empleado. Una vez comprobadas las tres inserciones, se procede al cambio de pantalla
                                 */
                                Tasks.whenAllComplete(
                                    insertUser,
                                    insertRol,
                                    employeeInsert,
                                    insertRegistered
                                )
                                    .addOnSuccessListener {
                                        val intent = Intent(this, SuccessActivity::class.java)
                                        val bundle = Bundle()
                                        bundle.putString("name", name)
                                        bundle.putString("surname", surname)
                                        bundle.putString("numEmp", newEmpNum.toString())
                                        bundle.putString("password", pass)
                                        intent.putExtra("data", bundle)
                                        startActivity(intent)
                                        auth.signInWithEmailAndPassword(superEmail,superPassword).addOnFailureListener{e->
                                            snackBar(binding.root,"Error: Error al iniciar sesión de Superjefe")
                                        }
                                    }.addOnFailureListener { e ->
                                        snackBar(
                                            binding.root,
                                            "Error al guardar el empleado ${e.message}"
                                        )

                                    }
                            }
                        }
                    }
            }
    }

    /**
     * Función para buscár un empleado
     *
     * Esta función permite la búsqueda de un empleado en la base de datos a traves de su número de empleado.
     */
    private fun findUser(numEmple: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(numEmple).get().addOnSuccessListener { document ->
            if (document.exists()) { //Si existe un documento con ese número, se cogen sus datos.
                val name = document.getString("nombre")
                val surname = document.getString("apellidos")
                val rol = document.getString("rol")
                val section = document.getString("seccion")

                val resultText =
                    "Nombre completo: ${name} ${surname}\nSección: ${rol} en ${section}." //Y se muestra el resultado
                binding.includeDeleteEmployee.textResult.text = resultText
            } else {
                snackBar(binding.root, "No se ha encontrado ningún empleado con ese número.")

                binding.includeDeleteEmployee.textResult.setText("Sin resultados")
            }
        }
    }

    /**
     * Función para borrar un empleado
     *
     * Esta función permite borrar un empleado a través de su número de empleado
     */
    private fun deleteUser(numEmple: String) {
        db.collection("users").document(numEmple).get().addOnSuccessListener { document ->
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
                //Recuperamos el uid de authenticator del usuario
                val deleteUser = db.collection("users").document(numEmple)
                    .delete() //Borramos al usuario de la colección usuarios.
                val setState = db.collection("usuarios registrados").document(numEmple)
                    .update(
                        "estado",
                        "inactivo"
                    ) //Actualizamos su estado a "inactivo" para que, cuando generemos otro nuevo empleado, el sistema no reutilice números de empleado ya dados de baja.
                val deleteRol = db.collection("rol").document(rol).update(
                    "numEmple",
                    FieldValue.delete()
                ) //FieldValue.delete permite eliminar un campo de un documento mediante un UPDATE
                val locateSection = db.collection("secciones").document(sectionNumber)
                when (rol) {
                    "Jefe de tienda" -> { //Si es el Jefe de la tienda NO se puede borrar.
                        snackBar(
                            binding.root,
                            "No puedes borrar el jefe de tienda. Para borrarlo contacta con Central."
                        )
                    }

                    "Jefe de sección" -> { //Si es jefe de sección lo borramos de su colección.
                        val deleteBoss =
                            locateSection.collection("Jefe de sección").document(numEmple).delete()

                        /**
                         * task.whenAllComplete
                         *
                         * Cuando se completen las tareas de borrar usuario, actualizar el estado, borrar su rol y borrar el jefe
                         * Se muestra el mensaje de exito o de error dependiendo del caso.
                         */
                        Tasks.whenAllComplete(deleteUser, setState, deleteRol, deleteBoss)
                            .addOnSuccessListener {
                                snackBar(
                                    binding.root,
                                    "Se ha borrado el jefe de sección correctamente"
                                )

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
                            locateSection.collection("Empleados").document(numEmple).delete()
                        Tasks.whenAllComplete(deleteUser, deleteRol, setState, deleteEmployee)
                            .addOnSuccessListener {
                                snackBar(binding.root, "Se ha borrado el empleado correctamente")

                            }.addOnFailureListener { e ->
                                snackBar(binding.root, "Error al borrar el empleado ${e.message}")
                            }
                    }
                }
            }
        }
    }
}

    private fun snackBar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()

}


