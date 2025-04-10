package com.example.proyectofinal

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        val frameContent = findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)


        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        binding.radioGroup.setOnCheckedChangeListener(this)
        binding.includeAddEmployee.btnAdd.setOnClickListener(this)
        binding.includeDeleteEmployee.btnFind.setOnClickListener(this)
        binding.includeDeleteEmployee.btnDelete.setOnClickListener(this)
        binding.includeAddEmployee.spinnerSections.onItemSelectedListener = this
        binding.includeAddEmployee.spinnerJob.onItemSelectedListener = this

    }

    /**Sobreescribimos el método onCheckedChange para mostrar las Cardview "addCardView" o "deleteCardView" segun la seleccion del
     * RadioGroup mediante los métodos view.VISIBLE y view.GONE
     */
    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            binding.AddEmployee.id -> {
                binding.addCardView.visibility = View.VISIBLE
                binding.deleteCardView.visibility = View.GONE
            }

            binding.deleteEmployee.id -> {
                binding.addCardView.visibility = View.GONE
                binding.deleteCardView.visibility = View.VISIBLE
            }
        }
    }

    /**En la función del botón guardamos un nuevo usuario o empleado en la BBDD mediante la función "saveData",
     * la cual utiliza como parámetros los datos recogidos de los campos del CardView
     * "addCardView".
     */
    override fun onClick(v: View?) {
        val findNum = binding.includeDeleteEmployee.editNumber.text.toString()
        when (v?.id) {
            binding.includeAddEmployee.btnAdd.id -> {
                validateData()
            }

            binding.includeDeleteEmployee.btnFind.id -> {
                findUser(findNum)
            }

            binding.includeDeleteEmployee.btnDelete.id -> {
                deleteUser(findNum)
            }
        }
    }

    private fun validateData() {
        val name = binding.includeAddEmployee.editName.text.toString()
        val surname = binding.includeAddEmployee.editSurname.text.toString()
        val email = binding.includeAddEmployee.editEmail.text.toString()
        if (name.isEmpty()) {
            Snackbar.make(binding.root, "Debe ingresar un nombre", Snackbar.LENGTH_SHORT).show()
        } else if (surname.isEmpty()) {
            Snackbar.make(binding.root, "Debe ingresar apellidos", Snackbar.LENGTH_SHORT).show()
        } else if (email.isEmpty()) {
            Snackbar.make(binding.root, "Debe ingresar un email", Snackbar.LENGTH_SHORT).show()
        } else if (positionSection == 0) {
            Snackbar.make(binding.root, "Debe seleccionar una sección", Snackbar.LENGTH_SHORT)
                .show()
        } else if (positionJob == 0) {
            Snackbar.make(binding.root, "Debe seleccionar un puesto", Snackbar.LENGTH_SHORT).show()
        } else {
            saveData(name, surname, email, section, positionSection, job)
        }
    }

    /**Aquí sobreescribimos la función onItemSelected de los Spinner para recoger tanto la posición del item seleccionado de
     * ambos, como su posición en un número.
     */
    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p0?.id) {
            binding.includeAddEmployee.spinnerSections.id -> {
                section = p0.getItemAtPosition(p2).toString()
                positionSection = p2
                if (positionSection == 0) {
                    Snackbar.make(
                        binding.root,
                        "Debes seleccionar una sección",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(binding.root, "$section selecccionado", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            binding.includeAddEmployee.spinnerJob.id -> {
                job = p0.getItemAtPosition(p2).toString()
                positionJob = p2
                if (positionJob == 0) {
                    Snackbar.make(
                        binding.root,
                        "Debes seleccionar una ocupación",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(binding.root, "$job selecccionado", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    /**Esta función es la encargada de realizar la conexión a la base de datos y de guardar sus datos en las colecciones correspondientes
     * o en los documentos correspondientes. Además, se genera de forma automática un número de empleado de tipo Int y una contraseña, la cual
     * será la misma que el número de empleado. Para el número de empleado, se hace una consulta a la coleccion "users" mediante un ORDER BY
     * descendente para averiguar el último numero de empleado que existe. Una vez averiguado, se crea uno nuevo sumándole +32 al último
     * numero de empleado existente.
     */
    private fun saveData(
        name: String,
        surname: String,
        email: String,
        section: String,
        positionSection: Int,
        job: String
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
                            val uid = auth.currentUser!!.uid
                            val logged = false
                            val employee = hashMapOf(
                                "uid" to uid,
                                "nombre" to name,
                                "apellidos" to surname,
                                "numEmple" to newEmpNum,  //Se utiliza un Hashmap para guardar todos los datos recogidos en una variable "employee"
                                "email" to email,         //la cual se utilizará para meter lo datos en la coleccion "users".
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
                            )
                            val state = "activo"
                            val registeredUser = hashMapOf("estado" to state)
                            //Estas variables guardan las inserciones de datos en los documentos o colecciones correspondientes utilizando los Hashmaps anteriores.

                            val insertRol = db.collection("rol").document(job).set(employeeNumber)
                            val insertUser =
                                db.collection("users").document(newEmpNum.toString()).set(employee)
                            val insertRegistered =
                                db.collection("usuarios registrados").document(newEmpNum.toString())
                                    .set(registeredUser)
                            val insertJob =
                                db.collection("secciones").document(positionSection.toString())
                            if (job == "Jefe de sección") {
                                insertJob.collection("Jefe de sección").get()
                                    .addOnSuccessListener { bossDocument ->
                                        if (bossDocument.isEmpty) {
                                            //si no hay jefe de sección lo inserta
                                            val insertBoss = insertJob.collection("Jefe de sección")
                                                .document(newEmpNum.toString()).set(sectionData)

                                            /**Utilizamos el método "Tasks.whenAllComplete" para indicar que cuando se terminen de realizar las tres acciones
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
                                                .addOnSuccessListener {
                                                    val intent =
                                                        Intent(this, SuccessActivity::class.java)
                                                    val bundle = Bundle()
                                                    bundle.putString("name", name)
                                                    bundle.putString("surname", surname)
                                                    bundle.putString("numEmp", newEmpNum.toString())
                                                    bundle.putString("password", pass)
                                                    intent.putExtra("data", bundle)
                                                    startActivity(intent)
                                                }.addOnFailureListener { e ->
                                                    Snackbar.make(
                                                        binding.root,
                                                        "Error al guardar el jefe: ${e.message}",
                                                        Snackbar.LENGTH_SHORT
                                                    ).show()
                                                }
                                        } else {
                                            Snackbar.make(
                                                binding.root,
                                                "$name $surname ya es el jefe de esta sección",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                //Si es empleado normal se procede a su inserción siguiendo el mismo procedimiento de antes
                                val employeeInsert =
                                    insertJob.collection("Empleados").document(newEmpNum.toString())
                                        .set(sectionData)

                                /**Mismo Tasks.whenAllComplete que antes, solo que este cambia la última variable por la de inserción de empleado, ya que en
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
                                    }.addOnFailureListener { e ->
                                        Snackbar.make(
                                            binding.root,
                                            "Error al guardar el empleado ${e.message}",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    }
            }
    }


    private fun findUser(numEmple: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(numEmple).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val name = document.getString("nombre")
                val surname = document.getString("apellidos")
                val rol = document.getString("rol")
                val section = document.getString("seccion")

                val resultText =
                    "Nombre completo: ${name} ${surname}\nSección: ${rol} en ${section}."
                binding.includeDeleteEmployee.textResult.text = resultText
            } else {
                Snackbar.make(
                    binding.root,
                    "No se ha encontrado ningún empleado con ese número.",
                    Snackbar.LENGTH_SHORT
                ).show()
                binding.includeDeleteEmployee.textResult.setText("Sin resultados")
            }
        }
    }

    private fun deleteUser(numEmple: String) {

        var sectionNumber = ""
        var rol = ""

        db.collection("users").document(numEmple).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val findSection = document.getString("seccion")
                rol = document.getString("rol").toString()
                sectionNumber =
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

                val deleteUser = db.collection("users").document(numEmple).delete()
                val setState = db.collection("usuarios registrados").document(numEmple)
                    .update("estado", "inactivo")
                val deleteRol = db.collection("rol").document(rol).update(
                    "numEmple",
                    FieldValue.delete()
                ) //FieldValue.delete permite eliminar un campo de un documento mediante un UPDATE
                val locateSection = db.collection("secciones").document(sectionNumber)
                when (rol) {
                    "Jefe de tienda" -> {
                        Snackbar.make(
                            binding.root,
                            "No puedes borrar el jefe de tienda. Para borrarlo contacta con Central.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    "Jefe de sección" -> {
                        val deleteBoss =
                            locateSection.collection("Jefe de sección").document(numEmple).delete()

                        Tasks.whenAllComplete(deleteUser, setState, deleteRol, deleteBoss)
                            .addOnSuccessListener {
                                Snackbar.make(
                                    binding.root,
                                    "Se ha borrado el jefe de sección correctamente",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener { e ->
                                Snackbar.make(
                                    binding.root,
                                    "Error al borrar el jefe de sección ${e.message}",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                    }
                    else -> {
                        val deleteEmployee =
                            locateSection.collection("Empleados").document(numEmple).delete()
                        Tasks.whenAllComplete(deleteUser, deleteRol, setState, deleteEmployee)
                            .addOnSuccessListener {
                                Snackbar.make(
                                    binding.root,
                                    "Se ha borrado el empleado correctamente",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener { e ->
                                Snackbar.make(
                                    binding.root,
                                    "Error al borrar el empleado ${e.message}",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }
        }
    }


}


